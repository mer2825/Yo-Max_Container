package com.example.acceso.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ReportePeriodoDTO {
    private LocalDate desde;
    private LocalDate hasta;
    private BigDecimal totalPeriodo;
    private BigDecimal ticketPromedio;
    private LocalDate mejorDia;
    private BigDecimal mejorDiaTotal;
    private Long totalSesiones;
    private Map<String, BigDecimal> ventasPorMetodoPago;
    private List<ResumenDiaDTO> resumenPorDia;
    private List<ProductoMasVendidoDTO> top5Productos;

    public ReportePeriodoDTO() {
    }

    public ReportePeriodoDTO(LocalDate desde, LocalDate hasta, BigDecimal totalPeriodo,
                             BigDecimal ticketPromedio, LocalDate mejorDia, BigDecimal mejorDiaTotal,
                             Long totalSesiones, Map<String, BigDecimal> ventasPorMetodoPago,
                             List<ResumenDiaDTO> resumenPorDia, List<ProductoMasVendidoDTO> top5Productos) {
        this.desde = desde;
        this.hasta = hasta;
        this.totalPeriodo = totalPeriodo;
        this.ticketPromedio = ticketPromedio;
        this.mejorDia = mejorDia;
        this.mejorDiaTotal = mejorDiaTotal;
        this.totalSesiones = totalSesiones;
        this.ventasPorMetodoPago = ventasPorMetodoPago;
        this.resumenPorDia = resumenPorDia;
        this.top5Productos = top5Productos;
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

    public BigDecimal getTotalPeriodo() {
        return totalPeriodo;
    }

    public void setTotalPeriodo(BigDecimal totalPeriodo) {
        this.totalPeriodo = totalPeriodo;
    }

    public BigDecimal getTicketPromedio() {
        return ticketPromedio;
    }

    public void setTicketPromedio(BigDecimal ticketPromedio) {
        this.ticketPromedio = ticketPromedio;
    }

    public LocalDate getMejorDia() {
        return mejorDia;
    }

    public void setMejorDia(LocalDate mejorDia) {
        this.mejorDia = mejorDia;
    }

    public BigDecimal getMejorDiaTotal() {
        return mejorDiaTotal;
    }

    public void setMejorDiaTotal(BigDecimal mejorDiaTotal) {
        this.mejorDiaTotal = mejorDiaTotal;
    }

    public Long getTotalSesiones() {
        return totalSesiones;
    }

    public void setTotalSesiones(Long totalSesiones) {
        this.totalSesiones = totalSesiones;
    }

    public Map<String, BigDecimal> getVentasPorMetodoPago() {
        return ventasPorMetodoPago;
    }

    public void setVentasPorMetodoPago(Map<String, BigDecimal> ventasPorMetodoPago) {
        this.ventasPorMetodoPago = ventasPorMetodoPago;
    }

    public List<ResumenDiaDTO> getResumenPorDia() {
        return resumenPorDia;
    }

    public void setResumenPorDia(List<ResumenDiaDTO> resumenPorDia) {
        this.resumenPorDia = resumenPorDia;
    }

    public List<ProductoMasVendidoDTO> getTop5Productos() {
        return top5Productos;
    }

    public void setTop5Productos(List<ProductoMasVendidoDTO> top5Productos) {
        this.top5Productos = top5Productos;
    }
}