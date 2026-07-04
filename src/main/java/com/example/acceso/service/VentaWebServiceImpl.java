package com.example.acceso.service;

import com.example.acceso.model.*;
import com.example.acceso.repository.ClienteRepository;
import com.example.acceso.repository.ProductoRepository;
import com.example.acceso.repository.VentaRepository;
import com.example.acceso.repository.VentaWebRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class VentaWebServiceImpl implements VentaWebService {

    private final VentaWebRepository ventaWebRepository;
    private final VentaService ventaService;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;

    @Autowired
    public VentaWebServiceImpl(VentaWebRepository ventaWebRepository, VentaService ventaService, ClienteRepository clienteRepository, ProductoRepository productoRepository) {
        this.ventaWebRepository = ventaWebRepository;
        this.ventaService = ventaService;
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
    }

    @Override
    @Transactional
    public VentaWeb guardarVentaWeb(VentaWeb ventaWeb) {
        ventaWeb.setFechaPedido(LocalDateTime.now());
        for (DetalleVentaWeb detalle : ventaWeb.getDetalles()) {
            detalle.setVentaWeb(ventaWeb);
        }
        return ventaWebRepository.save(ventaWeb);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaWeb> listarTodasLasVentasWeb() {
        return ventaWebRepository.findAll();
    }

    @Override
    @Transactional
    public void procesarVentaWeb(Long idVentaWeb) {
        VentaWeb ventaWeb = ventaWebRepository.findById(idVentaWeb)
                .orElseThrow(() -> new RuntimeException("Venta web no encontrada con id: " + idVentaWeb));

        // Validar stock de cada producto en el momento exacto del guardado
        for (DetalleVentaWeb detalleWeb : ventaWeb.getDetalles()) {
            Producto producto = productoRepository.findById(detalleWeb.getProducto().getId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + detalleWeb.getProducto().getId()));
            
            Integer stockActual = producto.getStock();
            Integer cantidadSolicitada = detalleWeb.getCantidad();
            
            if (cantidadSolicitada > stockActual) {
                throw new RuntimeException("Stock insuficiente para el producto '" + producto.getNombre() + "'. " +
                        "Disponible: " + stockActual + ", solicitado: " + cantidadSolicitada);
            }
        }

        Cliente clienteParaVenta;
        if (ventaWeb.getCliente() != null) {
            clienteParaVenta = ventaWeb.getCliente();
        } else {
            Optional<Cliente> clienteExistente = clienteRepository.findByNumeroDocumento(ventaWeb.getNumeroDocumentoCliente());
            clienteParaVenta = clienteExistente.orElseGet(() -> {
                Cliente nuevoCliente = new Cliente();
                nuevoCliente.setNombre(ventaWeb.getNombreCliente());
                nuevoCliente.setNumeroDocumento(ventaWeb.getNumeroDocumentoCliente());
                nuevoCliente.setTipoDocumento("DNI"); // O el tipo que corresponda
                nuevoCliente.setEstado(1);
                return clienteRepository.save(nuevoCliente);
            });
        }

        Venta nuevaVenta = new Venta();
        nuevaVenta.setCliente(clienteParaVenta);
        nuevaVenta.setTotal(ventaWeb.getTotal());
        nuevaVenta.setTipoComprobante(ventaWeb.getTipoComprobante() != null ? ventaWeb.getTipoComprobante() : "Nota de Venta");
        nuevaVenta.setMetodoPago(ventaWeb.getMetodoPago() != null ? ventaWeb.getMetodoPago() : "Por coordinar");
        nuevaVenta.setNota(ventaWeb.getNota());
        nuevaVenta.setDescuento(ventaWeb.getDescuento());
        nuevaVenta.setOrigen("web"); // Se marca como origen web

        List<DetalleVenta> detallesVenta = new ArrayList<>();
        for (DetalleVentaWeb detalleWeb : ventaWeb.getDetalles()) {
            DetalleVenta detalleVenta = new DetalleVenta();
            detalleVenta.setProducto(detalleWeb.getProducto());
            detalleVenta.setCantidad(detalleWeb.getCantidad());
            detalleVenta.setPrecioUnitario(detalleWeb.getPrecioUnitario());
            detallesVenta.add(detalleVenta);
        }
        nuevaVenta.setDetalles(detallesVenta);

        ventaService.crearVenta(nuevaVenta);
        ventaWebRepository.delete(ventaWeb);
    }

    @Override
    @Transactional
    public void eliminarVentaWeb(Long idVentaWeb, String motivo) {
        VentaWeb ventaWeb = ventaWebRepository.findById(idVentaWeb)
                .orElseThrow(() -> new RuntimeException("No se encontró la venta web con id: " + idVentaWeb));
        
        // Guardar el motivo de anulación en el campo nota
        if (motivo != null && !motivo.trim().isEmpty()) {
            ventaWeb.setNota("ANULADO: " + motivo);
            ventaWebRepository.save(ventaWeb);
        }
        
        ventaWebRepository.deleteById(idVentaWeb);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VentaWeb> obtenerVentaWebPorId(Long idVentaWeb) {
        return ventaWebRepository.findById(idVentaWeb);
    }

    @Override
    @Transactional
    public VentaWeb actualizarVentaWeb(Long id, VentaWeb ventaWebActualizada) {
        VentaWeb ventaExistente = ventaWebRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venta web no encontrada con id: " + id));

        // Actualizar todos los campos
        ventaExistente.setCliente(ventaWebActualizada.getCliente());
        ventaExistente.setTipoComprobante(ventaWebActualizada.getTipoComprobante());
        ventaExistente.setMetodoPago(ventaWebActualizada.getMetodoPago());
        ventaExistente.setNota(ventaWebActualizada.getNota());
        ventaExistente.setDescuento(ventaWebActualizada.getDescuento());
        ventaExistente.setTotal(ventaWebActualizada.getTotal());

        // Actualizar detalles
        ventaExistente.getDetalles().clear();
        for(DetalleVentaWeb detalle : ventaWebActualizada.getDetalles()) {
            detalle.setVentaWeb(ventaExistente);
            ventaExistente.getDetalles().add(detalle);
        }

        return ventaWebRepository.save(ventaExistente);
    }
}
