package com.example.acceso.controller;

import com.example.acceso.dto.CierreResumenDTO;
import com.example.acceso.dto.EventoAuditoriaDTO;
import com.example.acceso.dto.MovimientoLogDTO;
import com.example.acceso.dto.ReportePeriodoDTO;
import com.example.acceso.dto.ResumenSesionActivaDTO;
import com.example.acceso.model.SesionCaja;
import com.example.acceso.model.Usuario;
import com.example.acceso.service.CajaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/caja")
public class CajaController {

    private final CajaService cajaService;

    @Autowired
    public CajaController(CajaService cajaService) {
        this.cajaService = cajaService;
    }

    private Long getUsuarioId(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        return usuario != null ? usuario.getId() : null;
    }

    @GetMapping
    public String index(Model model, HttpSession session) {
        Optional<SesionCaja> sesionActiva = cajaService.obtenerSesionActiva();
        
        if (sesionActiva.isPresent()) {
            // Hay sesión abierta, mostrar monitor en tiempo real
            ResumenSesionActivaDTO resumen = cajaService.obtenerResumenSesionActiva();
            model.addAttribute("resumen", resumen);
            model.addAttribute("sesionActiva", sesionActiva.get());
            model.addAttribute("hayAlertaCierre", cajaService.debeAlertarCierre(sesionActiva.get()));
            return "caja-monitor";
        } else {
            // No hay sesión abierta, mostrar formulario de apertura con saldo propuesto
            BigDecimal saldoPropuesto = cajaService.obtenerSaldoParaApertura();
            SesionCaja ultimaCerrada = cajaService.obtenerUltimaSesionCerrada().orElse(null);

            model.addAttribute("saldoPropuesto", saldoPropuesto);
            model.addAttribute("ultimaCerrada", ultimaCerrada);
            model.addAttribute("hayAlertaDiaSinCerrar", cajaService.haySesionDelDiaAnteriorSinCerrar());
            return "caja-apertura";
        }
    }

    @PostMapping("/abrir")
    public String abrirCaja(@RequestParam BigDecimal montoInicial,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        try {
            Long usuarioId = getUsuarioId(session);
            if (usuarioId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Debe iniciar sesión para realizar esta acción.");
                return "redirect:/caja";
            }
            
            cajaService.abrirCaja(montoInicial, usuarioId);
            redirectAttributes.addFlashAttribute("successMessage", "Caja abierta correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error al abrir caja: " + e.getMessage());
        }
        return "redirect:/caja";
    }

    @GetMapping("/cerrar")
    public String mostrarCerrarCaja(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        Optional<SesionCaja> sesionActiva = cajaService.obtenerSesionActiva();

        if (sesionActiva.isEmpty()) {
            return "redirect:/caja";
        }

        try {
            CierreResumenDTO resumen = cajaService.obtenerResumenParaCierre();
            model.addAttribute("resumen", resumen);
            model.addAttribute("sesion", sesionActiva.get());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error al cargar el resumen de cierre: " + e.getMessage());
            return "redirect:/caja";
        }

        return "caja-cierre";
    }

    @PostMapping("/cerrar")
    public String procesarArqueo(@RequestParam BigDecimal montoDeclarado,
                                   @RequestParam(required = false) String motivoDiferencia,
                                   @RequestParam(required = false) String observaciones,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        // Validación básica
        if (montoDeclarado == null || montoDeclarado.compareTo(BigDecimal.ZERO) < 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "monto-invalido");
            return "redirect:/caja/cerrar";
        }

        Long usuarioId = getUsuarioId(session);
        if (usuarioId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Debe iniciar sesión para realizar esta acción.");
            return "redirect:/caja";
        }

        // Guardar en sesión HTTP para el paso 3 (claves actuales del flujo)
        session.setAttribute("caja.montoDeclarado", montoDeclarado);
        session.setAttribute("caja.motivoDiferencia", motivoDiferencia);
        session.setAttribute("caja.observaciones", observaciones);

        // Guardar también con claves de compatibilidad con tu Corrección 7
        session.setAttribute("arqueo_monto", montoDeclarado);
        session.setAttribute("arqueo_motivo", motivoDiferencia);
        session.setAttribute("arqueo_obs", observaciones);

        // (opcional) compatibilidad con Corrección 7: usuarioId en sesión
        session.setAttribute("usuarioId", usuarioId);

        return "redirect:/caja/cerrar/revisar";
    }

    @PostMapping("/cerrar/confirmar")
    public String confirmarCierre(@RequestParam BigDecimal montoDeclarado,
                                    @RequestParam(required = false) String motivoDiferencia,
                                    @RequestParam(required = false) String observaciones,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        // Mantener compatibilidad con el endpoint anterior
        return procesarArqueo(montoDeclarado, motivoDiferencia, observaciones, session, redirectAttributes);
    }

    @GetMapping("/cerrar/revisar")
    public String revisarCierre(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            Optional<SesionCaja> sesionActiva = cajaService.obtenerSesionActiva();
            if (sesionActiva.isEmpty()) {
                return "redirect:/caja";
            }

            CierreResumenDTO resumen = cajaService.obtenerResumenParaCierre();
            model.addAttribute("resumen", resumen);
            model.addAttribute("sesion", sesionActiva.get());

            // Mostrar valores declarados guardados (si existen)
            Object montoDeclarado = session.getAttribute("caja.montoDeclarado");
            Object motivoDiferencia = session.getAttribute("caja.motivoDiferencia");
            Object observaciones = session.getAttribute("caja.observaciones");

            if (montoDeclarado == null) {
                return "redirect:/caja/cerrar";
            }

            model.addAttribute("montoDeclaradoSesion", montoDeclarado);
            model.addAttribute("motivoDiferenciaSesion", motivoDiferencia);
            model.addAttribute("observacionesSesion", observaciones);

            return "caja-cierre";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error al revisar el cierre: " + e.getMessage());
            return "redirect:/caja/cerrar";
        }
    }

    @GetMapping("/confirmar-cierre")
    public String mostrarConfirmacionCierre(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            Optional<SesionCaja> sesionActiva = cajaService.obtenerSesionActiva();
            if (sesionActiva.isEmpty()) {
                return "redirect:/caja";
            }

            // Resumen esperado
            CierreResumenDTO resumen = cajaService.obtenerResumenParaCierre();

            Object montoDeclaradoObj = session.getAttribute("caja.montoDeclarado");
            if (montoDeclaradoObj == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "No hay datos de arqueo para confirmar.");
                return "redirect:/caja/cerrar";
            }

            BigDecimal montoDeclarado = (BigDecimal) montoDeclaradoObj;
            String motivoDiferencia = (String) session.getAttribute("caja.motivoDiferencia");
            String observaciones = (String) session.getAttribute("caja.observaciones");

            BigDecimal diferencia = null;
            if (resumen != null && resumen.getEfectivoEsperado() != null) {
                diferencia = montoDeclarado.subtract(resumen.getEfectivoEsperado());
            }

            // Para la vista: nombres/metadata
            Usuario usuarioApertura = sesionActiva.get().getUsuarioApertura();

            Object usuarioIdObj = session.getAttribute("usuarioId");
            String usuarioConfirmacionNombre = usuarioApertura != null ? usuarioApertura.getNombre() : null;
            String usuarioConfirmacionHora = LocalDateTime.now().toString();

            model.addAttribute("resumen", resumen);
            model.addAttribute("sesion", sesionActiva.get());
            model.addAttribute("montoDeclarado", montoDeclarado);
            model.addAttribute("motivoDiferencia", motivoDiferencia);
            model.addAttribute("observaciones", observaciones);
            model.addAttribute("diferencia", diferencia);

            model.addAttribute("usuarioConfirmacionNombre", usuarioConfirmacionNombre);
            model.addAttribute("usuarioConfirmacionHora", usuarioConfirmacionHora);

            return "caja/confirmar-cierre";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error al mostrar confirmación: " + e.getMessage());
            return "redirect:/caja/cerrar";
        }
    }

    @PostMapping("/confirmar-cierre")
    public String confirmarCierreDefinitivo(HttpSession session, RedirectAttributes redirectAttributes) {
        // Alias que reutiliza la lógica existente del cierre definitivo
        return finalizarCierre(session, redirectAttributes);
    }

    @PostMapping("/cerrar/finalizar")
    public String finalizarCierre(HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            Long usuarioId = getUsuarioId(session);
            if (usuarioId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Debe iniciar sesión para realizar esta acción.");
                return "redirect:/caja";
            }

            Object montoDeclaradoObj = session.getAttribute("caja.montoDeclarado");
            if (montoDeclaradoObj == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "No hay información de cierre para finalizar.");
                return "redirect:/caja/cerrar";
            }

            BigDecimal montoDeclarado = (BigDecimal) montoDeclaradoObj;
            String motivoDiferencia = (String) session.getAttribute("caja.motivoDiferencia");
            String observaciones = (String) session.getAttribute("caja.observaciones");

            SesionCaja sesionCerrada = cajaService.cerrarCaja(montoDeclarado, motivoDiferencia, observaciones, usuarioId);

            // Limpiar sesión
            session.removeAttribute("caja.montoDeclarado");
            session.removeAttribute("caja.motivoDiferencia");
            session.removeAttribute("caja.observaciones");

            session.removeAttribute("arqueo_monto");
            session.removeAttribute("arqueo_motivo");
            session.removeAttribute("arqueo_obs");
            session.removeAttribute("usuarioId");

            redirectAttributes.addFlashAttribute("successMessage", "Caja cerrada correctamente.");
            return "redirect:/caja/reporte-cierre?id=" + sesionCerrada.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error al finalizar el cierre: " + e.getMessage());
            return "redirect:/caja/cerrar";
        }
    }

    @PostMapping("/movimiento")
    public String registrarMovimiento(@RequestParam String tipo,
                                      @RequestParam BigDecimal monto,
                                      @RequestParam String motivo,
                                      @RequestParam String categoria,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        try {
            Long usuarioId = getUsuarioId(session);
            if (usuarioId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Debe iniciar sesión para realizar esta acción.");
                return "redirect:/caja";
            }
            
            Optional<SesionCaja> sesionActiva = cajaService.obtenerSesionActiva();
            if (sesionActiva.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "No hay una sesión de caja abierta.");
                return "redirect:/caja";
            }
            
            cajaService.registrarMovimiento(sesionActiva.get().getId(), tipo, monto, motivo, categoria, usuarioId);
            redirectAttributes.addFlashAttribute("successMessage", "Movimiento registrado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error al registrar movimiento: " + e.getMessage());
        }
        return "redirect:/caja";
    }

    @GetMapping("/historial")
    public String historial(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                            @RequestParam(required = false, defaultValue = "TODAS") String filtroArqueo,
                            Model model) {
        
        List<SesionCaja> sesiones = cajaService.obtenerHistorialSesiones(
            desde != null ? desde : LocalDate.now().minusDays(30),
            hasta != null ? hasta : LocalDate.now(),
            filtroArqueo
        );
        
        model.addAttribute("sesiones", sesiones);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("filtroArqueo", filtroArqueo);
        
        return "caja-historial";
    }

    @GetMapping("/historial/{id}")
    public String detalleSesion(@PathVariable Long id, Model model) {
        SesionCaja sesion = cajaService.obtenerSesionPorId(id)
                .orElse(null);
        
        if (sesion == null) {
            return "redirect:/caja/historial";
        }
        
        // Obtener resumen detallado de la sesión
        Map<String, Object> detalle = cajaService.obtenerDetalleSesion(id);
        List<EventoAuditoriaDTO> logAuditoria = cajaService.obtenerLogAuditoriaSesion(id);
        
        model.addAttribute("sesion", sesion);
        model.addAttribute("detalle", detalle);
        model.addAttribute("logAuditoria", logAuditoria);
        
        return "caja-detalle-sesion";
    }

    @GetMapping("/reporte-cierre")
    public String reporteCierre(@RequestParam Long id, Model model) {
        SesionCaja sesion = cajaService.obtenerSesionPorId(id)
                .orElse(null);

        if (sesion == null) {
            return "redirect:/caja/historial";
        }

        Map<String, Object> detalle = cajaService.obtenerDetalleSesion(id);
        model.addAttribute("sesion", sesion);
        model.addAttribute("detalle", detalle);

        return "caja/reporte-cierre";
    }

    @GetMapping("/historial/{id}/pdf")
    public String reporteSesionPdf(@PathVariable Long id, Model model) {
        SesionCaja sesion = cajaService.obtenerSesionPorId(id)
                .orElse(null);
        
        if (sesion == null) {
            return "redirect:/caja/historial";
        }
        
        // Obtener resumen detallado de la sesión
        Map<String, Object> detalle = cajaService.obtenerDetalleSesion(id);
        List<EventoAuditoriaDTO> logAuditoria = cajaService.obtenerLogAuditoriaSesion(id);
        
        model.addAttribute("sesion", sesion);
        model.addAttribute("detalle", detalle);
        model.addAttribute("logAuditoria", logAuditoria);
        model.addAttribute("usuarioGenerador", "Usuario"); // Se puede obtener de sesión
        
        return "caja-sesion-pdf";
    }

    @GetMapping("/api/resumen")
    @ResponseBody
    public ResponseEntity<?> apiResumen() {
        ResumenSesionActivaDTO resumen = cajaService.obtenerResumenSesionActiva();
        if (resumen == null) {
            return ResponseEntity.ok(Map.of(
                "sesionAbierta", false
            ));
        }
        return ResponseEntity.ok(Map.of(
            "sesionAbierta", true,
            "resumen", resumen
        ));
    }

    @GetMapping("/api/log-auditoria")
    @ResponseBody
    public ResponseEntity<?> apiLogAuditoria() {
        List<EventoAuditoriaDTO> log = cajaService.obtenerLogAuditoriaSesionActiva();
        return ResponseEntity.ok(log);
    }

    @GetMapping("/api/sesion-actual/log")
    @ResponseBody
    public ResponseEntity<?> apiSesionActualLog() {
        Optional<SesionCaja> sesionActiva = cajaService.obtenerSesionActiva();
        if (sesionActiva.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        List<MovimientoLogDTO> log = cajaService.construirLogSesion(sesionActiva.get().getId());
        return ResponseEntity.ok(log);
    }

    @GetMapping("/sesion-actual/pdf")
    public String reporteSesionActualPdf(Model model) {
        Optional<SesionCaja> sesionActiva = cajaService.obtenerSesionActiva();
        if (sesionActiva.isEmpty()) {
            return "redirect:/caja";
        }
        
        Map<String, Object> detalle = cajaService.obtenerDetalleSesion(sesionActiva.get().getId());
        List<EventoAuditoriaDTO> logAuditoria = cajaService.obtenerLogAuditoriaSesionActiva();
        
        model.addAttribute("sesion", sesionActiva.get());
        model.addAttribute("detalle", detalle);
        model.addAttribute("logAuditoria", logAuditoria);
        
        return "caja-sesion-actual-pdf";
    }

    @GetMapping("/reporte")
    public String reporte(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                          Model model) {
        
        ReportePeriodoDTO reporte = null;
        
        if (desde != null && hasta != null) {
            reporte = cajaService.obtenerReportePeriodo(desde, hasta);
        }
        
        model.addAttribute("reporte", reporte);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        
        return "caja-reporte";
    }

    @GetMapping("/reporte/pdf")
    public String reportePdf(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                             Model model) {
        ReportePeriodoDTO reporte = cajaService.obtenerReportePeriodo(desde, hasta);
        model.addAttribute("reporte", reporte);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        return "caja-reporte-pdf";
    }

    @GetMapping("/reporte/excel")
    public String reporteExcel(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                               Model model) {
        ReportePeriodoDTO reporte = cajaService.obtenerReportePeriodo(desde, hasta);
        model.addAttribute("reporte", reporte);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        return "caja-reporte-excel";
    }
}