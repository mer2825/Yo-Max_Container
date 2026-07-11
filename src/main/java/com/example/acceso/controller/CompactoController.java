package com.example.acceso.controller;

import com.example.acceso.dto.ReporteCompactoDTO;
import com.example.acceso.service.ReporteCompactoService;
import com.example.acceso.service.ReporteExportService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/compacto")
public class CompactoController {

    private final ReporteCompactoService reporteCompactoService;
    private final ReporteExportService reporteExportService;

    public CompactoController(ReporteCompactoService reporteCompactoService,
                              ReporteExportService reporteExportService) {
        this.reporteCompactoService = reporteCompactoService;
        this.reporteExportService = reporteExportService;
    }

    /**
     * Página principal del reporte Compacto
     */
    @GetMapping
    public String index(Model model) {
        LocalDate hoy = LocalDate.now();
        LocalDate primerDia = hoy.withDayOfMonth(1);
        
        ReporteCompactoDTO reporte = reporteCompactoService.generarReporteCompacto(primerDia, hoy);
        model.addAttribute("reporte", reporte);
        model.addAttribute("desde", primerDia);
        model.addAttribute("hasta", hoy);
        
        return "reportes/compacto";
    }

    /**
     * Reporte Compacto con rango de fechas personalizado
     */
    @GetMapping("/reporte")
    public String reporteCompacto(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model) {
        
        if (desde == null) desde = LocalDate.now().withDayOfMonth(1);
        if (hasta == null) hasta = LocalDate.now();
        
        ReporteCompactoDTO reporte = reporteCompactoService.generarReporteCompacto(desde, hasta);
        model.addAttribute("reporte", reporte);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        
        return "reportes/compacto";
    }

    // ── EXPORTACIONES ────────────────────────────────────────────

    @GetMapping("/pdf")
    public ResponseEntity<InputStreamResource> exportarPdf(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        
        if (desde == null) desde = LocalDate.now().withDayOfMonth(1);
        if (hasta == null) hasta = LocalDate.now();
        
        return pdfResponse(reporteExportService.exportarCompactoPdf(desde, hasta),
            "reporte-compacto.pdf");
    }

    @GetMapping("/excel")
    public ResponseEntity<InputStreamResource> exportarExcel(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        
        if (desde == null) desde = LocalDate.now().withDayOfMonth(1);
        if (hasta == null) hasta = LocalDate.now();
        
        return excelResponse(reporteExportService.exportarCompactoExcel(desde, hasta),
            "reporte-compacto.xlsx");
    }

    // ── Helpers ──────────────────────────────────────────────────

    private ResponseEntity<InputStreamResource> pdfResponse(
            java.io.ByteArrayInputStream stream, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
        return ResponseEntity.ok()
            .headers(headers)
            .contentType(MediaType.APPLICATION_PDF)
            .body(new InputStreamResource(stream));
    }

    private ResponseEntity<InputStreamResource> excelResponse(
            java.io.ByteArrayInputStream stream, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
        return ResponseEntity.ok()
            .headers(headers)
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(new InputStreamResource(stream));
    }
}