package com.example.acceso.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * DTO para el reporte Compacto de terminalización de caja
 * Contiene el flujo de dinero por día separado por método de pago
 */
public class ReporteCompactoDTO {

    private LocalDate desde;
    private LocalDate hasta;
    private int totalDias;
    private int totalVentas;
    
    // Totales generales
    private BigDecimal totalGeneral;
    private BigDecimal totalEfectivo;
    private BigDecimal totalYape;
    private BigDecimal totalOtros;
    
    // Detalle completo por día
    private List<Map<String, Object>> detallePorDia;
    
    // Detalle específico de ventas en efectivo
    private List<Map<String, Object>> ventasEfectivo;
    
    // Detalle específico de ventas en Yape
    private List<Map<String, Object>> ventasYape;

    public ReporteCompactoDTO() {
    }

    public ReporteCompactoDTO(LocalDate desde, LocalDate hasta, int totalDias, int totalVentas,
                              BigDecimal totalGeneral, BigDecimal totalEfectivo,
                              BigDecimal totalYape, BigDecimal totalOtros,
                              List<Map<String, Object>> detallePorDia,
                              List<Map<String, Object>> ventasEfectivo,
                              List<Map<String, Object>> ventasYape) {
        this.desde = desde;
        this.hasta = hasta;
        this.totalDias = totalDias;
        this.totalVentas = totalVentas;
        this.totalGeneral = totalGeneral;
        this.totalEfectivo = totalEfectivo;
        this.totalYape = totalYape;
        this.totalOtros = totalOtros;
        this.detallePorDia = detallePorDia;
        this.ventasEfectivo = ventasEfectivo;
        this.ventasYape = ventasYape;
    }

    // Getters y Setters

    public LocalDate getDesde() {
        return desde;
    }

    public void setDesde(LocalDate desde) {
        this.desde = desde;
    }

    public LocalDate getHasta() {
        return hasta;
    }

    public void setHasta(LocalDate hasta) {
        this.hasta = hasta;
    }

    public int getTotalDias() {
        return totalDias;
    }

    public void setTotalDias(int totalDias) {
        this.totalDias = totalDias;
    }

    public int getTotalVentas() {
        return totalVentas;
    }

    public void setTotalVentas(int totalVentas) {
        this.totalVentas = totalVentas;
    }

    public BigDecimal getTotalGeneral() {
        return totalGeneral;
    }

    public void setTotalGeneral(BigDecimal totalGeneral) {
        this.totalGeneral = totalGeneral;
    }

    public BigDecimal getTotalEfectivo() {
        return totalEfectivo;
    }

    public void setTotalEfectivo(BigDecimal totalEfectivo) {
        this.totalEfectivo = totalEfectivo;
    }

    public BigDecimal getTotalYape() {
        return totalYape;
    }

    public void setTotalYape(BigDecimal totalYape) {
        this.totalYape = totalYape;
    }

    public BigDecimal getTotalOtros() {
        return totalOtros;
    }

    public void setTotalOtros(BigDecimal totalOtros) {
        this.totalOtros = totalOtros;
    }

    public List<Map<String, Object>> getDetallePorDia() {
        return detallePorDia;
    }

    public void setDetallePorDia(List<Map<String, Object>> detallePorDia) {
        this.detallePorDia = detallePorDia;
    }

    public List<Map<String, Object>> getVentasEfectivo() {
        return ventasEfectivo;
    }

    public void setVentasEfectivo(List<Map<String, Object>> ventasEfectivo) {
        this.ventasEfectivo = ventasEfectivo;
    }

    public List<Map<String, Object>> getVentasYape() {
        return ventasYape;
    }

    public void setVentasYape(List<Map<String, Object>> ventasYape) {
        this.ventasYape = ventasYape;
    }
}