package com.example.acceso.controller;

import com.example.acceso.dto.NotaCreditoItemDTO;
import com.example.acceso.model.DetalleVenta;
import com.example.acceso.model.Empresa;
import com.example.acceso.model.NotasCredito;
import com.example.acceso.model.Usuario;
import com.example.acceso.model.Venta;
import com.example.acceso.repository.DetalleVentaRepository;
import com.example.acceso.repository.EmpresaRepository;
import com.example.acceso.repository.NotasCreditoRepository;
import com.example.acceso.service.ApisunatService;
import com.example.acceso.service.VentaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/ventas/nota-credito")
public class NotaCreditoController {

    private static final Map<String, String> TIPOS_NC_DESCRIPCION = Map.of(
        "01", "Anulación de la operación",
        "02", "Anulación por error en el RUC",
        "03", "Corrección por error en la descripción",
        "04", "Sustitución de serie/num. comprobante",
        "05", "Descuento global",
        "06", "Devolución total",
        "07", "Devolución parcial",
        "08", "Sustitución de datos del cliente",
        "09", "Disminución en el valor",
        "10", "Otros"
    );

    private static final Logger logger = LoggerFactory.getLogger(NotaCreditoController.class);
    private final VentaService ventaService;
    private final ApisunatService apisunatService;
    private final NotasCreditoRepository notasCreditoRepository;
    private final EmpresaRepository empresaRepository;
    private final DetalleVentaRepository detalleVentaRepository;

    @Autowired
    public NotaCreditoController(VentaService ventaService,
                                 ApisunatService apisunatService,
                                 NotasCreditoRepository notasCreditoRepository,
                                 EmpresaRepository empresaRepository,
                                 DetalleVentaRepository detalleVentaRepository) {
        this.ventaService = ventaService;
        this.apisunatService = apisunatService;
        this.notasCreditoRepository = notasCreditoRepository;
        this.empresaRepository = empresaRepository;
        this.detalleVentaRepository = detalleVentaRepository;
    }

    /**
     * Muestra la página de listado de todas las notas de crédito.
     */
    @GetMapping("")
    public String listarNotasCredito() {
        return "lista-notas-credito";
    }

    /**
     * API: Lista todas las notas de crédito para DataTables.
     */
    @GetMapping("/api/listar")
    @ResponseBody
    public ResponseEntity<?> listarNotasCreditoApi(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate hasta) {
        List<NotasCredito> notas;
        if (desde != null && hasta != null) {
            LocalDateTime fechaInicio = desde.atStartOfDay();
            LocalDateTime fechaFin = hasta.atTime(23, 59, 59);
            notas = notasCreditoRepository.findByFechaEmisionBetweenOrderByFechaEmisionDesc(fechaInicio, fechaFin);
        } else {
            notas = notasCreditoRepository.findAllByOrderByFechaEmisionDesc();
        }

        List<Map<String, Object>> data = notas.stream().map(nc -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", nc.getId());
            map.put("serieCorrelativo", nc.getSerieCorrelativo());
            map.put("tipoNota", nc.getTipoNota());
            map.put("descripcionTipo", TIPOS_NC_DESCRIPCION.getOrDefault(nc.getTipoNota(), nc.getDescripcionTipo()));
            map.put("motivo", nc.getMotivo());
            map.put("totalAcreditado", nc.getTotalAcreditado());
            map.put("estadoSunat", nc.getEstadoSunat());
            map.put("fechaEmision", nc.getFechaEmision());
            map.put("pdfUrl", nc.getPdfUrl());
            map.put("xmlUrl", nc.getXmlUrl());

            // Datos de la venta asociada
            Venta v = nc.getVenta();
            if (v != null) {
                map.put("numeroVenta", v.getNumeroVenta());
                map.put("serieCorrelativoVenta", v.getSerieCorrelativo());
                map.put("totalVenta", v.getTotal());
                if (v.getCliente() != null) {
                    map.put("nombreCliente", v.getCliente().getNombre());
                    map.put("docCliente", v.getCliente().getNumeroDocumento());
                }
            }

            // Usuario que emitió
            if (nc.getEmitidaPorUsuario() != null) {
                map.put("emitidoPor", nc.getEmitidaPorUsuario().getUsuario());
            }

            return map;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    /**
     * API: Obtiene detalle completo de una nota de crédito por su ID.
     */
    @GetMapping("/api/detalle/{id}")
    @ResponseBody
    public ResponseEntity<?> detalleNotaCredito(@PathVariable Long id) {
        return notasCreditoRepository.findById(id)
                .map(nc -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", nc.getId());
                    map.put("serieCorrelativo", nc.getSerieCorrelativo());
                    map.put("tipoNota", nc.getTipoNota());
                    map.put("descripcionTipo", TIPOS_NC_DESCRIPCION.getOrDefault(nc.getTipoNota(), nc.getDescripcionTipo()));
                    map.put("motivo", nc.getMotivo());
                    map.put("totalAcreditado", nc.getTotalAcreditado());
                    map.put("estadoSunat", nc.getEstadoSunat());
                    map.put("fechaEmision", nc.getFechaEmision());
                    map.put("pdfUrl", nc.getPdfUrl());
                    map.put("xmlUrl", nc.getXmlUrl());
                    map.put("nubefactId", nc.getNubefactId());

                    Venta v = nc.getVenta();
                    if (v != null) {
                        map.put("numeroVenta", v.getNumeroVenta());
                        map.put("serieCorrelativoVenta", v.getSerieCorrelativo());
                        map.put("totalVenta", v.getTotal());
                        map.put("fechaVenta", v.getFechaVenta());
                        map.put("tipoComprobante", v.getTipoComprobante());
                        if (v.getCliente() != null) {
                            map.put("nombreCliente", v.getCliente().getNombre());
                            map.put("docCliente", v.getCliente().getNumeroDocumento());
                        }
                    }

                    if (nc.getEmitidaPorUsuario() != null) {
                        map.put("emitidoPor", nc.getEmitidaPorUsuario().getUsuario());
                    }

                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("data", map);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Nota de crédito no encontrada");
                    return ResponseEntity.badRequest().body(response);
                });
    }

    @GetMapping("/{id}")
    public String crearNotaCredito(@PathVariable Long id, Model model, HttpServletRequest request) {
        logger.info("Intentando crear nota de crédito para venta ID: {}", id);
        try {
            return ventaService.obtenerVentaDetalladaPorId(id)
                    .map(ventaMap -> {
                        logger.info("Venta encontrada: {}", ventaMap);
                        
                        // Validación 1: solo boletas y facturas
                        String tipoComprobante = (String) ventaMap.get("tipoComprobante");
                        if (tipoComprobante != null && "nota_venta".equalsIgnoreCase(tipoComprobante)) {
                            logger.warn("Intento de NC sobre nota de venta ID: {}", id);
                            return "redirect:/ventas/listar?error=nc-no-aplica-nota-venta";
                        }
                        
                        // Validación 2: no rechazadas
                        String estadoSunat = (String) ventaMap.get("estadoSunat");
                        if ("rechazado".equalsIgnoreCase(estadoSunat)) {
                            logger.warn("Intento de NC sobre comprobante rechazado ID: {}", id);
                            return "redirect:/ventas/listar?error=nc-no-aplica-rechazado";
                        }
                        
                        // Validación 3: no tiene NC total ya emitida
                        String estadoNotaCredito = (String) ventaMap.get("estadoNotaCredito");
                        if ("TOTAL".equalsIgnoreCase(estadoNotaCredito)) {
                            logger.warn("Intento de NC total sobre venta que ya tiene NC ID: {}", id);
                            return "redirect:/ventas/listar?error=nc-ya-emitida-total";
                        }
                        
                        boolean esPendiente = "pendiente".equalsIgnoreCase(estadoSunat);
                        
                        model.addAttribute("venta", ventaMap);
                        model.addAttribute("detalles", ventaMap.get("detalles"));
                        model.addAttribute("esPendiente", esPendiente);
                        
                        // Tipos de NC para el dropdown
                        List<Map<String, String>> tiposNC = List.of(
                            Map.of("codigo", "01", "descripcion",
                                "01 — Anulación de la operación (anula todo el comprobante)"),
                            Map.of("codigo", "02", "descripcion",
                                "02 — Anulación por error en el RUC del cliente"),
                            Map.of("codigo", "06", "descripcion",
                                "06 — Devolución total de productos"),
                            Map.of("codigo", "07", "descripcion",
                                "07 — Devolución parcial (por ítem específico)"),
                            Map.of("codigo", "09", "descripcion",
                                "09 — Disminución en el valor (descuento posterior)")
                        );
                        model.addAttribute("tiposNC", tiposNC);
                        
                        // Agregar token CSRF al modelo
                        if (request.getAttribute("_csrf") != null) {
                            model.addAttribute("_csrf", request.getAttribute("_csrf"));
                        }
                        
                        return "nota-credito";
                    })
                    .orElseGet(() -> {
                        logger.warn("Venta no encontrada con ID: {}", id);
                        String errorMsg = "Venta no encontrada con ID: " + id;
                        model.addAttribute("errorMessage", errorMsg);
                        model.addAttribute("errorDetalle", "La venta solicitada no existe en la base de datos.");
                        return "error";
                    });
        } catch (Exception e) {
            logger.error("Error al crear nota de crédito para venta ID: " + id, e);
            String errorMsg = "Error al procesar la nota de crédito: " + (e.getMessage() != null ? e.getMessage() : "Error desconocido");
            model.addAttribute("errorMessage", errorMsg);
            model.addAttribute("errorDetalle", "Tipo de error: " + e.getClass().getName());
            model.addAttribute("errorTecnico", e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "Sin detalles");
            return "error";
        }
    }

    @PostMapping("/{id}")
    @Transactional
    public String procesarNC(@PathVariable Long id,
                             @RequestParam String tipoNota,
                             @RequestParam String motivo,
                             @RequestParam(required = false) String itemsData,
                             HttpServletRequest request,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        logger.info("=== INICIO PROCESAMIENTO NC ===");
        logger.info("Procesando NC para venta ID: {}, tipo: {}, motivo: {}", id, tipoNota, motivo);
        logger.info("itemsData recibido: {}", itemsData);
        try {
            // Obtener la entidad Venta completa con sus detalles
            Venta venta = ventaService.obtenerEntidadVentaPorId(id)
                    .orElseThrow(() -> new RuntimeException("Venta no encontrada"));
            
            // Revalidar elegibilidad
            if ("nota_venta".equalsIgnoreCase(venta.getTipoComprobante())
                || "rechazado".equalsIgnoreCase(venta.getEstadoSunat())
                || "TOTAL".equalsIgnoreCase(venta.getEstadoNotaCredito())) {
                logger.warn("Intento de NC no permitida para venta ID: {}", id);
                return "redirect:/ventas/listar?error=nc-no-permitida";
            }
            
            // Obtener empresa
            Empresa empresa = empresaRepository.findFirstByOrderByIdAsc()
                    .orElseThrow(() -> new RuntimeException("No hay empresa configurada"));
            
            // Procesar itemsData (formato: "detalleId:cantidad,detalleId:cantidad")
            List<NotaCreditoItemDTO> itemsSeleccionados = new ArrayList<>();
            BigDecimal totalAcreditado = BigDecimal.ZERO;
            
            if (itemsData != null && !itemsData.isBlank()) {
                String[] pares = itemsData.split(",");
                for (String par : pares) {
                    String[] partes = par.split(":");
                    if (partes.length != 2) continue;
                    
                    try {
                        Long detalleId = Long.parseLong(partes[0].trim());
                        Integer cantidad = Integer.parseInt(partes[1].trim());
                        
                        if (cantidad <= 0) continue;
                        
                        // Buscar el detalle en la venta
                        Optional<DetalleVenta> detalleOpt = detalleVentaRepository.findById(detalleId);
                        if (detalleOpt.isPresent()) {
                            DetalleVenta detalle = detalleOpt.get();
                            NotaCreditoItemDTO itemDTO = new NotaCreditoItemDTO();
                            itemDTO.setDetalleVentaId(detalleId);
                            itemDTO.setCantidad(cantidad);
                            itemDTO.setDescripcion(detalle.getProducto() != null 
                                ? detalle.getProducto().getNombre() : "Producto");
                            itemDTO.setPrecioUnitario(detalle.getPrecioUnitario());
                            
                            itemsSeleccionados.add(itemDTO);
                            totalAcreditado = totalAcreditado.add(
                                detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(cantidad))
                            );
                        } else {
                            logger.warn("Detalle de venta no encontrado: {}", detalleId);
                        }
                    } catch (NumberFormatException e) {
                        logger.warn("Error parseando item: {}", par);
                    }
                }
            } else {
                // Si no hay itemsData (anulación total), usar todos los detalles
                logger.info("Sin itemsData explicitos, usando todos los detalles de la venta (anulación total)");
                if (venta.getDetalles() != null) {
                    for (DetalleVenta detalle : venta.getDetalles()) {
                        NotaCreditoItemDTO itemDTO = new NotaCreditoItemDTO();
                        itemDTO.setDetalleVentaId(detalle.getId());
                        itemDTO.setCantidad(detalle.getCantidad());
                        itemDTO.setDescripcion(detalle.getProducto() != null 
                            ? detalle.getProducto().getNombre() : "Producto");
                        itemDTO.setPrecioUnitario(detalle.getPrecioUnitario());
                        
                        itemsSeleccionados.add(itemDTO);
                        totalAcreditado = totalAcreditado.add(
                            detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad()))
                        );
                    }
                }
            }
            
            if (itemsSeleccionados.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Debe seleccionar al menos un ítem para devolver");
                return "redirect:/ventas/nota-credito/" + id;
            }
            
            logger.info("Items seleccionados: {} items, total a acreditar: S/{}", 
                itemsSeleccionados.size(), totalAcreditado);
            
            // Obtener usuario actual
            Usuario usuarioActual = obtenerUsuarioActual(request);
            
            // Obtener serie y correlativo para la nota de crédito
            boolean esFacturaOriginal = "01".equals(venta.getTipoComprobante()) 
                || "Factura".equalsIgnoreCase(venta.getTipoComprobante());
            
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
            
            logger.info("NC serie: {}, correlativo: {}", serie, correlativo);
            
            // Determinar si es anulación total o parcial
            boolean esAnulacionTotal = "01".equals(tipoNota) || "06".equals(tipoNota);
            
            // Crear entidad NotasCredito
            NotasCredito notaCredito = new NotasCredito();
            notaCredito.setVenta(venta);
            notaCredito.setTipoNota(tipoNota);
            notaCredito.setMotivo(motivo);
            notaCredito.setSerie(serie);
            notaCredito.setCorrelativo(correlativo);
            notaCredito.setSerieCorrelativo(serie + "-" + String.format("%08d", correlativo));
            notaCredito.setTotalAcreditado(totalAcreditado.setScale(2, RoundingMode.HALF_UP));
            notaCredito.setEstadoSunat("pendiente");
            notaCredito.setEmitidaPorUsuario(usuarioActual);
            notaCredito.setFechaEmision(LocalDateTime.now());
            
            // Guardar nota de crédito en BD primero
            notaCredito = notasCreditoRepository.save(notaCredito);
            logger.info("Nota de crédito guardada en BD con ID: {} - serieCorrelativo: {}", 
                notaCredito.getId(), notaCredito.getSerieCorrelativo());
            
            // Emitir en MiAPI Cloud
            logger.info("Enviando a MiAPI Cloud...");
            ApisunatService.ApisunatResult resultado = apisunatService.emitirNotaCredito(
                venta, serie, correlativo, tipoNota, motivo, itemsSeleccionados, empresa
            );
            
            logger.info("Resultado MiAPI: status={}, pdfUrl={}, xmlUrl={}, documentId={}, error={}", 
                resultado.getStatus(), resultado.getPdfUrl(), resultado.getXmlUrl(), 
                resultado.getDocumentId(), resultado.getErrorCode());
            
            // Actualizar nota de crédito con respuesta de MiAPI
            notaCredito.setEstadoSunat(resultado.getStatus() != null ? resultado.getStatus().toLowerCase() : "error");
            notaCredito.setNubefactId(resultado.getDocumentId());
            notaCredito.setPdfUrl(resultado.getPdfUrl());
            notaCredito.setXmlUrl(resultado.getXmlUrl());
            notaCredito.setHashCdr(resultado.getHashCdr());
            notaCredito.setRawResponse(resultado.getRawResponse());
            
            boolean esAceptado = "ACEPTADO".equalsIgnoreCase(resultado.getStatus());
            
            // Actualizar correlativo en empresa solo si fue exitoso o rechazado (no en excepción)
            if (esAceptado || "RECHAZADO".equalsIgnoreCase(resultado.getStatus())) {
                if (esFacturaOriginal) {
                    empresa.setCorrelativoNotaCreditoFactura(correlativo + 1);
                    logger.info("Correlativo NC Factura actualizado a: {}", correlativo + 1);
                } else {
                    empresa.setCorrelativoNotaCreditoBoleta(correlativo + 1);
                    logger.info("Correlativo NC Boleta actualizado a: {}", correlativo + 1);
                }
                empresaRepository.save(empresa);
            }
            
            // Actualizar estado de la venta original
            String estadoNCVenta = esAnulacionTotal ? "TOTAL" : "PARCIAL";
            if (esAceptado) {
                ventaService.aplicarNotaCredito(id, estadoNCVenta, itemsSeleccionados);
            }
            
            // Guardar cambios finales en nota de crédito
            notasCreditoRepository.save(notaCredito);
            
            logger.info("Nota de crédito procesada exitosamente. Estado SUNAT: {}, URL PDF: {}", 
                resultado.getStatus(), resultado.getPdfUrl());
            
            // Redirigir a vista de comprobante
            model.addAttribute("venta", ventaService.obtenerVentaDetalladaPorId(id).orElse(null));
            model.addAttribute("notaCredito", notaCredito);
            model.addAttribute("tipoNota", tipoNota);
            model.addAttribute("motivo", motivo);
            model.addAttribute("itemsSeleccionados", itemsSeleccionados);
            model.addAttribute("totalAcreditado", totalAcreditado);
            model.addAttribute("resultadoSunat", resultado);
            
            if (esAceptado) {
                model.addAttribute("mensaje", "✅ Nota de Crédito emitida y aceptada por SUNAT exitosamente");
            } else if ("RECHAZADO".equals(resultado.getStatus())) {
                model.addAttribute("mensaje", "❌ Nota de Crédito emitida pero RECHAZADA por SUNAT");
                model.addAttribute("errorDetalle", resultado.getErrorCode());
            } else if ("EXCEPCION".equals(resultado.getStatus())) {
                model.addAttribute("mensaje", "⚠️ Nota de Crédito con estado: " + resultado.getStatus());
                model.addAttribute("errorDetalle", resultado.getErrorCode() != null ? resultado.getErrorCode() : resultado.getRawResponse());
            } else {
                model.addAttribute("mensaje", "Nota de Crédito emitida con estado: " + resultado.getStatus());
            }
            
            return "nota-credito-pdf";
            
        } catch (Exception e) {
            logger.error("Error al emitir nota de crédito para venta ID: " + id, e);
            String errorMsg = "Error al procesar la nota de crédito: " + (e.getMessage() != null ? e.getMessage() : "Error desconocido");
            model.addAttribute("errorMessage", errorMsg);
            model.addAttribute("errorDetalle", "Tipo de error: " + e.getClass().getName());
            model.addAttribute("errorTecnico", e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "Sin detalles");
            return "error";
        }
    }

    @GetMapping("/{id}/ver")
    public String verNotaCredito(@PathVariable Long id, Model model) {
        return ventaService.obtenerVentaDetalladaPorId(id)
                .map(ventaMap -> {
                    model.addAttribute("venta", ventaMap);
                    return "nota-credito-pdf";
                })
                .orElseGet(() -> {
                    model.addAttribute("errorMessage", "Venta no encontrada");
                    return "error";
                });
    }

    @GetMapping("/refrescar/{ncId}")
    @ResponseBody
    public ResponseEntity<?> refrescarNC(@PathVariable Long ncId) {
        logger.info("Refrescando estado de NC ID: {}", ncId);
        try {
            NotasCredito nc = notasCreditoRepository.findById(ncId)
                    .orElseThrow(() -> new RuntimeException("NC no encontrada"));

            if (nc.getNubefactId() == null || nc.getNubefactId().isBlank()) {
                logger.warn("NC ID {} sin documentId de APISUNAT", ncId);
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "NC sin documentId de APISUNAT"));
            }

            logger.info("Consultando documento APISUNAT con ID: {}", nc.getNubefactId());
            ApisunatService.ApisunatResult detalle =
                    apisunatService.consultarDocumento(nc.getNubefactId());

            logger.info("Resultado consulta: status={}, pdfUrl={}, xmlUrl={}", 
                detalle.getStatus(), detalle.getPdfUrl(), detalle.getXmlUrl());

            if (detalle.getPdfUrl() != null) nc.setPdfUrl(detalle.getPdfUrl());
            if (detalle.getXmlUrl() != null) nc.setXmlUrl(detalle.getXmlUrl());
            if (detalle.getHashCdr() != null) nc.setHashCdr(detalle.getHashCdr());
            if (detalle.getStatus() != null)
                nc.setEstadoSunat(detalle.getStatus().toLowerCase());
            notasCreditoRepository.save(nc);

            logger.info("NC ID {} actualizada. Estado: {}", ncId, nc.getEstadoSunat());

            return ResponseEntity.ok(Map.of(
                    "estadoSunat", nc.getEstadoSunat() != null ? nc.getEstadoSunat() : "",
                    "pdfUrl", nc.getPdfUrl() != null ? nc.getPdfUrl() : "",
                    "xmlUrl", nc.getXmlUrl() != null ? nc.getXmlUrl() : ""
            ));
        } catch (Exception e) {
            logger.error("Error al refrescar NC ID: " + ncId, e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Error al consultar APISUNAT: " + e.getMessage()));
        }
    }
    
    private Usuario obtenerUsuarioActual(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object usuarioObj = session.getAttribute("usuarioLogueado");
            if (usuarioObj instanceof Usuario) {
                return (Usuario) usuarioObj;
            }
        }
        return null;
    }
}