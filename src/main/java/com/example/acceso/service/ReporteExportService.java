package com.example.acceso.service;

import com.example.acceso.dto.ReporteCajaDTO;
import com.example.acceso.dto.ReporteComprobantesDTO;
import com.example.acceso.dto.ReporteEgresosDTO;
import com.example.acceso.dto.ReporteInventarioDTO;
import com.example.acceso.dto.ReporteIngresosDTO;
import com.example.acceso.dto.ReporteVentasDTO;
import com.example.acceso.model.Empresa;
import com.example.acceso.model.Producto;
import com.example.acceso.model.SesionCaja;
import com.example.acceso.model.Venta;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class ReporteExportService {

    private final EmpresaService empresaService;
    private final ReporteService reporteService;
    private final ReporteCompactoService reporteCompactoService;

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private static final Font BODY_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font KPI_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
    private static final Font KPI_LABEL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9);

    public ReporteExportService(EmpresaService empresaService, ReporteService reporteService,
                               ReporteCompactoService reporteCompactoService) {
        this.empresaService = empresaService;
        this.reporteService = reporteService;
        this.reporteCompactoService = reporteCompactoService;
    }

    // ── PDF ──────────────────────────────────────────────────────

    public ByteArrayInputStream exportarVentasPdf(LocalDate desde, LocalDate hasta) {
        ReporteVentasDTO r = reporteService.generarReporteVentas(desde, hasta);
        return generarPdf("Reporte de Ventas", desde, hasta, document -> {
            addKpiRowPdf(document, new String[][]{
                {"Total Vendido", "S/ " + format(r.getTotalVendido())},
                {"Transacciones", String.valueOf(r.getCantTransacciones())},
                {"Ticket Promedio", "S/ " + format(r.getTicketPromedio())},
                {"Notas de Crédito", "S/ " + format(r.getTotalNotasCredito())}
            });
            addTablePdf(document, new String[]{"Método de Pago", "Monto"},
                r.getPorMetodoPago().entrySet().stream()
                    .map(e -> new String[]{e.getKey(), "S/ " + format(e.getValue())}).toList());
            addTablePdf(document, new String[]{"Producto", "Cantidad"},
                r.getTopProductos().entrySet().stream()
                    .map(e -> new String[]{e.getKey(), String.valueOf(e.getValue())}).toList());
        });
    }

    public ByteArrayInputStream exportarInventarioPdf() {
        ReporteInventarioDTO r = reporteService.generarReporteInventario();
        return generarPdf("Reporte de Inventario", null, null, document -> {
            addKpiRowPdf(document, new String[][]{
                {"Total Productos", String.valueOf(r.getTotalProductos())},
                {"Valor Inventario", "S/ " + format(r.getValorTotalInventario())},
                {"Stock Bajo", String.valueOf(r.getProductosBajoMinimo().size())},
                {"Sin Movimiento", String.valueOf(r.getProductoSinMovimiento().size())}
            });
            if (!r.getProductosBajoMinimo().isEmpty()) {
                addTablePdf(document, new String[]{"Producto", "Stock", "Stock Mínimo"},
                    r.getProductosBajoMinimo().stream()
                        .map(p -> new String[]{p.getNombre(), String.valueOf(p.getStock()), String.valueOf(p.getStockMinimo())}).toList());
            }
            if (!r.getProductoSinMovimiento().isEmpty()) {
                addTablePdf(document, new String[]{"Productos sin Movimiento (30 días)", "Stock"},
                    r.getProductoSinMovimiento().stream()
                        .map(p -> new String[]{p.getNombre(), String.valueOf(p.getStock())}).toList());
            }
        });
    }

    public ByteArrayInputStream exportarComprobantesPdf(LocalDate desde, LocalDate hasta) {
        ReporteComprobantesDTO r = reporteService.generarReporteComprobantes(desde, hasta);
        return generarPdf("Reporte de Comprobantes SUNAT", desde, hasta, document -> {
            addKpiRowPdf(document, new String[][]{
                {"Boletas", String.valueOf(r.getTotalBoletas()) + " / S/ " + format(r.getMontoBoletas())},
                {"Facturas", String.valueOf(r.getTotalFacturas()) + " / S/ " + format(r.getMontoFacturas())},
                {"Base Imponible", "S/ " + format(r.getBaseImponible())},
                {"IGV Generado", "S/ " + format(r.getIgvGenerado())}
            });
            if (r.getVentas() != null && !r.getVentas().isEmpty()) {
                addTablePdf(document, new String[]{"N° Venta", "Cliente", "Comprobante", "Total"},
                    r.getVentas().stream()
                        .map(v -> new String[]{
                            v.getNumeroVenta(),
                            v.getCliente() != null ? v.getCliente().getNombre() : "-",
                            v.getTipoComprobante(),
                            "S/ " + format(v.getTotal())
                        }).toList());
            }
        });
    }

    public ByteArrayInputStream exportarCajaPdf(LocalDate desde, LocalDate hasta) {
        ReporteCajaDTO r = reporteService.generarReporteCaja(desde, hasta);
        return generarPdf("Reporte de Caja", desde, hasta, document -> {
            addKpiRowPdf(document, new String[][]{
                {"Total Sesiones", String.valueOf(r.getTotalSesiones())},
                {"Cerradas", String.valueOf(r.getSesionesCerradas())},
                {"Con Diferencia", String.valueOf(r.getSesionesConDiferencia())},
                {"Total Diferencias", "S/ " + format(r.getTotalDiferencias())}
            });
            if (r.getSesiones() != null && !r.getSesiones().isEmpty()) {
                addTablePdf(document, new String[]{"ID", "Apertura", "Cierre", "Estado", "Diferencia"},
                    r.getSesiones().stream()
                        .map(s -> new String[]{
                            String.valueOf(s.getId()),
                            s.getFechaApertura() != null ? s.getFechaApertura().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "-",
                            s.getFechaCierre() != null ? s.getFechaCierre().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "-",
                            s.getEstado(),
                            s.getDiferencia() != null ? "S/ " + format(s.getDiferencia()) : "-"
                        }).toList());
            }
        });
    }

    // ── EXCEL ─────────────────────────────────────────────────────

    public ByteArrayInputStream exportarVentasExcel(LocalDate desde, LocalDate hasta) {
        ReporteVentasDTO r = reporteService.generarReporteVentas(desde, hasta);
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // KPIs
            Sheet kpiSheet = wb.createSheet("KPIs");
            fillKpiSheet(wb, kpiSheet, new String[][]{
                {"Total Vendido", "S/ " + format(r.getTotalVendido())},
                {"Transacciones", String.valueOf(r.getCantTransacciones())},
                {"Ticket Promedio", "S/ " + format(r.getTicketPromedio())},
                {"Notas de Crédito", "S/ " + format(r.getTotalNotasCredito())}
            });
            // Ventas por método de pago
            Sheet metodoSheet = wb.createSheet("Por Método de Pago");
            fillDataSheet(wb, metodoSheet, new String[]{"Método de Pago", "Monto"},
                r.getPorMetodoPago().entrySet().stream()
                    .map(e -> new Object[]{e.getKey(), e.getValue()}).toList());
            // Top productos
            Sheet topSheet = wb.createSheet("Top Productos");
            fillDataSheet(wb, topSheet, new String[]{"Producto", "Cantidad Vendida"},
                r.getTopProductos().entrySet().stream()
                    .map(e -> new Object[]{e.getKey(), e.getValue()}).toList());
            wb.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Error al generar Excel de ventas", e);
        }
    }

    public ByteArrayInputStream exportarInventarioExcel() {
        ReporteInventarioDTO r = reporteService.generarReporteInventario();
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet kpiSheet = wb.createSheet("KPIs");
            fillKpiSheet(wb, kpiSheet, new String[][]{
                {"Total Productos", String.valueOf(r.getTotalProductos())},
                {"Valor Inventario", "S/ " + format(r.getValorTotalInventario())},
                {"Stock Bajo", String.valueOf(r.getProductosBajoMinimo().size())},
                {"Sin Movimiento", String.valueOf(r.getProductoSinMovimiento().size())}
            });
            if (!r.getProductosBajoMinimo().isEmpty()) {
                Sheet bajoSheet = wb.createSheet("Stock Bajo");
                fillDataSheet(wb, bajoSheet, new String[]{"Producto", "Stock", "Stock Mínimo"},
                    r.getProductosBajoMinimo().stream()
                        .map(p -> new Object[]{p.getNombre(), p.getStock(), p.getStockMinimo()}).toList());
            }
            if (!r.getProductoSinMovimiento().isEmpty()) {
                Sheet sinMovSheet = wb.createSheet("Sin Movimiento");
                fillDataSheet(wb, sinMovSheet, new String[]{"Producto", "Stock"},
                    r.getProductoSinMovimiento().stream()
                        .map(p -> new Object[]{p.getNombre(), p.getStock()}).toList());
            }
            wb.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Error al generar Excel de inventario", e);
        }
    }

    public ByteArrayInputStream exportarComprobantesExcel(LocalDate desde, LocalDate hasta) {
        ReporteComprobantesDTO r = reporteService.generarReporteComprobantes(desde, hasta);
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet kpiSheet = wb.createSheet("KPIs");
            fillKpiSheet(wb, kpiSheet, new String[][]{
                {"Boletas", String.valueOf(r.getTotalBoletas()) + " / S/ " + format(r.getMontoBoletas())},
                {"Facturas", String.valueOf(r.getTotalFacturas()) + " / S/ " + format(r.getMontoFacturas())},
                {"Base Imponible", "S/ " + format(r.getBaseImponible())},
                {"IGV Generado", "S/ " + format(r.getIgvGenerado())}
            });
            if (r.getVentas() != null && !r.getVentas().isEmpty()) {
                Sheet ventasSheet = wb.createSheet("Ventas");
                fillDataSheet(wb, ventasSheet, new String[]{"N° Venta", "Cliente", "Comprobante", "Total"},
                    r.getVentas().stream()
                        .map(v -> new Object[]{
                            v.getNumeroVenta(),
                            v.getCliente() != null ? v.getCliente().getNombre() : "-",
                            v.getTipoComprobante(),
                            v.getTotal()
                        }).toList());
            }
            wb.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Error al generar Excel de comprobantes", e);
        }
    }

    public ByteArrayInputStream exportarCajaExcel(LocalDate desde, LocalDate hasta) {
        ReporteCajaDTO r = reporteService.generarReporteCaja(desde, hasta);
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet kpiSheet = wb.createSheet("KPIs");
            fillKpiSheet(wb, kpiSheet, new String[][]{
                {"Total Sesiones", String.valueOf(r.getTotalSesiones())},
                {"Cerradas", String.valueOf(r.getSesionesCerradas())},
                {"Con Diferencia", String.valueOf(r.getSesionesConDiferencia())},
                {"Total Diferencias", "S/ " + format(r.getTotalDiferencias())}
            });
            if (r.getSesiones() != null && !r.getSesiones().isEmpty()) {
                Sheet sesionesSheet = wb.createSheet("Sesiones");
                fillDataSheet(wb, sesionesSheet, new String[]{"ID", "Apertura", "Cierre", "Estado", "Diferencia"},
                    r.getSesiones().stream()
                        .map(s -> new Object[]{
                            s.getId(),
                            s.getFechaApertura() != null ? s.getFechaApertura().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "-",
                            s.getFechaCierre() != null ? s.getFechaCierre().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "-",
                            s.getEstado(),
                            s.getDiferencia()
                        }).toList());
            }
            wb.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Error al generar Excel de caja", e);
        }
    }

    public ByteArrayInputStream exportarIngresosPdf(LocalDate desde, LocalDate hasta) {
        ReporteIngresosDTO r = reporteService.generarReporteIngresos(desde, hasta);
        return generarPdf("Reporte de Ingresos", desde, hasta, document -> {
            addKpiRowPdf(document, new String[][]{
                {"Total de Registros", String.valueOf(r.getTotalRegistros())},
                {"Total Recaudado", "S/ " + format(r.getTotalRecaudado())},
                {"Total Comisiones Online", "S/ " + format(r.getTotalComisionesOnline())}
            });
            if (r.getIngresos() != null && !r.getIngresos().isEmpty()) {
                addTablePdf(document, new String[]{"Cliente", "Comprobante", "Fecha", "Concepto", "Método", "Total", "Cajero"},
                    r.getIngresos().stream()
                        .map(i -> new String[]{
                            (String) i.getOrDefault("cliente", "-"),
                            (String) i.getOrDefault("numeroComprobante", "-"),
                            i.get("fecha") != null ? ((LocalDateTime) i.get("fecha")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-",
                            (String) i.getOrDefault("concepto", "-"),
                            (String) i.getOrDefault("metodoPago", "-"),
                            "S/ " + format((BigDecimal) i.get("total")),
                            (String) i.getOrDefault("cajero", "-")
                        }).toList());
            }
        });
    }

    public ByteArrayInputStream exportarIngresosExcel(LocalDate desde, LocalDate hasta) {
        ReporteIngresosDTO r = reporteService.generarReporteIngresos(desde, hasta);
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet kpiSheet = wb.createSheet("KPIs");
            fillKpiSheet(wb, kpiSheet, new String[][]{
                {"Total de Registros", String.valueOf(r.getTotalRegistros())},
                {"Total Recaudado", "S/ " + format(r.getTotalRecaudado())},
                {"Total Comisiones Online", "S/ " + format(r.getTotalComisionesOnline())}
            });
            if (r.getIngresos() != null && !r.getIngresos().isEmpty()) {
                Sheet ingresosSheet = wb.createSheet("Detalle de Ingresos");
                fillDataSheet(wb, ingresosSheet, new String[]{"Cliente", "Comprobante", "Fecha", "Período", "Concepto", "Método Pago", "Subtotal", "Total", "Cajero"},
                    r.getIngresos().stream()
                        .map(i -> new Object[]{
                            i.getOrDefault("cliente", "-"),
                            i.getOrDefault("numeroComprobante", "-"),
                            i.get("fecha") != null ? ((LocalDateTime) i.get("fecha")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "-",
                            i.getOrDefault("periodo", "-"),
                            i.getOrDefault("concepto", "-"),
                            i.getOrDefault("metodoPago", "-"),
                            i.get("subtotal"),
                            i.get("total"),
                            i.getOrDefault("cajero", "-")
                        }).toList());
            }
            wb.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Error al generar Excel de ingresos", e);
        }
    }

    public ByteArrayInputStream exportarEgresosPdf(LocalDate desde, LocalDate hasta, String tipoEgreso) {
        ReporteEgresosDTO r = reporteService.generarReporteEgresos(desde, hasta, tipoEgreso);
        return generarPdf("Reporte de Egresos", desde, hasta, document -> {
            addKpiRowPdf(document, new String[][]{
                {"Total de Registros", String.valueOf(r.getTotalRegistros())},
                {"Monto Total Egresado", "S/ " + format(r.getMontoTotalEgresado())},
                {"Cantidad de Tipos", String.valueOf(r.getCantidadTiposEgreso())}
            });
            if (r.getEgresosPorTipo() != null && !r.getEgresosPorTipo().isEmpty()) {
                addTablePdf(document, new String[]{"Tipo de Egreso", "Monto Total"},
                    r.getEgresosPorTipo().entrySet().stream()
                        .map(e -> new String[]{e.getKey(), "S/ " + format(e.getValue())}).toList());
            }
            if (r.getEgresos() != null && !r.getEgresos().isEmpty()) {
                addTablePdf(document, new String[]{"Fecha", "Tipo", "Comentario", "Registrado Por", "Monto"},
                    r.getEgresos().stream()
                        .map(e -> new String[]{
                            e.get("fecha") != null ? ((LocalDateTime) e.get("fecha")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-",
                            (String) e.getOrDefault("tipoEgreso", "-"),
                            (String) e.getOrDefault("comentario", "-"),
                            (String) e.getOrDefault("registradoPor", "-"),
                            "S/ " + format((BigDecimal) e.get("monto"))
                        }).toList());
            }
        });
    }

    public ByteArrayInputStream exportarEgresosExcel(LocalDate desde, LocalDate hasta, String tipoEgreso) {
        ReporteEgresosDTO r = reporteService.generarReporteEgresos(desde, hasta, tipoEgreso);
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet kpiSheet = wb.createSheet("KPIs");
            fillKpiSheet(wb, kpiSheet, new String[][]{
                {"Total de Registros", String.valueOf(r.getTotalRegistros())},
                {"Monto Total Egresado", "S/ " + format(r.getMontoTotalEgresado())},
                {"Cantidad de Tipos", String.valueOf(r.getCantidadTiposEgreso())}
            });
            if (r.getEgresosPorTipo() != null && !r.getEgresosPorTipo().isEmpty()) {
                Sheet tiposSheet = wb.createSheet("Por Tipo");
                fillDataSheet(wb, tiposSheet, new String[]{"Tipo de Egreso", "Monto Total"},
                    r.getEgresosPorTipo().entrySet().stream()
                        .map(e -> new Object[]{e.getKey(), e.getValue()}).toList());
            }
            if (r.getEgresos() != null && !r.getEgresos().isEmpty()) {
                Sheet egresosSheet = wb.createSheet("Detalle");
                fillDataSheet(wb, egresosSheet, new String[]{"Fecha", "Tipo", "Comentario", "Registrado Por", "N° Operación", "Monto"},
                    r.getEgresos().stream()
                        .map(e -> new Object[]{
                            e.get("fecha") != null ? ((LocalDateTime) e.get("fecha")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "-",
                            e.getOrDefault("tipoEgreso", "-"),
                            e.getOrDefault("comentario", "-"),
                            e.getOrDefault("registradoPor", "-"),
                            e.getOrDefault("numeroOperacion", "-"),
                            e.get("monto")
                        }).toList());
            }
            wb.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Error al generar Excel de egresos", e);
        }
    }

    // ── REPORTE COMPACTO ─────────────────────────────────────────

    public ByteArrayInputStream exportarCompactoPdf(LocalDate desde, LocalDate hasta) {
        com.example.acceso.dto.ReporteCompactoDTO r = reporteCompactoService.generarReporteCompacto(desde, hasta);
        
        return generarPdf("Reporte Compacto - Terminalización de Caja", desde, hasta, document -> {
            // SECCIÓN 1: Resumen General
            Paragraph seccion1 = new Paragraph("RESUMEN GENERAL", HEADER_FONT);
            seccion1.setAlignment(Element.ALIGN_LEFT);
            seccion1.setSpacingBefore(10);
            seccion1.setSpacingAfter(10);
            document.add(seccion1);
            
            // KPIs principales
            addKpiRowPdf(document, new String[][]{
                {"Total General", "S/ " + format(r.getTotalGeneral())},
                {"Total Efectivo", "S/ " + format(r.getTotalEfectivo())},
                {"Total Yape", "S/ " + format(r.getTotalYape())},
                {"Total Otros", "S/ " + format(r.getTotalOtros())}
            });
            
            // Tabla de detalle por día
            if (r.getDetallePorDia() != null && !r.getDetallePorDia().isEmpty()) {
                addTablePdf(document, 
                    new String[]{"Fecha", "Efectivo", "Yape", "Otros", "Total", "Ventas"},
                    r.getDetallePorDia().stream()
                        .map(d -> new String[]{
                            d.get("fecha") != null ? ((LocalDate) d.get("fecha")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-",
                            "S/ " + format((BigDecimal) d.getOrDefault("efectivo", BigDecimal.ZERO)),
                            "S/ " + format((BigDecimal) d.getOrDefault("yape", BigDecimal.ZERO)),
                            "S/ " + format((BigDecimal) d.getOrDefault("otros", BigDecimal.ZERO)),
                            "S/ " + format((BigDecimal) d.getOrDefault("total", BigDecimal.ZERO)),
                            String.valueOf(d.getOrDefault("cantidadVentas", 0))
                        }).toList()
                );
            }
            
            // SECCIÓN 2: Detalle Efectivo
            if (r.getVentasEfectivo() != null && !r.getVentasEfectivo().isEmpty()) {
                document.add(Chunk.NEWLINE);
                Paragraph seccion2 = new Paragraph("DETALLE DE VENTAS EN EFECTIVO", HEADER_FONT);
                seccion2.setAlignment(Element.ALIGN_LEFT);
                seccion2.setSpacingBefore(10);
                seccion2.setSpacingAfter(10);
                document.add(seccion2);
                
                addTablePdf(document,
                    new String[]{"Fecha", "N° Venta", "Cliente", "Comprobante", "Subtotal", "Descuento", "Total", "Cajero"},
                    r.getVentasEfectivo().stream()
                        .map(v -> new String[]{
                            v.get("fecha") != null ? ((LocalDateTime) v.get("fecha")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "-",
                            (String) v.getOrDefault("numeroVenta", "-"),
                            (String) v.getOrDefault("cliente", "-"),
                            (String) v.getOrDefault("comprobante", "-"),
                            "S/ " + format((BigDecimal) v.getOrDefault("subtotal", BigDecimal.ZERO)),
                            "S/ " + format((BigDecimal) v.getOrDefault("descuento", BigDecimal.ZERO)),
                            "S/ " + format((BigDecimal) v.getOrDefault("total", BigDecimal.ZERO)),
                            (String) v.getOrDefault("cajero", "-")
                        }).toList()
                );
            }
            
            // SECCIÓN 3: Detalle Yape
            if (r.getVentasYape() != null && !r.getVentasYape().isEmpty()) {
                document.add(Chunk.NEWLINE);
                Paragraph seccion3 = new Paragraph("DETALLE DE VENTAS EN YAPE", HEADER_FONT);
                seccion3.setAlignment(Element.ALIGN_LEFT);
                seccion3.setSpacingBefore(10);
                seccion3.setSpacingAfter(10);
                document.add(seccion3);
                
                addTablePdf(document,
                    new String[]{"Fecha", "N° Venta", "Cliente", "Comprobante", "Subtotal", "Descuento", "Total", "Cajero"},
                    r.getVentasYape().stream()
                        .map(v -> new String[]{
                            v.get("fecha") != null ? ((LocalDateTime) v.get("fecha")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "-",
                            (String) v.getOrDefault("numeroVenta", "-"),
                            (String) v.getOrDefault("cliente", "-"),
                            (String) v.getOrDefault("comprobante", "-"),
                            "S/ " + format((BigDecimal) v.getOrDefault("subtotal", BigDecimal.ZERO)),
                            "S/ " + format((BigDecimal) v.getOrDefault("descuento", BigDecimal.ZERO)),
                            "S/ " + format((BigDecimal) v.getOrDefault("total", BigDecimal.ZERO)),
                            (String) v.getOrDefault("cajero", "-")
                        }).toList()
                );
            }
        });
    }

    public ByteArrayInputStream exportarCompactoExcel(LocalDate desde, LocalDate hasta) {
        com.example.acceso.dto.ReporteCompactoDTO r = reporteCompactoService.generarReporteCompacto(desde, hasta);
        
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Hoja 1: Resumen
            Sheet kpiSheet = wb.createSheet("Resumen");
            fillKpiSheet(wb, kpiSheet, new String[][]{
                {"Total General", "S/ " + format(r.getTotalGeneral())},
                {"Total Efectivo", "S/ " + format(r.getTotalEfectivo())},
                {"Total Yape", "S/ " + format(r.getTotalYape())},
                {"Total Otros", "S/ " + format(r.getTotalOtros())},
                {"Total Días", String.valueOf(r.getTotalDias())},
                {"Total Ventas", String.valueOf(r.getTotalVentas())}
            });
            
            // Hoja 2: Detalle por día
            if (r.getDetallePorDia() != null && !r.getDetallePorDia().isEmpty()) {
                Sheet detalleSheet = wb.createSheet("Flujo por Día");
                fillDataSheet(wb, detalleSheet, 
                    new String[]{"Fecha", "Efectivo", "Yape", "Otros", "Total", "Cantidad Ventas"},
                    r.getDetallePorDia().stream()
                        .map(d -> new Object[]{
                            d.get("fecha"),
                            d.getOrDefault("efectivo", BigDecimal.ZERO),
                            d.getOrDefault("yape", BigDecimal.ZERO),
                            d.getOrDefault("otros", BigDecimal.ZERO),
                            d.getOrDefault("total", BigDecimal.ZERO),
                            d.getOrDefault("cantidadVentas", 0)
                        }).toList()
                );
            }
            
            // Hoja 3: Detalle Efectivo
            if (r.getVentasEfectivo() != null && !r.getVentasEfectivo().isEmpty()) {
                Sheet efectivoSheet = wb.createSheet("Efectivo");
                fillDataSheet(wb, efectivoSheet,
                    new String[]{"Fecha", "N° Venta", "Cliente", "Comprobante", "Subtotal", "Descuento", "Total", "Cajero"},
                    r.getVentasEfectivo().stream()
                        .map(v -> new Object[]{
                            v.get("fecha"),
                            v.get("numeroVenta"),
                            v.get("cliente"),
                            v.get("comprobante"),
                            v.get("subtotal"),
                            v.get("descuento"),
                            v.get("total"),
                            v.get("cajero")
                        }).toList()
                );
            }
            
            // Hoja 4: Detalle Yape
            if (r.getVentasYape() != null && !r.getVentasYape().isEmpty()) {
                Sheet yapeSheet = wb.createSheet("Yape");
                fillDataSheet(wb, yapeSheet,
                    new String[]{"Fecha", "N° Venta", "Cliente", "Comprobante", "Subtotal", "Descuento", "Total", "Cajero"},
                    r.getVentasYape().stream()
                        .map(v -> new Object[]{
                            v.get("fecha"),
                            v.get("numeroVenta"),
                            v.get("cliente"),
                            v.get("comprobante"),
                            v.get("subtotal"),
                            v.get("descuento"),
                            v.get("total"),
                            v.get("cajero")
                        }).toList()
                );
            }
            
            wb.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Error al generar Excel de reporte compacto", e);
        }
    }

    // ── PDF helpers ──────────────────────────────────────────────

    private ByteArrayInputStream generarPdf(String titulo, LocalDate desde, LocalDate hasta,
                                            PdfConsumer consumer) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Logo empresa
            Empresa empresa = empresaService.getEmpresaInfo();
            if (empresa != null && empresa.getLogoUrl() != null && !empresa.getLogoUrl().isEmpty()) {
                try {
                    Image img = Image.getInstance(new URL(empresa.getLogoUrl()));
                    img.scaleToFit(80, 80);
                    img.setAlignment(Element.ALIGN_LEFT);
                    document.add(img);
                } catch (Exception ignored) {}
            }

            // Título
            Paragraph title = new Paragraph(titulo, TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4);
            document.add(title);

            // Empresa + período
            String nombreEmpresa = empresa != null ? empresa.getNombre() : "Empresa";
            String ruc = empresa != null && empresa.getRucEmpresa() != null ? "RUC: " + empresa.getRucEmpresa() : "";
            Paragraph info = new Paragraph(nombreEmpresa + "  |  " + ruc, SUBTITLE_FONT);
            info.setAlignment(Element.ALIGN_CENTER);
            info.setSpacingAfter(2);
            document.add(info);

            if (desde != null && hasta != null) {
                Paragraph periodo = new Paragraph(
                    "Período: " + desde.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    + " al " + hasta.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), SUBTITLE_FONT);
                periodo.setAlignment(Element.ALIGN_CENTER);
                periodo.setSpacingAfter(12);
                document.add(periodo);
            }

            Paragraph generado = new Paragraph("Generado: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), SUBTITLE_FONT);
            generado.setAlignment(Element.ALIGN_RIGHT);
            generado.setSpacingAfter(16);
            document.add(generado);

            // Contenido
            consumer.accept(document);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF: " + titulo, e);
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    private void addKpiRowPdf(Document document, String[][] kpis) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(16);
        for (String[] kpi : kpis) {
            PdfPCell cell = new PdfPCell();
            cell.setPadding(8);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(new Color(245, 245, 245));

            Paragraph label = new Paragraph(kpi[0], KPI_LABEL_FONT);
            label.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(label);

            Paragraph value = new Paragraph(kpi[1], KPI_FONT);
            value.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(value);

            table.addCell(cell);
        }
        document.add(table);
    }

    private void addTablePdf(Document document, String[] headers, List<String[]> rows) throws DocumentException {
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(16);

        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, HEADER_FONT));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(new Color(220, 220, 220));
            cell.setPadding(5);
            table.addCell(cell);
        }
        for (String[] row : rows) {
            for (String val : row) {
                PdfPCell cell = new PdfPCell(new Phrase(val != null ? val : "", BODY_FONT));
                cell.setPadding(4);
                table.addCell(cell);
            }
        }
        document.add(table);
    }

    @FunctionalInterface
    private interface PdfConsumer {
        void accept(Document document) throws DocumentException;
    }

    // ── Excel helpers ────────────────────────────────────────────

    private void fillKpiSheet(XSSFWorkbook wb, Sheet sheet, String[][] kpis) {
        CellStyle headerStyle = createHeaderStyle(wb);
        CellStyle valueStyle = createValueStyle(wb);
        int rowNum = 0;
        for (String[] kpi : kpis) {
            Row row = sheet.createRow(rowNum++);
            Cell c1 = row.createCell(0);
            c1.setCellValue(kpi[0]);
            c1.setCellStyle(headerStyle);
            Cell c2 = row.createCell(1);
            c2.setCellValue(kpi[1]);
            c2.setCellStyle(valueStyle);
        }
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void fillDataSheet(XSSFWorkbook wb, Sheet sheet, String[] headers, List<Object[]> rows) {
        CellStyle headerStyle = createHeaderStyle(wb);
        CellStyle dataStyle = createDataStyle(wb);
        // Header row
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        // Data rows
        int rowNum = 1;
        for (Object[] rowData : rows) {
            Row row = sheet.createRow(rowNum++);
            for (int i = 0; i < rowData.length; i++) {
                Cell cell = row.createCell(i);
                Object val = rowData[i];
                if (val instanceof BigDecimal) {
                    cell.setCellValue(((BigDecimal) val).doubleValue());
                    cell.setCellStyle(createNumberStyle(wb));
                } else if (val instanceof Number) {
                    cell.setCellValue(((Number) val).doubleValue());
                } else {
                    cell.setCellValue(val != null ? val.toString() : "");
                }
                cell.setCellStyle(dataStyle);
            }
        }
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private CellStyle createHeaderStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createValueStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontHeightInPoints((short) 12);
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createDataStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createNumberStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private String format(BigDecimal value) {
        if (value == null) return "0.00";
        return String.format("%.2f", value);
    }
}