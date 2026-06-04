package com.example.acceso.service;

import com.example.acceso.model.*;
import com.example.acceso.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PedidoWebServiceImpl implements PedidoWebService {

    private final PedidoWebRepository pedidoWebRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final com.example.acceso.repository.VentaRepository ventaRepository;

    public PedidoWebServiceImpl(PedidoWebRepository pedidoWebRepository,
                                UsuarioRepository usuarioRepository,
                                ProductoRepository productoRepository,
                                ClienteRepository clienteRepository,
                                com.example.acceso.repository.VentaRepository ventaRepository) {
        this.pedidoWebRepository = pedidoWebRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
        this.clienteRepository = clienteRepository;
        this.ventaRepository = ventaRepository;
    }

    @Override
    @Transactional
    public PedidoWeb crearPedido(PedidoWeb pedido) {
        System.out.println("=== INICIANDO CREACIÓN DE PEDIDO WEB ===");
        System.out.println("Nombre cliente: " + pedido.getNombreCliente());
        System.out.println("DNI cliente: " + pedido.getDniCliente());
        System.out.println("Teléfono cliente: " + pedido.getTelefonoCliente());
        System.out.println("Método pago: " + pedido.getMetodoPago());
        System.out.println("Voucher imagen: " + pedido.getVoucherImagen());
        System.out.println("Items: " + (pedido.getItems() != null ? pedido.getItems().size() : 0));

        pedido.setFechaPedido(LocalDateTime.now());
        pedido.setEstado(EstadoPedidoWeb.PENDIENTE);

        // Generar número de pedido único
        String numeroPedido = generarNumeroPedido();
        pedido.setNumeroPedido(numeroPedido);
        System.out.println("Número de pedido generado: " + numeroPedido);

        // Calcular subtotal y total
        BigDecimal subtotalCalculado = BigDecimal.ZERO;
        for (DetallePedidoWeb detalle : pedido.getItems()) {
            Producto producto = productoRepository.findById(detalle.getProducto().getId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + detalle.getProducto().getId()));

            // Asignar el producto recuperado de la base de datos (no el transient del request)
            detalle.setProducto(producto);
            detalle.setPrecioUnitario(producto.getPrecio());
            BigDecimal subtotalDetalle = producto.getPrecio().multiply(new BigDecimal(detalle.getCantidad()));
            detalle.setSubtotal(subtotalDetalle);
            subtotalCalculado = subtotalCalculado.add(subtotalDetalle);

            detalle.setPedidoWeb(pedido);
        }

        pedido.setSubtotal(subtotalCalculado);

        // Calcular descuento y total
        BigDecimal descuento = pedido.getDescuento() != null ? pedido.getDescuento() : BigDecimal.ZERO;
        if (descuento.compareTo(BigDecimal.ZERO) < 0) {
            descuento = BigDecimal.ZERO;
        }
        if (descuento.compareTo(subtotalCalculado) > 0) {
            throw new RuntimeException("El descuento no puede ser mayor que el subtotal.");
        }
        pedido.setDescuento(descuento);

        BigDecimal totalFinal = subtotalCalculado.subtract(descuento);
        pedido.setTotal(totalFinal);

        System.out.println("Subtotal: " + subtotalCalculado);
        System.out.println("Descuento: " + descuento);
        System.out.println("Total: " + totalFinal);

        PedidoWeb pedidoGuardado = pedidoWebRepository.save(pedido);
        System.out.println("Pedido guardado con ID: " + pedidoGuardado.getId());
        System.out.println("=== PEDIDO WEB CREADO EXITOSAMENTE ===");

        return pedidoGuardado;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoWeb> listarTodosLosPedidos() {
        System.out.println("=== LISTANDO TODOS LOS PEDIDOS WEB ===");
        List<PedidoWeb> pedidos = pedidoWebRepository.findAll();
        System.out.println("Cantidad de pedidos encontrados: " + pedidos.size());
        for (PedidoWeb pedido : pedidos) {
            System.out.println("Pedido ID: " + pedido.getId() + ", Número: " + pedido.getNumeroPedido() + ", Estado: " + pedido.getEstado() + ", Cliente: " + pedido.getNombreCliente());
        }
        System.out.println("=== FIN LISTADO DE PEDIDOS WEB ===");
        return pedidos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoWeb> listarPedidosPorCliente(Long clienteId) {
        return pedidoWebRepository.findByClienteId(clienteId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoWeb> listarPedidosPorEstado(EstadoPedidoWeb estado) {
        return pedidoWebRepository.findByEstadoOrderByFechaPedidoDesc(estado);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PedidoWeb> obtenerPedidoPorId(Long id) {
        return pedidoWebRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PedidoWeb> obtenerPedidoPorNumero(String numeroPedido) {
        return pedidoWebRepository.findByNumeroPedido(numeroPedido);
    }

    @Override
    @Transactional
    public PedidoWeb aprobarPedido(Long id, Long verificadoPorId) {
        PedidoWeb pedido = pedidoWebRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado con id: " + id));

        if (pedido.getEstado() != EstadoPedidoWeb.PENDIENTE && pedido.getEstado() != EstadoPedidoWeb.EN_REVISION) {
            throw new RuntimeException("Solo se pueden aprobar pedidos en estado PENDIENTE o EN_REVISION");
        }

        // Obtener verificador si se proporciona ID
        Usuario verificador = null;
        if (verificadoPorId != null) {
            verificador = usuarioRepository.findById(verificadoPorId)
                    .orElseThrow(() -> new RuntimeException("Usuario verificador no encontrado con id: " + verificadoPorId));
        }

        // Crear venta a partir del pedido
        Venta venta = new Venta();
        venta.setTotal(pedido.getTotal());
        venta.setSubtotal(pedido.getSubtotal());
        venta.setDescuento(pedido.getDescuento());
        venta.setMetodoPago(pedido.getMetodoPago());
        venta.setTipoComprobante("Nota de Venta");
        venta.setOrigen("web");
        venta.setEstado(1); // Activa (ya procesada)
        venta.setFechaVenta(LocalDateTime.now());

        // Generar número de venta
        String numeroVenta = generarNumeroVenta();
        venta.setNumeroVenta(numeroVenta);

        // Buscar o crear cliente
        Cliente cliente = clienteRepository.findByNumeroDocumento(pedido.getDniCliente())
                .orElseGet(() -> {
                    Cliente nuevoCliente = new Cliente();
                    nuevoCliente.setNombre(pedido.getNombreCliente());
                    nuevoCliente.setNumeroDocumento(pedido.getDniCliente());
                    nuevoCliente.setTipoDocumento("DNI");
                    nuevoCliente.setEstado(1);
                    return clienteRepository.save(nuevoCliente);
                });

        venta.setCliente(cliente);

        // Crear detalles de venta y actualizar stock
        List<DetalleVenta> detallesVenta = new ArrayList<>();
        for (DetallePedidoWeb detalleWeb : pedido.getItems()) {
            Producto producto = productoRepository.findById(detalleWeb.getProducto().getId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + detalleWeb.getProducto().getId()));

            // Validar stock suficiente
            if (producto.getStock() < detalleWeb.getCantidad()) {
                throw new RuntimeException("Stock insuficiente para el producto: " + producto.getNombre() +
                        ". Stock disponible: " + producto.getStock() + ", Cantidad solicitada: " + detalleWeb.getCantidad());
            }

            // Restar stock
            producto.setStock(producto.getStock() - detalleWeb.getCantidad());
            productoRepository.save(producto);

            DetalleVenta detalleVenta = new DetalleVenta();
            detalleVenta.setProducto(producto);
            detalleVenta.setCantidad(detalleWeb.getCantidad());
            detalleVenta.setPrecioUnitario(detalleWeb.getPrecioUnitario());
            detalleVenta.setSubtotal(detalleWeb.getSubtotal());
            detalleVenta.setVenta(venta);
            detallesVenta.add(detalleVenta);
        }
        venta.setDetalles(detallesVenta);

        // Guardar la venta directamente en el repositorio
        Venta ventaGuardada = ventaRepository.save(venta);

        // Actualizar el pedido a APROBADO primero
        pedido.setEstado(EstadoPedidoWeb.APROBADO);
        pedido.setFechaVerificacion(LocalDateTime.now());
        if (verificador != null) {
            pedido.setVerificadoPor(verificador);
        }
        pedido.setVenta(ventaGuardada);

        // Luego cambiar a PROCESADO
        pedido.setEstado(EstadoPedidoWeb.PROCESADO);

        // Enviar email de notificación al cliente (COMENTADO - Requiere configuración de email)
        // if (pedido.getCliente() != null && pedido.getCliente().getCorreo() != null) {
        //     emailService.enviarEmailAprobacion(pedido.getCliente().getCorreo(), pedido.getNumeroPedido());
        // }

        return pedidoWebRepository.save(pedido);
    }

    private String generarNumeroVenta() {
        String prefijo = "N";
        DateTimeFormatter mesFormatter = DateTimeFormatter.ofPattern("MM");
        String mes = LocalDateTime.now().format(mesFormatter);
        String prefijoBusqueda = prefijo + mes + "-";

        Optional<Venta> ultimaVenta = ventaRepository.findTopByNumeroVentaStartingWithOrderByNumeroVentaDesc(prefijoBusqueda);

        int correlativo = 1;
        if (ultimaVenta.isPresent()) {
            String ultimoNumero = ultimaVenta.get().getNumeroVenta();
            try {
                String correlativoStr = ultimoNumero.substring(ultimoNumero.lastIndexOf('-') + 1);
                correlativo = Integer.parseInt(correlativoStr) + 1;
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                correlativo = 1;
            }
        }

        return String.format("%s%s-%04d", prefijo, mes, correlativo);
    }

    @Override
    @Transactional
    public PedidoWeb rechazarPedido(Long id, Long verificadoPorId, String motivoRechazo) {
        PedidoWeb pedido = pedidoWebRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado con id: " + id));
        
        if (pedido.getEstado() != EstadoPedidoWeb.PENDIENTE && pedido.getEstado() != EstadoPedidoWeb.EN_REVISION) {
            throw new RuntimeException("Solo se pueden rechazar pedidos en estado PENDIENTE o EN_REVISION");
        }
        
        Usuario verificador = usuarioRepository.findById(verificadoPorId)
                .orElseThrow(() -> new RuntimeException("Usuario verificador no encontrado con id: " + verificadoPorId));
        
        pedido.setEstado(EstadoPedidoWeb.RECHAZADO);
        pedido.setFechaVerificacion(LocalDateTime.now());
        pedido.setVerificadoPor(verificador);
        pedido.setMotivoRechazo(motivoRechazo);
        
        // Enviar email de notificación al cliente (COMENTADO - Requiere configuración de email)
        // if (pedido.getCliente() != null && pedido.getCliente().getCorreo() != null) {
        //     emailService.enviarEmailRechazo(pedido.getCliente().getCorreo(), pedido.getNumeroPedido(), motivoRechazo);
        // }
        
        return pedidoWebRepository.save(pedido);
    }

    @Override
    @Transactional
    public PedidoWeb marcarComoProcesado(Long id) {
        PedidoWeb pedido = pedidoWebRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado con id: " + id));
        
        if (pedido.getEstado() != EstadoPedidoWeb.APROBADO) {
            throw new RuntimeException("Solo se pueden marcar como procesados pedidos en estado APROBADO");
        }
        
        pedido.setEstado(EstadoPedidoWeb.PROCESADO);
        return pedidoWebRepository.save(pedido);
    }

    @Override
    @Transactional
    public void eliminarPedido(Long id, String motivo) {
        PedidoWeb pedido = pedidoWebRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el pedido con id: " + id));
        
        // Guardar el motivo de anulación en el campo motivoRechazo
        if (motivo != null && !motivo.trim().isEmpty()) {
            pedido.setMotivoRechazo("ANULADO: " + motivo);
            pedidoWebRepository.save(pedido);
        }
        
        pedidoWebRepository.deleteById(id);
    }

    private String generarNumeroPedido() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");
        String mesAnio = LocalDateTime.now().format(formatter);
        String prefijoBusqueda = "PED-" + mesAnio + "-";
        
        Optional<PedidoWeb> ultimoPedido = pedidoWebRepository.findAll().stream()
                .filter(p -> p.getNumeroPedido() != null && p.getNumeroPedido().startsWith(prefijoBusqueda))
                .max((p1, p2) -> p1.getNumeroPedido().compareTo(p2.getNumeroPedido()));
        
        int correlativo = 1;
        if (ultimoPedido.isPresent()) {
            String ultimoNumero = ultimoPedido.get().getNumeroPedido();
            try {
                String correlativoStr = ultimoNumero.substring(ultimoNumero.lastIndexOf('-') + 1);
                correlativo = Integer.parseInt(correlativoStr) + 1;
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                correlativo = 1;
            }
        }
        
        return String.format("PED-%s-%04d", mesAnio, correlativo);
    }
}
