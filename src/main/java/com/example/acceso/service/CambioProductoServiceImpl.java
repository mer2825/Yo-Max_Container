package com.example.acceso.service;

import com.example.acceso.dto.CambioProductoRequestDTO;
import com.example.acceso.dto.CambioProductoResponseDTO;
import com.example.acceso.dto.NotaCreditoItemDTO;
import com.example.acceso.model.*;
import com.example.acceso.repository.*;
import com.example.acceso.model.NotasCredito;
import com.example.acceso.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CambioProductoServiceImpl implements CambioProductoService {

    private static final Logger logger = LoggerFactory.getLogger(CambioProductoServiceImpl.class);

    private final VentaRepository ventaRepository;
    private final CambioProductoRepository cambioProductoRepository;
    private final ProductoRepository productoRepository;
    private final NotasCreditoRepository notasCreditoRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final EmpresaRepository empresaRepository;

    private final ApisunatService apisunatService;

    private final VentaService ventaService;

    @Autowired
    public CambioProductoServiceImpl(
            VentaRepository ventaRepository,
            CambioProductoRepository cambioProductoRepository,
            ProductoRepository productoRepository,
            NotasCreditoRepository notasCreditoRepository,
            DetalleVentaRepository detalleVentaRepository,
            EmpresaRepository empresaRepository,
            ApisunatService apisunatService,
            VentaService ventaService
    ) {
        this.ventaRepository = ventaRepository;
        this.cambioProductoRepository = cambioProductoRepository;
        this.productoRepository = productoRepository;
        this.notasCreditoRepository = notasCreditoRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.empresaRepository = empresaRepository;
        this.apisunatService = apisunatService;
        this.ventaService = ventaService;
    }

    @Override
    @Transactional
    public CambioProductoResponseDTO procesarCambio(CambioProductoRequestDTO request, Usuario usuarioActual) {
        if (request == null) {
            throw new RuntimeException("Request no puede ser null");
        }

        Venta ventaOriginal = ventaRepository.findByIdWithDetallesAndProductos(request.getVentaOriginalId())
                .orElseThrow(() -> new RuntimeException("Venta no encontrada: " + request.getVentaOriginalId()));

        if (ventaOriginal.getEstado() != null && ventaOriginal.getEstado() == 2) {
            throw new RuntimeException("No se puede cambiar una venta anulada");
        }

        String tipoComprobante = ventaOriginal.getTipoComprobante();
        if (tipoComprobante != null && "nota_venta".equalsIgnoreCase(tipoComprobante)) {
            throw new RuntimeException("No se puede emitir cambios sobre una nota de venta");
        }

        // ⚠️ VALIDACIÓN: Solo se puede emitir nota de crédito si no han pasado más de 7 días desde la emisión
        if (ventaOriginal.getFechaVenta() != null) {
            long diasDesdeEmision = ChronoUnit.DAYS.between(ventaOriginal.getFechaVenta(), LocalDateTime.now());
            if (diasDesdeEmision > 7) {
                throw new RuntimeException(
                    "No se puede realizar el cambio de producto porque han pasado " + diasDesdeEmision +
                    " días desde la emisión del comprobante. Solo se permite emitir notas de crédito " +
                    "dentro de los 7 días posteriores a la fecha de emisión."
                );
            }
        }

        // Validar detalle devuelto
        DetalleVenta detalleVentaOriginal = null;
        if (ventaOriginal.getDetalles() != null) {
            for (DetalleVenta d : ventaOriginal.getDetalles()) {
                if (d != null && d.getId() != null && d.getId().equals(request.getDetalleVentaId())) {
                    detalleVentaOriginal = d;
                    break;
                }
            }
        }
        if (detalleVentaOriginal == null) {
            throw new RuntimeException("Detalle de venta no pertenece a la venta original");
        }

        if (request.getCantidadDevuelta() > (detalleVentaOriginal.getCantidad() != null ? detalleVentaOriginal.getCantidad() : 0)) {
            throw new RuntimeException("Cantidad devuelta no puede exceder la cantidad del detalle original");
        }

        boolean yaCompletado = cambioProductoRepository
                .existsByDetalleVentaOriginalIdAndEstado(request.getDetalleVentaId(), "COMPLETADO");
        if (yaCompletado) {
            throw new RuntimeException("Este ítem ya fue cambiado previamente");
        }

        // Producto nuevo
        Producto productoNuevo = productoRepository.findById(request.getProductoNuevoId())
                .orElseThrow(() -> new RuntimeException("Producto nuevo no encontrado: " + request.getProductoNuevoId()));

        if (productoNuevo.getEstado() != null && productoNuevo.getEstado() != 1) {
            throw new RuntimeException("Producto nuevo no está activo");
        }

        if (productoNuevo.getStock() < request.getCantidadNuevoProducto()) {
            // 422 (insuficiente stock)
            throw new IllegalStateException("Stock insuficiente");
        }

        BigDecimal precioUnitarioDetalle = detalleVentaOriginal.getPrecioUnitario() != null
                ? detalleVentaOriginal.getPrecioUnitario()
                : BigDecimal.ZERO;

        BigDecimal montoNC = precioUnitarioDetalle
                .multiply(BigDecimal.valueOf(request.getCantidadDevuelta()))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal montoNuevo = productoNuevo.getPrecio()
                .multiply(BigDecimal.valueOf(request.getCantidadNuevoProducto()))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal montoExcedente = montoNuevo.subtract(montoNC);
        if (montoExcedente.compareTo(BigDecimal.ZERO) < 0) {
            montoExcedente = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // Variables para construir el cambio de producto
        CambioProducto cambioProducto;

        // --- Emitir Nota de Crédito (línea única: detalle devuelto)
        // Construir item DTO compatible con el controller existente
        NotaCreditoItemDTO item = new NotaCreditoItemDTO();
        item.setDetalleVentaId(detalleVentaOriginal.getId());
        item.setCantidad(request.getCantidadDevuelta());
        item.setDescripcion(detalleVentaOriginal.getProducto() != null
                ? detalleVentaOriginal.getProducto().getNombre()
                : "Producto");
        item.setPrecioUnitario(precioUnitarioDetalle);

        Empresa empresa = empresaRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new RuntimeException("No hay empresa configurada"));

        boolean esFacturaOriginal = "01".equals(ventaOriginal.getTipoComprobante())
                || "Factura".equalsIgnoreCase(ventaOriginal.getTipoComprobante());

        String serie;
        Integer correlativo;
        if (esFacturaOriginal) {
            serie = empresa.getSerieNotaCreditoFactura();
            correlativo = empresa.getCorrelativoNotaCreditoFactura();
        } else {
            serie = empresa.getSerieNotaCreditoBoleta();
            correlativo = empresa.getCorrelativoNotaCreditoBoleta();
        }

        if (serie == null || serie.isBlank()) {
            serie = esFacturaOriginal ? empresa.getSerieFactura() : empresa.getSerieBoleta();
        }
        if (correlativo == null || correlativo < 1) {
            correlativo = 1;
        }

        String tipoNota = "07"; // devolución parcial
        String motivo = request.getMotivo() != null ? request.getMotivo() : "Devolución parcial por cambio de producto";

        // persistimos NotasCredito como hace el NotaCreditoController
        NotasCredito notaCredito = new NotasCredito();
        notaCredito.setVenta(ventaOriginal);
        notaCredito.setTipoNota(tipoNota);
        notaCredito.setMotivo(motivo);
        notaCredito.setSerie(serie);
        notaCredito.setCorrelativo(correlativo);
        notaCredito.setSerieCorrelativo(serie + "-" + String.format("%08d", correlativo));
        notaCredito.setTotalAcreditado(montoNC);
        notaCredito.setEstadoSunat("pendiente");
        notaCredito.setEmitidaPorUsuario(usuarioActual);
        notaCredito.setFechaEmision(LocalDateTime.now());
        notaCredito = notasCreditoRepository.save(notaCredito);

        ApisunatService.ApisunatResult resultado = apisunatService.emitirNotaCredito(
                ventaOriginal,
                serie,
                correlativo,
                tipoNota,
                motivo,
                List.of(item),
                empresa
        );

        notaCredito.setEstadoSunat(resultado.getStatus() != null ? resultado.getStatus().toLowerCase() : "error");
        notaCredito.setNubefactId(resultado.getDocumentId());
        notaCredito.setPdfUrl(resultado.getPdfUrl());
        notaCredito.setXmlUrl(resultado.getXmlUrl());
        notaCredito.setHashCdr(resultado.getHashCdr());
        notaCredito.setRawResponse(resultado.getRawResponse());

        boolean esAceptado = "ACEPTADO".equalsIgnoreCase(resultado.getStatus());

        // Actualizar correlativo en empresa como el flow actual
        if (esAceptado || "RECHAZADO".equalsIgnoreCase(resultado.getStatus())) {
            if (esFacturaOriginal) {
                empresa.setCorrelativoNotaCreditoFactura(correlativo + 1);
            } else {
                empresa.setCorrelativoNotaCreditoBoleta(correlativo + 1);
            }
            // solo guardamos si se usa repos para empresa
            empresaRepository.save(empresa);
        }

        if (esAceptado) {
            // Paso 7: Reponer stock del producto devuelto y actualizar estado de la venta original
            // Usar PARCIAL si quedan ítems no devueltos; TOTAL si se devolvieron todos los ítems de la venta.
            String estadoNCVenta = "TOTAL";
            if (ventaOriginal.getDetalles() != null && !ventaOriginal.getDetalles().isEmpty()) {
                boolean devolvioTodos = false;
                for (DetalleVenta d : ventaOriginal.getDetalles()) {
                    if (d == null || d.getId() == null) continue;
                    // Esta v1 recibe un único detalle devuelto.
                    if (!d.getId().equals(detalleVentaOriginal.getId())) {
                        devolvioTodos = false;
                        break;
                    }
                    devolvioTodos = true;
                }
                estadoNCVenta = devolvioTodos ? "TOTAL" : "PARCIAL";
            }

            ventaService.aplicarNotaCredito(ventaOriginal.getId(), estadoNCVenta, List.of(item));
        } else {
            // abortar todo si no fue aceptado en MiAPI (rollback por @Transactional)
            logger.error("MiAPI NC no fue aceptada. rawResponse={}", resultado.getRawResponse());
            throw new IllegalStateException("MiAPI NC no fue aceptada: " + resultado.getStatus());
        }

        notaCredito = notasCreditoRepository.save(notaCredito);

        BigDecimal montoExcedenteFinal = montoNuevo.subtract(montoNC).setScale(2, RoundingMode.HALF_UP);

        // Paso 8: Descontar stock del producto nuevo
        int nuevoStock = productoNuevo.getStock() - request.getCantidadNuevoProducto();
        if (nuevoStock < 0) {
            throw new IllegalStateException("Stock insuficiente para el producto nuevo.");
        }
        productoNuevo.setStock(nuevoStock);
        productoRepository.save(productoNuevo);

        // Paso 9: Si montoExcedente > 0 → generar venta del excedente
        Venta ventaExcedente = null;
        if (montoExcedenteFinal != null && montoExcedenteFinal.compareTo(BigDecimal.ZERO) > 0) {
            ventaExcedente = new Venta();
            ventaExcedente.setCliente(ventaOriginal.getCliente());
            ventaExcedente.setMetodoPago(ventaOriginal.getMetodoPago());
            ventaExcedente.setTipoComprobante(ventaOriginal.getTipoComprobante());
            ventaExcedente.setOrigen("pos");
            ventaExcedente.setEstado(1);
            ventaExcedente.setFechaVenta(LocalDateTime.now());
            ventaExcedente.setSubtotal(montoExcedenteFinal);
            ventaExcedente.setDescuento(BigDecimal.ZERO);
            ventaExcedente.setTotal(montoExcedenteFinal);
            ventaExcedente.setNota("Excedente por cambio de producto. Ref. NC: " + notaCredito.getSerieCorrelativo()
                    + " / Venta original: " + ventaOriginal.getNumeroVenta());
            // numeroVenta lo dejará procesarComprobanteElectronico si fuera necesario,
            // pero el sistema ya usa serie/correlativo por SUNAT.
            ventaExcedente.setNumeroVenta("EXC-" + System.currentTimeMillis());

            DetalleVenta detalleExcedente = new DetalleVenta();
            detalleExcedente.setProducto(productoNuevo);
            detalleExcedente.setCantidad(1);
            detalleExcedente.setPrecioUnitario(montoExcedenteFinal);
            detalleExcedente.setSubtotal(montoExcedenteFinal);
            detalleExcedente.setVenta(ventaExcedente);

            ventaExcedente.setDetalles(new ArrayList<>(List.of(detalleExcedente)));
            ventaExcedente.setEstadoSunat("pendiente");

            ventaExcedente = ventaRepository.save(ventaExcedente);
            ventaExcedente = ventaService.procesarComprobanteElectronico(ventaExcedente);
        }

        // Paso 10: Crear y persistir CambioProducto (ahora con notaCredito ya creada)
        cambioProducto = new CambioProducto();
        cambioProducto.setVentaOriginal(ventaOriginal);
        cambioProducto.setDetalleVentaOriginal(detalleVentaOriginal);
        cambioProducto.setProductoNuevo(productoNuevo);
        cambioProducto.setCantidadDevuelta(request.getCantidadDevuelta());
        cambioProducto.setCantidadNuevoProducto(request.getCantidadNuevoProducto());
        cambioProducto.setNotaCredito(notaCredito);
        cambioProducto.setVentaExcedente(ventaExcedente);
        cambioProducto.setMontoNotaCredito(montoNC);
        cambioProducto.setMontoProductoNuevo(montoNuevo);
        cambioProducto.setMontoExcedente(montoExcedenteFinal);
        cambioProducto.setMotivo(request.getMotivo());
        cambioProducto.setEstado("COMPLETADO");
        cambioProducto.setFechaCambio(LocalDateTime.now());
        cambioProducto.setUsuario(usuarioActual);

        cambioProducto = cambioProductoRepository.save(cambioProducto);

        CambioProductoResponseDTO resp = new CambioProductoResponseDTO();
        resp.setCambioProductoId(cambioProducto.getId());
        resp.setEstado(cambioProducto.getEstado());

        resp.setNcSerieCorrelativo(notaCredito.getSerieCorrelativo());
        resp.setNcEstadoSunat(notaCredito.getEstadoSunat());
        resp.setNcPdfUrl(notaCredito.getPdfUrl());

        resp.setMontoNotaCredito(montoNC);
        resp.setMontoProductoNuevo(montoNuevo);
        resp.setMontoExcedente(montoExcedente);

        if (ventaExcedente != null) {
            resp.setVentaExcedenteId(ventaExcedente.getId());
            resp.setVentaExcedenteSerieCorrelativo(ventaExcedente.getSerieCorrelativo());
            resp.setVentaExcedenteEstadoSunat(ventaExcedente.getEstadoSunat());
            resp.setVentaExcedentePdfUrl(ventaExcedente.getPdfUrl());
        }

        resp.setMensaje(esAceptado ? "Cambio de producto completado" : "Cambio de producto con error SUNAT");

        return resp;
    }
}