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
    private final VentaService ventaService;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final EmailService emailService;

    public PedidoWebServiceImpl(PedidoWebRepository pedidoWebRepository,
                                UsuarioRepository usuarioRepository,
                                VentaService ventaService,
                                ProductoRepository productoRepository,
                                ClienteRepository clienteRepository,
                                EmailService emailService) {
        this.pedidoWebRepository = pedidoWebRepository;
        this.usuarioRepository = usuarioRepository;
        this.ventaService = ventaService;
        this.productoRepository = productoRepository;
        this.clienteRepository = clienteRepository;
        this.emailService = emailService;
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
        
        Usuario verificador = usuarioRepository.findById(verificadoPorId)
                .orElseThrow(() -> new RuntimeException("Usuario verificador no encontrado con id: " + verificadoPorId));
        
        // Crear venta a partir del pedido
        Venta venta = new Venta();
        venta.setTotal(pedido.getTotal());
        venta.setSubtotal(pedido.getSubtotal());
        venta.setDescuento(pedido.getDescuento());
        venta.setMetodoPago(pedido.getMetodoPago());
        venta.setTipoComprobante("Nota de Venta");
        venta.setOrigen("web");
        venta.setEstado(1); // Activa (ya procesada)
        
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
        
        // Crear detalles de venta
        List<DetalleVenta> detallesVenta = new ArrayList<>();
        for (DetallePedidoWeb detalleWeb : pedido.getItems()) {
            DetalleVenta detalleVenta = new DetalleVenta();
            detalleVenta.setProducto(detalleWeb.getProducto());
            detalleVenta.setCantidad(detalleWeb.getCantidad());
            detalleVenta.setPrecioUnitario(detalleWeb.getPrecioUnitario());
            detallesVenta.add(detalleVenta);
        }
        venta.setDetalles(detallesVenta);
        
        // Guardar la venta
        Venta ventaGuardada = ventaService.crearVenta(venta);
        
        // Actualizar el pedido a APROBADO primero
        pedido.setEstado(EstadoPedidoWeb.APROBADO);
        pedido.setFechaVerificacion(LocalDateTime.now());
        pedido.setVerificadoPor(verificador);
        pedido.setVenta(ventaGuardada);
        
        // Luego cambiar a PROCESADO
        pedido.setEstado(EstadoPedidoWeb.PROCESADO);
        
        // Enviar email de notificación al cliente (COMENTADO - Requiere configuración de email)
        // if (pedido.getCliente() != null && pedido.getCliente().getCorreo() != null) {
        //     emailService.enviarEmailAprobacion(pedido.getCliente().getCorreo(), pedido.getNumeroPedido());
        // }
        
        return pedidoWebRepository.save(pedido);
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
    public void eliminarPedido(Long id) {
        if (!pedidoWebRepository.existsById(id)) {
            throw new RuntimeException("No se encontró el pedido con id: " + id);
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
