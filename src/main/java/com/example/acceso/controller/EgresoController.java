package com.example.acceso.controller;

import com.example.acceso.model.MovimientoCaja;
import com.example.acceso.service.EgresoServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/egresos")
public class EgresoController {

    private static final Logger logger = LoggerFactory.getLogger(EgresoController.class);

    private final EgresoServiceImpl egresoService;

    @Autowired
    public EgresoController(EgresoServiceImpl egresoService) {
        this.egresoService = egresoService;
    }

    @GetMapping("/listar")
    public String listarEgresos(Model model) {
        LocalDate hoy = LocalDate.now();
        LocalDate primerDia = hoy.withDayOfMonth(1);
        List<MovimientoCaja> egresos = egresoService.listarEgresosDesdeMovimientosCaja(primerDia, hoy);
        model.addAttribute("egresos", egresos);
        model.addAttribute("tiposEgreso", egresoService.obtenerTiposEgreso());
        return "egresos";
    }

    @GetMapping("/api/listar")
    @ResponseBody
    public ResponseEntity<?> listarEgresosApi(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        try {
            LocalDate fechaDesde = desde != null ? desde : LocalDate.now().withDayOfMonth(1);
            LocalDate fechaHasta = hasta != null ? hasta : LocalDate.now();

            List<MovimientoCaja> egresos = egresoService.listarEgresosDesdeMovimientosCaja(fechaDesde, fechaHasta);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", egresos);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error al listar egresos", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al listar egresos: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/api/tipos")
    @ResponseBody
    public ResponseEntity<?> obtenerTiposEgreso() {
        try {
            List<String> tipos = egresoService.obtenerTiposEgreso();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", tipos);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error al obtener tipos de egreso", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al obtener tipos de egreso: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

