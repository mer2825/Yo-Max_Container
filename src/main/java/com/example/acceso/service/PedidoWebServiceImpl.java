package com.example.acceso.service;

import com.example.acceso.model.*;
import com.example.acceso.repository.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.security.SecureRandom;

@Service
public class PedidoWebServiceImpl implements PedidoWebService {

    private final PedidoWebRepository pedidoWebRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final com.example.acceso.repository.VentaRepository ventaRepository;
    private final VentaService ventaService;
    private final CloudinaryService cloudinaryService;
    private final CajaService cajaService;
    private final EmailService emailService;
    private final PdfService pdfService;

    public PedidoWebServiceImpl(PedidoWebRepository pedidoWebRepository,
                                UsuarioRepository usuarioRepository,
                                ProductoRepository productoRepository,
                                ClienteRepository clienteRepository,
                                com.example.acceso.repository.VentaRepository ventaRepository,
                                VentaService ventaService,
                                CloudinaryService cloudinaryService,
                                CajaService cajaService,
                                EmailService emailService,
                                PdfService pdfService) {
        this.pedidoWebRepository = pedidoWebRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
        this.clienteRepository = clienteRepository;
        this.ventaRepository = ventaRepository;
        this.ventaService = ventaService;
        this.cloudinaryService = cloudinaryService;
        this.cajaService = cajaService;
        this.emailService = emailService;
        this.pdfService = pdfService;
    }

    private String generarPdfKey() {
        SecureRandom random = new SecureRandom();
        StringBuilder key = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            key.append(random.nextInt(10));
        }
        return key.toString();
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
        pedido.setPdfKey(generarPdfKey());

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
    public Page<PedidoWeb> listarPedidosConFiltros(int page, int size, String sortBy, String sortDir, String estado, LocalDate fechaDesde, LocalDate fechaHasta, String busqueda) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<PedidoWeb> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (estado != null && !estado.isEmpty()) {
                try {
                    EstadoPedidoWeb estadoEnum = EstadoPedidoWeb.valueOf(estado.toUpperCase());
                    predicates.add(cb.equal(root.get("estado"), estadoEnum));
                } catch (IllegalArgumentException e) {
                    // Si el estado no es válido, no se añade el filtro de estado
                    System.err.println("Estado de pedido inválido: " + estado);
                }
            }

            if (fechaDesde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaPedido"), LocalDateTime.of(fechaDesde, LocalTime.MIN)));
            }
            if (fechaHasta != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaPedido"), LocalDateTime.of(fechaHasta, LocalTime.MAX)));
            }

            if (busqueda != null && !busqueda.isEmpty()) {
                String likePattern = "%" + busqueda.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("nombreCliente")), likePattern),
                        cb.like(cb.lower(root.get("dniCliente")), likePattern),
                        cb.like(cb.lower(root.get("numeroPedido")), likePattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return pedidoWebRepository.findAll(spec, pageable);
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

        Usuario verificador = null;
        if (verificadoPorId != null) {
            verificador = usuarioRepository.findById(verificadoPorId)
                    .orElseThrow(() -> new RuntimeException("Usuario verificador no encontrado con id: " + verificadoPorId));
        }

        pedido.setEstado(EstadoPedidoWeb.APROBADO);
        pedido.setFechaVerificacion(LocalDateTime.now());
        if (verificador != null) {
            pedido.setVerificadoPor(verificador);
        }
        pedidoWebRepository.save(pedido);

        Venta venta = crearVentaDesdePedido(pedido);

        // Vincular la venta a la sesión de caja activa (OBLIGATORIO para llevar registro)
        Optional<SesionCaja> sesionActiva = cajaService.obtenerSesionActiva();
        if (sesionActiva.isEmpty()) {
            throw new RuntimeException("No hay una sesión de caja abierta. Debe abrir caja antes de aprobar pedidos web.");
        }
        venta.setSesionCaja(sesionActiva.get());
        venta = ventaRepository.save(venta);

        Venta ventaProcesada;
        try {
            ventaProcesada = ventaService.procesarComprobanteElectronico(venta);
        } catch (Exception e) {
            venta.setEstadoSunat("error");
            venta.setNota("Error al emitir comprobante electrónico: " + e.getMessage());
            ventaProcesada = ventaRepository.save(venta);
        }

        pedido.setVenta(ventaProcesada);
        pedido.setEstado(EstadoPedidoWeb.PROCESADO);

        PedidoWeb pedidoFinal = pedidoWebRepository.save(pedido);

        // Enviar email de confirmación al cliente con PDFs adjuntos (especificación + boleta)
        if (pedidoFinal.getEmailCliente() != null && !pedidoFinal.getEmailCliente().isBlank()) {
            try {
                byte[] especPdfBytes = pdfService.generarEspecificacionCompra(pedidoFinal).readAllBytes();
                java.io.ByteArrayInputStream especPdfStream = new java.io.ByteArrayInputStream(especPdfBytes);
                
                // Intentar descargar la boleta PDF desde la URL de SUNAT
                java.io.ByteArrayInputStream boletaPdfStream = null;
                String serieCorrelativo = null;
                if (ventaProcesada != null) {
                    serieCorrelativo = ventaProcesada.getSerieCorrelativo();
                    if (ventaProcesada.getPdfUrl() != null && !ventaProcesada.getPdfUrl().isBlank()) {
                        try {
                            java.net.URL boletaUrl = new java.net.URL(ventaProcesada.getPdfUrl());
                            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                            try (java.io.InputStream is = boletaUrl.openStream()) {
                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                while ((bytesRead = is.read(buffer)) != -1) {
                                    baos.write(buffer, 0, bytesRead);
                                }
                            }
                            boletaPdfStream = new java.io.ByteArrayInputStream(baos.toByteArray());
                        } catch (Exception ex) {
                            System.err.println("No se pudo descargar la boleta PDF desde " + ventaProcesada.getPdfUrl() + ": " + ex.getMessage());
                        }
                    }
                }
                
                emailService.enviarEmailConfirmacionConPdf(
                    pedidoFinal.getEmailCliente(),
                    pedidoFinal.getNumeroPedido(),
                    pedidoFinal.getNombreCliente(),
                    especPdfStream,
                    boletaPdfStream,
                    serieCorrelativo
                );
            } catch (Exception e) {
                System.err.println("Error al enviar email de confirmación a " + pedidoFinal.getEmailCliente() + ": " + e.getMessage());
            }
        }

        return pedidoFinal;
    }

    private Venta crearVentaDesdePedido(PedidoWeb pedido) {
        Venta venta = new Venta();
        venta.setOrigen("web");
        venta.setMetodoPago("Yape");
        venta.setTipoComprobante(determinarTipoComprobante(pedido.getDniCliente()));
        venta.setPdfKey(pedido.getPdfKey());
        venta.setNota("Pedido web " + pedido.getNumeroPedido());
        venta.setDescuento(pedido.getDescuento());

        Cliente cliente = clienteRepository.findByNumeroDocumento(pedido.getDniCliente())
                .orElseGet(() -> {
                    Cliente nuevoCliente = new Cliente();
                    nuevoCliente.setNombre(pedido.getNombreCliente());
                    nuevoCliente.setNumeroDocumento(pedido.getDniCliente());
                    nuevoCliente.setTipoDocumento(pedido.getDniCliente() != null && pedido.getDniCliente().matches("\\d{11}") ? "RUC" : "DNI");
                    nuevoCliente.setEstado(1);
                    return clienteRepository.save(nuevoCliente);
                });

        venta.setCliente(cliente);

        List<DetalleVenta> detallesVenta = new ArrayList<>();
        for (DetallePedidoWeb detalleWeb : pedido.getItems()) {
            Producto producto = productoRepository.findById(detalleWeb.getProducto().getId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + detalleWeb.getProducto().getId()));

            if (producto.getStock() < detalleWeb.getCantidad()) {
                throw new RuntimeException("Stock insuficiente para el producto: " + producto.getNombre() +
                        ". Disponible: " + producto.getStock() + ", solicitado: " + detalleWeb.getCantidad());
            }

            producto.setStock(producto.getStock() - detalleWeb.getCantidad());
            productoRepository.save(producto);

            DetalleVenta detalleVenta = new DetalleVenta();
            detalleVenta.setProducto(producto);
            detalleVenta.setCantidad(detalleWeb.getCantidad());
            detalleVenta.setPrecioUnitario(detalleWeb.getPrecioUnitario());
            detalleVenta.setVenta(venta);
            detallesVenta.add(detalleVenta);
        }
        venta.setDetalles(detallesVenta);

        return ventaService.crearVenta(venta);
    }

    private String determinarTipoComprobante(String documento) {
        if (documento == null) {
            return "Boleta";
        }
        String trimmed = documento.trim();
        if (trimmed.matches("\\d{11}")) {
            return "Factura";
        }
        return "Boleta";
    }

    private int parseCorrelativoFromNumeroVenta(String numeroVenta) {
        if (numeroVenta == null || numeroVenta.isBlank()) {
            return 0;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+)$").matcher(numeroVenta);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    @Override
    @Transactional
    public PedidoWeb rechazarPedido(Long id, Long verificadoPorId, String motivoRechazo) {
        PedidoWeb pedido = pedidoWebRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado con id: " + id));
        
        // Si el estado es PENDIENTE o EN_REVISION, se puede rechazar.
        // Si el estado es APROBADO o PROCESADO, no se puede rechazar directamente, se anula.
        // Si ya está RECHAZADO o ANULADO, no se hace nada.
        if (pedido.getEstado() == EstadoPedidoWeb.APROBADO || pedido.getEstado() == EstadoPedidoWeb.PROCESADO) {
            throw new RuntimeException("No se puede rechazar un pedido ya APROBADO o PROCESADO. Considere anularlo.");
        }
        
        // Si el pedido ya está RECHAZADO o ANULADO, no se permite cambiar el estado
        if (pedido.getEstado() == EstadoPedidoWeb.RECHAZADO || pedido.getEstado() == EstadoPedidoWeb.ANULADO) {
            throw new RuntimeException("El pedido ya se encuentra en estado " + pedido.getEstado() + ".");
        }

        // Si verificadoPorId es null, significa que la acción es "anular" desde el frontend
        Usuario verificador = null;
        if (verificadoPorId != null) {
            verificador = usuarioRepository.findById(verificadoPorId)
                    .orElseThrow(() -> new RuntimeException("Usuario verificador no encontrado con id: " + verificadoPorId));
        }
        
        pedido.setEstado(EstadoPedidoWeb.ANULADO); // Usamos ANULADO como estado final para ambos casos
        pedido.setFechaVerificacion(LocalDateTime.now());
        pedido.setVerificadoPor(verificador);
        pedido.setMotivoRechazo(motivoRechazo);
        
        // Enviar email de notificación de rechazo al cliente
        if (pedido.getEmailCliente() != null && !pedido.getEmailCliente().isBlank()) {
            try {
                emailService.enviarEmailRechazo(pedido.getEmailCliente(), pedido.getNumeroPedido(), motivoRechazo != null ? motivoRechazo : "No se especificó motivo");
            } catch (Exception e) {
                System.err.println("Error al enviar email de rechazo a " + pedido.getEmailCliente() + ": " + e.getMessage());
            }
        }
        
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
        
        // Eliminar la imagen del voucher de Cloudinary si existe
        if (pedido.getVoucherImagen() != null && !pedido.getVoucherImagen().isEmpty()) {
            try {
                cloudinaryService.deleteImageByUrl(pedido.getVoucherImagen());
                System.out.println("Voucher eliminado de Cloudinary: " + pedido.getVoucherImagen());
            } catch (Exception e) {
                System.err.println("Error al eliminar el voucher de Cloudinary: " + e.getMessage());
                // Considerar si lanzar una excepción o solo loguear el error
            }
        }
        
        // Guardar el motivo de eliminación en el campo motivoRechazo antes de eliminar
        if (motivo != null && !motivo.trim().isEmpty()) {
            pedido.setMotivoRechazo("ELIMINADO: " + motivo);
            pedidoWebRepository.save(pedido); // Guardar para registrar el motivo antes de la eliminación
        }
        
        pedidoWebRepository.deleteById(id);
        System.out.println("Pedido web eliminado de la base de datos: " + id);
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