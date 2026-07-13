package com.example.acceso.service;

import com.example.acceso.model.Cliente;
import com.example.acceso.model.DetalleVenta;
import com.example.acceso.model.Empresa;
import com.example.acceso.model.NotasCredito;
import com.example.acceso.model.Producto;
import com.example.acceso.model.Venta;
import com.example.acceso.repository.ClienteRepository;
import com.example.acceso.repository.ProductoRepository;
import com.example.acceso.repository.VentaRepository;
import com.example.acceso.dto.ProductoMasVendidoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VentaServiceImpl implements VentaService {

    private static final Logger logger = LoggerFactory.getLogger(VentaServiceImpl.class);

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final EmpresaService empresaService;
    private final ApisunatService apisunatService;
    private final FechaHoraService fechaHoraService;

    @Autowired
    public VentaServiceImpl(VentaRepository ventaRepository,
                            ProductoRepository productoRepository,
                            ClienteRepository clienteRepository,
                            EmpresaService empresaService,
                            ApisunatService apisunatService,
                            FechaHoraService fechaHoraService) {
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
        this.clienteRepository = clienteRepository;
        this.empresaService = empresaService;
        this.apisunatService = apisunatService;
        this.fechaHoraService = fechaHoraService;
    }

    @Override
    @Transactional
    public Venta crearVenta(Venta venta) {
        venta.setTipoComprobante(normalizeTipoComprobante(venta.getTipoComprobante()));

        // Lógica para manejar el cliente en ventas web
        if ("web".equals(venta.getOrigen())) {
            // ...
        } else {
            // Lógica para ventas de punto de venta (POS)
            venta.setEstado(1); // Activa
        }

        // --- Lógica de cálculo y guardado ---
        BigDecimal subtotalCalculado = BigDecimal.ZERO;
        List<DetalleVenta> detalles = venta.getDetalles() != null ? venta.getDetalles() : new ArrayList<>();
        venta.setDetalles(detalles); // Asegurar que la lista se asigne a la venta
        for (DetalleVenta detalle : detalles) {
            Producto producto = productoRepository.findById(detalle.getProducto().getId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + detalle.getProducto().getId()));
            
            detalle.setPrecioUnitario(producto.getPrecio());
            BigDecimal subtotalDetalle = producto.getPrecio().multiply(new BigDecimal(detalle.getCantidad()));
            detalle.setSubtotal(subtotalDetalle);
            subtotalCalculado = subtotalCalculado.add(subtotalDetalle);

            // Descontar stock solo para ventas que no son web (procesadas inmediatamente)
            if (!"web".equals(venta.getOrigen())) {
                int nuevoStock = producto.getStock() - detalle.getCantidad();
                if (nuevoStock < 0) {
                    throw new RuntimeException("Stock insuficiente para el producto: " + producto.getNombre());
                }
                producto.setStock(nuevoStock);
                productoRepository.save(producto);
            }
            detalle.setVenta(venta);
        }

        venta.setSubtotal(subtotalCalculado);

        // Validar y calcular descuento
        BigDecimal descuento = venta.getDescuento() != null ? venta.getDescuento() : BigDecimal.ZERO;
        if (descuento.compareTo(BigDecimal.ZERO) < 0) {
            descuento = BigDecimal.ZERO;
        }
        if (descuento.compareTo(subtotalCalculado) > 0) {
            throw new RuntimeException("El descuento no puede ser mayor que el subtotal.");
        }
        venta.setDescuento(descuento);

        // Calcular total final
        BigDecimal totalFinal = subtotalCalculado.subtract(descuento);
        venta.setTotal(totalFinal);

        // Generar número de venta y fecha
        String numeroVenta = generarNumeroVenta(venta.getTipoComprobante());
        venta.setNumeroVenta(numeroVenta);
        venta.setFechaVenta(fechaHoraService.ahora());

        if ("Boleta".equalsIgnoreCase(venta.getTipoComprobante()) || "Factura".equalsIgnoreCase(venta.getTipoComprobante())) {
            venta.setEstadoSunat("pendiente");
        }

        return ventaRepository.save(venta);
    }

    private String normalizeTipoComprobante(String tipoComprobante) {
        if (tipoComprobante == null) {
            return null;
        }
        String normalized = tipoComprobante.trim().toLowerCase();
        if (normalized.contains("boleta")) {
            return "Boleta";
        }
        if (normalized.contains("factura")) {
            return "Factura";
        }
        if (normalized.contains("nota")) {
            return "Nota de Venta";
        }
        return tipoComprobante;
    }

    private String generarNumeroVenta(String tipoComprobante) {
        String prefijo;
        switch (tipoComprobante) {
            case "Boleta":
                prefijo = "B";
                break;
            case "Factura":
                prefijo = "F";
                break;
            case "Nota de Venta":
                prefijo = "N";
                break;
            default:
                prefijo = "V";
        }

        Integer maxCorrelativo = ventaRepository.findMaxCorrelativoByNumeroVentaPrefijo(prefijo);

        int correlativo = 1;
        if (maxCorrelativo != null && maxCorrelativo >= 0) {
            correlativo = maxCorrelativo + 1;
        }

        return String.format("%s%04d", prefijo, correlativo);
    }

    @Transactional
    public Venta procesarComprobanteElectronico(Venta venta) {
        if (venta == null || venta.getTipoComprobante() == null) {
            return venta;
        }

        // Asegurar que la venta tenga cargados todos los detalles y productos
        if (venta.getDetalles() == null || venta.getDetalles().isEmpty() || 
            (venta.getDetalles().get(0).getProducto() == null)) {
            Venta ventaCompleta = ventaRepository.findByIdWithDetallesAndProductos(venta.getId())
                    .orElseThrow(() -> new RuntimeException("Venta no encontrada con id: " + venta.getId()));
            venta.setDetalles(ventaCompleta.getDetalles());
        }

        String tipoComprobante = normalizeTipoComprobante(venta.getTipoComprobante());
        venta.setTipoComprobante(tipoComprobante);

        // --- Idempotencia: si ya está emitido (aceptado/pendiente) con evidencias, NO reemitir ---
        // Esto evita duplicidad cuando el front reintenta después de un fallo parcial.
        boolean yaTieneEvidencia =
                (venta.getEstadoSunat() != null && (
                        "aceptado".equalsIgnoreCase(venta.getEstadoSunat()) ||
                        "pendiente".equalsIgnoreCase(venta.getEstadoSunat()) ||
                        "rechazado".equalsIgnoreCase(venta.getEstadoSunat())
                ))
                && (
                        (venta.getPdfUrl() != null && !venta.getPdfUrl().isBlank()) ||
                        (venta.getXmlUrl() != null && !venta.getXmlUrl().isBlank()) ||
                        (venta.getHashCdr() != null && !venta.getHashCdr().isBlank()) ||
                        (venta.getCdrSunat() != null && !venta.getCdrSunat().isBlank())
                );

        if (yaTieneEvidencia) {
            // Aun así recortamos para asegurar compatibilidad con el esquema (vía UPDATE) evitando el 255.
            truncarCamposParaPersistencia(venta);
            return ventaRepository.save(venta);
        }

        Empresa empresa = empresaService.getEmpresaInfo();
        ApisunatService.ApisunatResult resultado;
        String serie;
        int correlativo;

        if ("Boleta".equalsIgnoreCase(tipoComprobante)) {
            serie = empresa.getSerieBoleta();
            // Obtener último correlativo de APISUNAT
            Integer lastCorrelativo = apisunatService.getLastDocument("03", serie);
            correlativo = (lastCorrelativo != null)
                    ? lastCorrelativo + 1
                    : (empresa.getCorrelativoBoleta() != null ? empresa.getCorrelativoBoleta() : 1);
            resultado = apisunatService.emitirBoleta(venta, empresa, serie, correlativo);
        } else if ("Factura".equalsIgnoreCase(tipoComprobante)) {
            serie = empresa.getSerieFactura();
            // Obtener último correlativo de APISUNAT
            Integer lastCorrelativo = apisunatService.getLastDocument("01", serie);
            correlativo = (lastCorrelativo != null)
                    ? lastCorrelativo + 1
                    : (empresa.getCorrelativoFactura() != null ? empresa.getCorrelativoFactura() : 1);
            resultado = apisunatService.emitirFactura(venta, empresa, serie, correlativo);
        } else {
            return venta;
        }

        // Generar serie-correlativo en formato SUNAT: B0001-00000001
        String prefijo = "Boleta".equalsIgnoreCase(tipoComprobante) ? "B" : "F";
        String serieFormateada = String.format("%s%04d", prefijo, 
            serie != null && serie.length() > 1 ? Integer.parseInt(serie.substring(1)) : 1);
        String correlativoFormateado = String.format("%08d", correlativo);
        String serieCorrelativoSUNAT = serieFormateada + "-" + correlativoFormateado;

        // Sólo asignar serie/correlativo y URIs si el servicio devolvió información
        String status = resultado.getStatus();
        if (status == null) {
            status = "EXCEPCION";
        }

        if ("ACEPTADO".equalsIgnoreCase(status) || "PENDIENTE".equalsIgnoreCase(status)) {
            venta.setSerieCorrelativo(serieCorrelativoSUNAT);
            venta.setPdfUrl(resultado.getPdfUrl());
            venta.setXmlUrl(resultado.getXmlUrl());
            venta.setHashCdr(resultado.getHashCdr());
            venta.setCdrSunat(resultado.getRawResponse());
            venta.setNubefactId(resultado.getDocumentId());
        } else if ("RECHAZADO".equalsIgnoreCase(status)) {
            venta.setSerieCorrelativo(serieCorrelativoSUNAT);
            venta.setCdrSunat(resultado.getRawResponse());
            venta.setNubefactId(resultado.getDocumentId());
        } else {
            // En caso de excepción dejamos los campos relacionados a SUNAT sin asignar o con detalle
            venta.setCdrSunat(resultado.getRawResponse());
            venta.setNubefactId(resultado.getDocumentId());
        }

        if ("ACEPTADO".equalsIgnoreCase(status)) {
            venta.setEstadoSunat("aceptado");
            if ("Boleta".equalsIgnoreCase(tipoComprobante)) {
                empresa.setCorrelativoBoleta(correlativo + 1);
            } else {
                empresa.setCorrelativoFactura(correlativo + 1);
            }
            empresaService.saveEmpresa(empresa);
        } else if ("PENDIENTE".equalsIgnoreCase(status)) {
            venta.setEstadoSunat("pendiente");
            venta.setNota("SUNAT pendiente: " + (resultado.getDocumentId() != null ? resultado.getDocumentId() : "sin documento"));
            // Guardar las URLs del PDF y XML aunque esté pendiente
            venta.setPdfUrl(resultado.getPdfUrl());
            venta.setXmlUrl(resultado.getXmlUrl());
        } else if ("RECHAZADO".equalsIgnoreCase(status)) {
            venta.setEstadoSunat("rechazado");
            venta.setNota("SUNAT rechazado: " + (resultado.getErrorCode() != null ? resultado.getErrorCode() : "sin código"));
            if ("Boleta".equalsIgnoreCase(tipoComprobante)) {
                empresa.setCorrelativoBoleta(correlativo + 1);
            } else {
                empresa.setCorrelativoFactura(correlativo + 1);
            }
            empresaService.saveEmpresa(empresa);
        } else {
            venta.setEstadoSunat("error");
            venta.setNota("SUNAT excepción: " + (resultado.getErrorCode() != null ? resultado.getErrorCode() : "sin detalle"));
        }

        // --- Protección por longitud (255) para evitar el error SQL ---
        truncarCamposParaPersistencia(venta);

        return ventaRepository.save(venta);
    }

    private void truncarCamposParaPersistencia(Venta venta) {
        if (venta == null) return;

        // El error menciona character varying(255). Aunque JPA tenga TEXT en algunos campos,
        // recortamos los campos con riesgo (los típicamente mapeados a varchar(255)).
        venta.setHashCdr(truncar255(venta.getHashCdr()));
        venta.setPdfUrl(truncar255(venta.getPdfUrl()));
        venta.setXmlUrl(truncar255(venta.getXmlUrl()));
        venta.setNubefactId(truncar255(venta.getNubefactId()));
        venta.setNota(truncar255(venta.getNota()));
        venta.setCdrSunat(truncar255(venta.getCdrSunat())); // seguridad extra ante discrepancias de esquema
        venta.setPdfKey(truncar255(venta.getPdfKey()));
    }

    private String truncar255(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.length() <= 255) return v;
        return v.substring(0, 255);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Venta> obtenerVentaPorId(Long id) {
        return ventaRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Venta> obtenerEntidadVentaPorId(Long id) {
        return ventaRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarTodasLasVentas() {
        return ventaRepository.findAllWithNotasCredito().stream()
                .map(this::convertVentaToMap)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarVentasActivas() {
        return ventaRepository.findByEstado(1).stream()
                .map(this::convertVentaToMap)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> buscarVentasPorRangoDeFechas(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return ventaRepository.findByFechaVentaBetween(fechaInicio, fechaFin).stream()
                .filter(venta -> venta.getEstado() != 2)
                .map(this::convertVentaToMap)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarVentasConNotasCredito() {
        return ventaRepository.findAllWithNotasCredito().stream()
                .filter(venta -> venta.getEstado() != 2)
                .map(this::convertVentaToMap)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Optional<Venta> cambiarEstadoVenta(Long id) {
        return ventaRepository.findById(id).map(venta -> {
            if (venta.getEstado() == 1) {
                venta.setEstado(0);
            } else if (venta.getEstado() == 0) {
                venta.setEstado(1);
            }
            return ventaRepository.save(venta);
        });
    }

    @Override
    @Transactional
    public void eliminarVenta(Long id) {
        ventaRepository.findById(id).ifPresent(venta -> {
            // Validación Fase 9 — solo Notas de Venta pueden anularse directamente
            String tipoComprobante = venta.getTipoComprobante();
            String estadoSunat = venta.getEstadoSunat();

            boolean esNotaVenta = tipoComprobante != null && "nota_venta".equalsIgnoreCase(tipoComprobante);

            // Si NO es Nota de Venta y SUNAT NO está rechazado => bloquear anulación directa
            if (!esNotaVenta && estadoSunat != null && !"rechazado".equalsIgnoreCase(estadoSunat)) {
                throw new RuntimeException(
                    "Las boletas y facturas electrónicas no pueden anularse directamente. Usa la opción Nota de Crédito."
                );
            }

            // Si la venta no estaba ya eliminada (estado 2), procedemos a devolver stock
            if (venta.getEstado() != 2) {
                List<DetalleVenta> detalles = venta.getDetalles() != null ? venta.getDetalles() : new ArrayList<>();
                // Devolver stock de cada producto en la venta
                for (DetalleVenta detalle : detalles) {
                    Producto producto = detalle.getProducto();
                    if (producto != null) {
                        int nuevoStock = producto.getStock() + detalle.getCantidad();
                        producto.setStock(nuevoStock);
                        productoRepository.save(producto);
                    }
                }
                // Cambiar estado a eliminado/anulado
                venta.setEstado(2);
                ventaRepository.save(venta);
            }
        });
    }

    @Override
    @Transactional
    public void actualizarVenta(Long id, Venta ventaActualizada) {
        Venta ventaExistente = ventaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada con id: " + id));

        // Devolver stock de la venta original
        List<DetalleVenta> detallesExistentes = ventaExistente.getDetalles() != null ? ventaExistente.getDetalles() : new ArrayList<>();
        for (DetalleVenta detalleExistente : detallesExistentes) {
            Producto producto = detalleExistente.getProducto();
            producto.setStock(producto.getStock() + detalleExistente.getCantidad());
            productoRepository.save(producto);
        }

        // Actualizar datos maestros de la venta
        if (ventaActualizada.getCliente() != null && ventaActualizada.getCliente().getId() != null) {
            Cliente cliente = clienteRepository.findById(ventaActualizada.getCliente().getId())
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado con id: " + ventaActualizada.getCliente().getId()));
            ventaExistente.setCliente(cliente);
        }
        ventaExistente.setMetodoPago(ventaActualizada.getMetodoPago());
        ventaExistente.setTipoComprobante(ventaActualizada.getTipoComprobante());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String formattedDateTime = fechaHoraService.ahora().format(formatter);
        String nota = "COMPROBANTE MODIFICADO (" + formattedDateTime + ")";
        ventaExistente.setNota(nota);

        // Limpiar detalles antiguos y calcular nuevos
        detallesExistentes.clear();
        ventaExistente.setDetalles(detallesExistentes);
        BigDecimal subtotalCalculado = BigDecimal.ZERO;

        List<DetalleVenta> detallesNuevos = ventaActualizada.getDetalles() != null ? ventaActualizada.getDetalles() : new ArrayList<>();
        for (DetalleVenta detalleNuevo : detallesNuevos) {
            Producto producto = productoRepository.findById(detalleNuevo.getProducto().getId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + detalleNuevo.getProducto().getId()));

            // Recalcular subtotal y descontar nuevo stock
            detalleNuevo.setPrecioUnitario(producto.getPrecio());
            BigDecimal subtotalDetalle = producto.getPrecio().multiply(new BigDecimal(detalleNuevo.getCantidad()));
            detalleNuevo.setSubtotal(subtotalDetalle);
            subtotalCalculado = subtotalCalculado.add(subtotalDetalle);

            int nuevoStock = producto.getStock() - detalleNuevo.getCantidad();
            if (nuevoStock < 0) {
                throw new RuntimeException("Stock insuficiente para el producto: " + producto.getNombre());
            }
            producto.setStock(nuevoStock);
            productoRepository.save(producto);

            detalleNuevo.setVenta(ventaExistente);
            detallesExistentes.add(detalleNuevo);
        }

        ventaExistente.setSubtotal(subtotalCalculado);

        // Aplicar descuento
        BigDecimal descuento = ventaActualizada.getDescuento() != null ? ventaActualizada.getDescuento() : BigDecimal.ZERO;
        if (descuento.compareTo(BigDecimal.ZERO) < 0) {
            descuento = BigDecimal.ZERO;
        }
        if (descuento.compareTo(subtotalCalculado) > 0) {
            throw new RuntimeException("El descuento no puede ser mayor que el subtotal.");
        }
        ventaExistente.setDescuento(descuento);

        // Calcular total final
        BigDecimal totalFinal = subtotalCalculado.subtract(descuento);
        ventaExistente.setTotal(totalFinal);

        ventaRepository.save(ventaExistente);
    }

    @Override
    @Transactional
    public void aplicarNotaCredito(Long ventaId, String estadoNotaCredito, List<com.example.acceso.dto.NotaCreditoItemDTO> itemsSeleccionados) {
        Venta ventaExistente = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada con id: " + ventaId));

        // Solo actualizamos el estado de la NC (NO recalculamos subtotal/total ni limpiamos detalles).
        ventaExistente.setEstadoNotaCredito(estadoNotaCredito);

        // Devolución de stock por ítems seleccionados (si aplica).
        if (itemsSeleccionados != null && !itemsSeleccionados.isEmpty()) {
            for (com.example.acceso.dto.NotaCreditoItemDTO item : itemsSeleccionados) {
                if (item == null || item.getDetalleVentaId() == null) continue;
                // Recuperar detalle para obtener el producto real
                // Nota: como el DTO solo tiene detalleVentaId y precio/cantidad, buscamos el producto a través de la venta.
                if (ventaExistente.getDetalles() == null) continue;

                for (DetalleVenta detalle : ventaExistente.getDetalles()) {
                    if (detalle == null || detalle.getId() == null) continue;
                    if (!detalle.getId().equals(item.getDetalleVentaId())) continue;

                    Producto producto = detalle.getProducto();
                    if (producto == null || item.getCantidad() == null) continue;

                    producto.setStock(producto.getStock() + item.getCantidad());
                    productoRepository.save(producto);
                    break;
                }
            }
        }

        ventaRepository.save(ventaExistente);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> obtenerVentaDetalladaPorId(Long id) {
        return ventaRepository.findByIdWithDetallesAndProductos(id).map(this::convertVentaToDetalleMap);
    }

    private Map<String, Object> convertVentaToMap(Venta venta) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", venta.getId());
        map.put("numeroVenta", venta.getNumeroVenta());

        if (venta.getCliente() != null) {
            map.put("nombreCliente", venta.getCliente().getNombre());
        } else {
            map.put("nombreCliente", "Cliente no disponible");
        }
        map.put("fechaVenta", venta.getFechaVenta() != null ? venta.getFechaVenta() : null);
        map.put("metodoPago", venta.getMetodoPago() != null ? venta.getMetodoPago() : "N/A");
        map.put("tipoComprobante", venta.getTipoComprobante() != null ? venta.getTipoComprobante() : "Nota de Venta");
        map.put("serieCorrelativo", venta.getSerieCorrelativo() != null ? venta.getSerieCorrelativo() : "");
        map.put("estadoSunat", venta.getEstadoSunat() != null ? venta.getEstadoSunat() : "");
        map.put("pdfUrl", venta.getPdfUrl());
        map.put("xmlUrl", venta.getXmlUrl());
        map.put("descuento", venta.getDescuento() != null ? venta.getDescuento() : BigDecimal.ZERO);
        map.put("total", venta.getTotal() != null ? venta.getTotal() : BigDecimal.ZERO);
        map.put("estado", venta.getEstado() != null ? venta.getEstado() : 1);
        map.put("nota", venta.getNota() != null ? venta.getNota() : "");

        // Cambio 1: incluir datos de la nota de crédito vinculada
        map.put("estadoNotaCredito", venta.getEstadoNotaCredito());

        List<NotasCredito> notasCredito = venta.getNotasCredito();
        if (notasCredito != null && !notasCredito.isEmpty()) {
            NotasCredito nc = notasCredito.get(0);
            map.put("ncSerieCorrelativo", nc.getSerieCorrelativo());
            map.put("ncPdfUrl", nc.getPdfUrl());
            map.put("ncXmlUrl", nc.getXmlUrl());
            map.put("ncTotalAcreditado", nc.getTotalAcreditado());
        } else {
            map.put("ncSerieCorrelativo", null);
            map.put("ncPdfUrl", null);
            map.put("ncXmlUrl", null);
            map.put("ncTotalAcreditado", null);
        }

        return map;
    }

    private Map<String, Object> convertVentaToDetalleMap(Venta venta) {
        Map<String, Object> ventaMap = new HashMap<>();
        ventaMap.put("id", venta.getId());
        ventaMap.put("numeroVenta", venta.getNumeroVenta() != null ? venta.getNumeroVenta() : "N/A");
        ventaMap.put("metodoPago", venta.getMetodoPago() != null ? venta.getMetodoPago() : "Efectivo");
        ventaMap.put("tipoComprobante", venta.getTipoComprobante() != null ? venta.getTipoComprobante() : "Nota de Venta");
        ventaMap.put("total", venta.getTotal() != null ? venta.getTotal() : BigDecimal.ZERO);
        ventaMap.put("subtotal", venta.getSubtotal() != null ? venta.getSubtotal() : BigDecimal.ZERO);
        ventaMap.put("descuento", venta.getDescuento() != null ? venta.getDescuento() : BigDecimal.ZERO);
        ventaMap.put("fechaVenta", venta.getFechaVenta() != null ? venta.getFechaVenta() : null);
        ventaMap.put("estadoSunat", venta.getEstadoSunat() != null ? venta.getEstadoSunat() : "");
        ventaMap.put("serieCorrelativo", venta.getSerieCorrelativo() != null ? venta.getSerieCorrelativo() : "");

        if (venta.getCliente() != null) {
            Map<String, Object> clienteMap = new HashMap<>();
            clienteMap.put("id", venta.getCliente().getId());
            clienteMap.put("nombre", venta.getCliente().getNombre() != null ? venta.getCliente().getNombre() : "Cliente General");
            clienteMap.put("tipoDocumento", venta.getCliente().getTipoDocumento() != null ? venta.getCliente().getTipoDocumento() : "DNI");
            clienteMap.put("numeroDocumento", venta.getCliente().getNumeroDocumento() != null ? venta.getCliente().getNumeroDocumento() : "");
            ventaMap.put("cliente", clienteMap);
        } else {
            Map<String, Object> clienteMap = new HashMap<>();
            clienteMap.put("id", null);
            clienteMap.put("nombre", "Cliente General");
            clienteMap.put("tipoDocumento", "DNI");
            clienteMap.put("numeroDocumento", "00000000");
            ventaMap.put("cliente", clienteMap);
        }

        List<DetalleVenta> detalles = venta.getDetalles() != null ? venta.getDetalles() : new ArrayList<>();
        List<Map<String, Object>> detallesList = detalles.stream().map(detalle -> {
            Map<String, Object> detalleMap = new HashMap<>();
            detalleMap.put("id", detalle.getId());
            detalleMap.put("cantidad", detalle.getCantidad());
            detalleMap.put("precioUnitario", detalle.getPrecioUnitario() != null ? detalle.getPrecioUnitario() : BigDecimal.ZERO);
            detalleMap.put("subtotal", detalle.getSubtotal() != null ? detalle.getSubtotal() : BigDecimal.ZERO);

            if (detalle.getProducto() != null) {
                Map<String, Object> productoMap = new HashMap<>();
                productoMap.put("id", detalle.getProducto().getId());
                String nombreProducto = detalle.getProducto().getNombre();
                String descripcionProducto = detalle.getProducto().getDescripcion();
                // Si el nombre es null, usar descripción, si no, usar ID del producto
                String nombreMostrar = (nombreProducto != null && !nombreProducto.trim().isEmpty()) 
                    ? nombreProducto 
                    : (descripcionProducto != null && !descripcionProducto.trim().isEmpty()) 
                        ? descripcionProducto 
                        : ("Producto " + detalle.getProducto().getId());
                productoMap.put("nombre", nombreMostrar);
                // Log para debugging
                logger.debug("Producto ID: {} -> Nombre mostrado: '{}' (nombre BD: '{}', descripción BD: '{}')", 
                    detalle.getProducto().getId(), nombreMostrar, nombreProducto, descripcionProducto);
                productoMap.put("precio", detalle.getProducto().getPrecio() != null ? detalle.getProducto().getPrecio() : BigDecimal.ZERO);
                detalleMap.put("stock", detalle.getProducto().getStock());
                detalleMap.put("producto", productoMap);
            } else {
                Map<String, Object> productoMap = new HashMap<>();
                productoMap.put("id", null);
                productoMap.put("nombre", "Producto no disponible");
                productoMap.put("precio", BigDecimal.ZERO);
                detalleMap.put("stock", 0);
                detalleMap.put("producto", productoMap);
            }
            return detalleMap;
        }).collect(Collectors.toList());

        ventaMap.put("detalles", detallesList);

        return ventaMap;
    }

    @Override
    public long obtenerNumeroVentasDiarias() {
        LocalDate today = fechaHoraService.hoy();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        return ventaRepository.countByFechaVenta(startOfDay, endOfDay);
    }

    @Override
    public BigDecimal obtenerTotalVentasDiarias() {
        LocalDate today = fechaHoraService.hoy();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        BigDecimal total = ventaRepository.sumTotalByFechaVenta(startOfDay, endOfDay);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    public long obtenerNumeroVentasMensuales() {
        YearMonth currentMonth = YearMonth.from(fechaHoraService.ahora());
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(LocalTime.MAX);
        return ventaRepository.countByFechaVentaMonth(startOfMonth, endOfMonth);
    }

    @Override
    public BigDecimal obtenerTotalVentasMensuales() {
        YearMonth currentMonth = YearMonth.from(fechaHoraService.ahora());
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(LocalTime.MAX);
        BigDecimal total = ventaRepository.sumTotalByFechaVentaMonth(startOfMonth, endOfMonth);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoMasVendidoDTO> obtenerTop5ProductosMasVendidosDeLaSemana() {
        LocalDate today = fechaHoraService.hoy();
        LocalDateTime inicioSemana = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime finSemana = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(LocalTime.MAX);

        List<Object[]> resultados = ventaRepository.findTop5ProductosMasVendidosDeLaSemana(inicioSemana, finSemana);

        return resultados.stream().map(resultado -> {
            String nombreProducto = (String) resultado[0];
            Long unidadesVendidas = ((Number) resultado[1]).longValue();
            BigDecimal totalDinero = (BigDecimal) resultado[2];
            return new ProductoMasVendidoDTO(nombreProducto, unidadesVendidas, totalDinero);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoMasVendidoDTO> obtenerProductosMasVendidosDeHoy() {
        LocalDate today = fechaHoraService.hoy();
        LocalDateTime inicioHoy = today.atStartOfDay();
        LocalDateTime finHoy = today.atTime(LocalTime.MAX);

        List<Venta> ventasHoy = ventaRepository.findByFechaVentaBetween(inicioHoy, finHoy).stream()
                .filter(v -> v.getEstado() == null || v.getEstado() != 2)
                .collect(Collectors.toList());

        Map<Long, ProductoMasVendidoDTO> acumulador = new HashMap<>();

        for (Venta venta : ventasHoy) {
            if (venta.getDetalles() == null) continue;
            for (DetalleVenta detalle : venta.getDetalles()) {
                if (detalle.getProducto() == null) continue;
                Long productoId = detalle.getProducto().getId();
                String nombre = detalle.getProducto().getNombre();
                Long unidades = (long) detalle.getCantidad();
                BigDecimal total = detalle.getPrecioUnitario() != null ? detalle.getPrecioUnitario().multiply(new BigDecimal(detalle.getCantidad())) : BigDecimal.ZERO;

                ProductoMasVendidoDTO dto = acumulador.get(productoId);
                if (dto == null) {
                    dto = new ProductoMasVendidoDTO(nombre, unidades, total);
                    acumulador.put(productoId, dto);
                } else {
                    dto.setUnidadesVendidas(dto.getUnidadesVendidas() + unidades);
                    dto.setTotalDinero(dto.getTotalDinero().add(total));
                }
            }
        }

        return acumulador.values().stream()
                .sorted((a, b) -> Long.compare(b.getUnidadesVendidas(), a.getUnidadesVendidas()))
                .limit(5)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarVentasWebPendientes() {
        return ventaRepository.findByEstado(3).stream()
                .map(this::convertVentaToMap)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void procesarVentaWeb(Long id) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada con id: " + id));

        if (venta.getEstado() != 3) {
            throw new RuntimeException("La venta no está pendiente de procesamiento.");
        }

        List<DetalleVenta> detalles = venta.getDetalles() != null ? venta.getDetalles() : new ArrayList<>();
        for (DetalleVenta detalle : detalles) {
            Producto producto = productoRepository.findById(detalle.getProducto().getId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + detalle.getProducto().getId()));
            int nuevoStock = producto.getStock() - detalle.getCantidad();
            if (nuevoStock < 0) {
                throw new RuntimeException("Stock insuficiente para el producto: " + producto.getNombre());
            }
            producto.setStock(nuevoStock);
            productoRepository.save(producto);
        }

        venta.setEstado(1);
        ventaRepository.save(venta);
    }
}
