package com.example.acceso.controller;

import com.example.acceso.dto.NotaCreditoItemDTO;
import com.example.acceso.model.DetalleVenta;
import com.example.acceso.model.Empresa;
import com.example.acceso.model.NotasCredito;
import com.example.acceso.model.Usuario;
import com.example.acceso.model.Venta;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/ventas/nota-credito")
public class NotaCreditoController {

    private static final Logger logger = LoggerFactory.getLogger(NotaCreditoController.class);
    private final VentaService ventaService;
    private final ApisunatService apisunatService;
    private final NotasCreditoRepository notasCreditoRepository;
    private final EmpresaRepository empresaRepository;

    @Autowired
    public NotaCreditoController(VentaService ventaService,
                                 ApisunatService apisunatService,
                                 NotasCreditoRepository notasCreditoRepository,
                                 EmpresaRepository empresaRepository) {
        this.ventaService = ventaService;
        this.apisunatService = apisunatService;
        this.notasCreditoRepository = notasCreditoRepository;
        this.empresaRepository = empresaRepository;
    }

    @GetMapping("/{id}")
    public String crearNotaCredito(@PathVariable Long id, Model model, HttpServletRequest request) {
        logger.info("Intentando crear nota de crédito para venta ID: {}", id);
        try {
            return ventaService.obtenerVentaDetalladaPorId(id)
                    .map(ventaMap -> {
                        logger.info("Venta encontrada: {}", ventaMap);
                        logger.info("numeroVenta: {}", ventaMap.get("numeroVenta"));
                        logger.info("total: {}", ventaMap.get("total"));
                        logger.info("cliente: {}", ventaMap.get("cliente"));
                        
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
                             @RequestParam(required = false) Map<String, String> items,
                             HttpServletRequest request,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        logger.info("=== INICIO PROCESAMIENTO NC ===");
        logger.info("Procesando NC para venta ID: {}, tipo: {}, motivo: {}", id, tipoNota, motivo);
        logger.info("Items recibidos: {}", items != null ? items.keySet() : "null");
        try {
            // Obtener la entidad Venta completa
            Venta venta = ventaService.obtenerEntidadVentaPorId(id)
                    .orElseThrow(() -> new RuntimeException("Venta no encontrada"));
            
            // Revalidar elegibilidad (por si acaso el usuario fue directo al POST)
            if ("nota_venta".equalsIgnoreCase(venta.getTipoComprobante())
                || "rechazado".equalsIgnoreCase(venta.getEstadoSunat())
                || "TOTAL".equalsIgnoreCase(venta.getEstadoNotaCredito())) {
                logger.warn("Intento de NC no permitida para venta ID: {}", id);
                return "redirect:/ventas/listar?error=nc-no-permitida";
            }
            
            // Obtener empresa
            Empresa empresa = empresaRepository.findFirstByOrderByIdAsc()
                    .orElseThrow(() -> new RuntimeException("No hay empresa configurada"));
            
            // Procesar ítems seleccionados
            List<NotaCreditoItemDTO> itemsSeleccionados = new ArrayList<>();
            BigDecimal totalAcreditado = BigDecimal.ZERO;
            
            if (items != null && !items.isEmpty()) {
                // Los items vienen como items[detalleId].id y items[detalleId].cantidad
                for (Map.Entry<String, String> entry : items.entrySet()) {
                    String key = entry.getKey();
                    if (key.endsWith(".id")) {
                        // Extraer el ID del formato items[detalleId].id
                        int startIdx = key.indexOf('[');
                        int endIdx = key.indexOf(']');
                        String detalleIdStr = key.substring(startIdx + 1, endIdx);
                        Long detalleId = Long.parseLong(detalleIdStr);
                        String cantidadStr = items.get("items[" + detalleIdStr + "].cantidad");
                        int cantidad = Integer.parseInt(cantidadStr);
                        
                        // Validar que al menos un ítem fue seleccionado con cantidad > 0
                        if (cantidad > 0) {
                            // Buscar el detalle en la venta
                            if (venta.getDetalles() != null) {
                                for (DetalleVenta detalle : venta.getDetalles()) {
                                    if (detalle.getId().equals(detalleId)) {
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
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            if (itemsSeleccionados.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Debe seleccionar al menos un ítem para devolver");
                return "redirect:/ventas/nota-credito/" + id;
            }
            
            // Obtener usuario actual
            Usuario usuarioActual = obtenerUsuarioActual(request);
            
            // Obtener serie y correlativo para la nota de crédito
            String serie = empresa.getSerieBoleta(); // Por defecto usar serie de boleta
            if ("01".equals(venta.getTipoComprobante())) {
                serie = empresa.getSerieFactura();
            }
            
            // Obtener siguiente correlativo
            Integer correlativo;
            if ("01".equals(venta.getTipoComprobante())) {
                correlativo = empresa.getCorrelativoNotaCreditoFactura();
            } else {
                correlativo = empresa.getCorrelativoNotaCreditoBoleta();
            }
            
            if (correlativo == null) {
                correlativo = 1;
            }
            
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
            notaCredito.setTotalAcreditado(totalAcreditado);
            notaCredito.setEstadoSunat("pendiente");
            notaCredito.setEmitidaPorUsuario(usuarioActual);
            notaCredito.setFechaEmision(LocalDateTime.now());
            
            // Guardar nota de crédito en BD
            notaCredito = notasCreditoRepository.save(notaCredito);
            logger.info("Nota de crédito guardada en BD con ID: {}", notaCredito.getId());
            
            // Emitir en APISUNAT
            logger.info("Enviando a APISUNAT...");
            ApisunatService.ApisunatResult resultado = apisunatService.emitirNotaCredito(
                venta, serie, correlativo, tipoNota, motivo, itemsSeleccionados, empresa
            );
            
            logger.info("Resultado APISUNAT: status={}, pdfUrl={}, xmlUrl={}, documentId={}", 
                resultado.getStatus(), resultado.getPdfUrl(), resultado.getXmlUrl(), resultado.getDocumentId());
            
            // Actualizar nota de crédito con respuesta de APISUNAT
            notaCredito.setEstadoSunat(resultado.getStatus());
            notaCredito.setNubefactId(resultado.getDocumentId());
            notaCredito.setPdfUrl(resultado.getPdfUrl());
            notaCredito.setXmlUrl(resultado.getXmlUrl());
            notaCredito.setHashCdr(resultado.getHashCdr());
            notaCredito.setRawResponse(resultado.getRawResponse());
            
            // Actualizar correlativo en empresa
            if ("01".equals(venta.getTipoComprobante())) {
                empresa.setCorrelativoNotaCreditoFactura(correlativo + 1);
            } else {
                empresa.setCorrelativoNotaCreditoBoleta(correlativo + 1);
            }
            empresaRepository.save(empresa);
            
            // Actualizar estado de la venta original SOLO para NC (no recalcular ni limpiar detalles)
            String estadoNCVenta = esAnulacionTotal ? "TOTAL" : "PARCIAL";
            ventaService.aplicarNotaCredito(id, estadoNCVenta, itemsSeleccionados);
            
            // Guardar cambios finales en nota de crédito
            notasCreditoRepository.save(notaCredito);
            
            logger.info("Nota de crédito procesada exitosamente. Estado SUNAT: {}", resultado.getStatus());
            
            // Redirigir a vista de comprobante
            model.addAttribute("venta", ventaService.obtenerVentaDetalladaPorId(id).orElse(null));
            model.addAttribute("notaCredito", notaCredito);
            model.addAttribute("tipoNota", tipoNota);
            model.addAttribute("motivo", motivo);
            model.addAttribute("itemsSeleccionados", itemsSeleccionados);
            model.addAttribute("totalAcreditado", totalAcreditado);
            model.addAttribute("resultadoSunat", resultado);
            
            if ("ACEPTADO".equals(resultado.getStatus())) {
                model.addAttribute("mensaje", "Nota de Crédito emitida y aceptada por SUNAT exitosamente");
            } else if ("RECHAZADO".equals(resultado.getStatus())) {
                model.addAttribute("mensaje", "Nota de Crédito emitida pero RECHAZADA por SUNAT");
                model.addAttribute("errorDetalle", resultado.getErrorCode());
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
        // Si no hay usuario en sesión, retornar null (se manejará en la lógica de negocio)
        return null;
    }
}
