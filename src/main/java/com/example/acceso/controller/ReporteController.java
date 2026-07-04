package com.example.acceso.controller;

import com.example.acceso.service.ReporteExportService;
import com.example.acceso.service.ReporteService;
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
@RequestMapping("/reportes")
public class ReporteController {

    private final ReporteService reporteService;
    private final ReporteExportService reporteExportService;

    public ReporteController(ReporteService reporteService,
                             ReporteExportService reporteExportService) {
        this.reporteService = reporteService;
        this.reporteExportService = reporteExportService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("tipoActivo", "ventas");
        LocalDate hoy = LocalDate.now();
        model.addAttribute("reporte",
            reporteService.generarReporteVentas(
                hoy.withDayOfMonth(1), hoy));
        model.addAttribute("desde", hoy.withDayOfMonth(1));
        model.addAttribute("hasta", hoy);
        return "reportes/index";
    }

    @GetMapping("/ventas")
    public String reporteVentas(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model) {
        if (desde == null) desde = LocalDate.now().withDayOfMonth(1);
        if (hasta == null) hasta = LocalDate.now();
        model.addAttribute("reporte",
            reporteService.generarReporteVentas(desde, hasta));
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("tipoActivo", "ventas");
        return "reportes/index";
    }

    @GetMapping("/inventario")
    public String reporteInventario(Model model) {
        model.addAttribute("reporte",
            reporteService.generarReporteInventario());
        model.addAttribute("tipoActivo", "inventario");
        return "reportes/index";
    }

    @GetMapping("/comprobantes")
    public String reporteComprobantes(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model) {
        if (desde == null) desde = LocalDate.now().withDayOfMonth(1);
        if (hasta == null) hasta = LocalDate.now();
        model.addAttribute("reporte",
            reporteService.generarReporteComprobantes(desde, hasta));
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("tipoActivo", "comprobantes");
        return "reportes/index";
    }

    @GetMapping("/caja")
    public String reporteCaja(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model) {
        if (desde == null) desde = LocalDate.now().minusDays(30);
        if (hasta == null) hasta = LocalDate.now();
        model.addAttribute("reporte",
            reporteService.generarReporteCaja(desde, hasta));
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("tipoActivo", "caja");
        return "reportes/index";
    }

    // ── PDF ──────────────────────────────────────────────────────

    @GetMapping("/ventas/pdf")
    public ResponseEntity<InputStreamResource> exportarVentasPdf(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        if (desde == null) desde = LocalDate.now().withDayOfMonth(1);
        if (hasta == null) hasta = LocalDate.now();
        return pdfResponse(reporteExportService.exportarVentasPdf(desde, hasta),
            "reporte-ventas.pdf");
    }

    @GetMapping("/inventario/pdf")
    public ResponseEntity<InputStreamResource> exportarInventarioPdf() {
        return pdfResponse(reporteExportService.exportarInventarioPdf(),
            "reporte-inventario.pdf");
    }

    @GetMapping("/comprobantes/pdf")
    public ResponseEntity<InputStreamResource> exportarComprobantesPdf(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        if (desde == null) desde = LocalDate.now().withDayOfMonth(1);
        if (hasta == null) hasta = LocalDate.now();
        return pdfResponse(reporteExportService.exportarComprobantesPdf(desde, hasta),
            "reporte-comprobantes.pdf");
    }

    @GetMapping("/caja/pdf")
    public ResponseEntity<InputStreamResource> exportarCajaPdf(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        if (desde == null) desde = LocalDate.now().minusDays(30);
        if (hasta == null) hasta = LocalDate.now();
        return pdfResponse(reporteExportService.exportarCajaPdf(desde, hasta),
            "reporte-caja.pdf");
    }

    // ── EXCEL ────────────────────────────────────────────────────

    @GetMapping("/ventas/excel")
    public ResponseEntity<InputStreamResource> exportarVentasExcel(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        if (desde == null) desde = LocalDate.now().withDayOfMonth(1);
        if (hasta == null) hasta = LocalDate.now();
        return excelResponse(reporteExportService.exportarVentasExcel(desde, hasta),
            "reporte-ventas.xlsx");
    }

    @GetMapping("/inventario/excel")
    public ResponseEntity<InputStreamResource> exportarInventarioExcel() {
        return excelResponse(reporteExportService.exportarInventarioExcel(),
            "reporte-inventario.xlsx");
    }

    @GetMapping("/comprobantes/excel")
    public ResponseEntity<InputStreamResource> exportarComprobantesExcel(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        if (desde == null) desde = LocalDate.now().withDayOfMonth(1);
        if (hasta == null) hasta = LocalDate.now();
        return excelResponse(reporteExportService.exportarComprobantesExcel(desde, hasta),
            "reporte-comprobantes.xlsx");
    }

    @GetMapping("/caja/excel")
    public ResponseEntity<InputStreamResource> exportarCajaExcel(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        if (desde == null) desde = LocalDate.now().minusDays(30);
        if (hasta == null) hasta = LocalDate.now();
        return excelResponse(reporteExportService.exportarCajaExcel(desde, hasta),
            "reporte-caja.xlsx");
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