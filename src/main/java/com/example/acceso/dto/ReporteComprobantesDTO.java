package com.example.acceso.dto;

import com.example.acceso.model.Venta;

import java.math.BigDecimal;
import java.util.List;

public class ReporteComprobantesDTO {

    private long totalBoletas;
    private BigDecimal montoBoletas;
    private long totalFacturas;
    private BigDecimal montoFacturas;
    private BigDecimal igvGenerado;
    private BigDecimal baseImponible;
    private int cantidadNotasCredito;
    private BigDecimal totalNotasCredito;
    private List<Venta> ventas;

    public ReporteComprobantesDTO() {
    }

    public ReporteComprobantesDTO(long totalBoletas, BigDecimal montoBoletas,
                                  long totalFacturas, BigDecimal montoFacturas,
                                  BigDecimal igvGenerado, BigDecimal baseImponible,
                                  int cantidadNotasCredito, BigDecimal totalNotasCredito,
                                  List<Venta> ventas) {
        this.totalBoletas = totalBoletas;
        this.montoBoletas = montoBoletas;
        this.totalFacturas = totalFacturas;
        this.montoFacturas = montoFacturas;
        this.igvGenerado = igvGenerado;
        this.baseImponible = baseImponible;
        this.cantidadNotasCredito = cantidadNotasCredito;
        this.totalNotasCredito = totalNotasCredito;
        this.ventas = ventas;
    }

    public long getTotalBoletas() {
        return totalBoletas;
    }

    public void setTotalBoletas(long totalBoletas) {
        this.totalBoletas = totalBoletas;
    }

    public BigDecimal getMontoBoletas() {
        return montoBoletas;
    }

    public void setMontoBoletas(BigDecimal montoBoletas) {
        this.montoBoletas = montoBoletas;
    }

    public long getTotalFacturas() {
        return totalFacturas;
    }

    public void setTotalFacturas(long totalFacturas) {
        this.totalFacturas = totalFacturas;
    }

    public BigDecimal getMontoFacturas() {
        return montoFacturas;
    }

    public void setMontoFacturas(BigDecimal montoFacturas) {
        this.montoFacturas = montoFacturas;
    }

    public BigDecimal getIgvGenerado() {
        return igvGenerado;
    }

    public void setIgvGenerado(BigDecimal igvGenerado) {
        this.igvGenerado = igvGenerado;
    }

    public BigDecimal getBaseImponible() {
        return baseImponible;
    }

    public void setBaseImponible(BigDecimal baseImponible) {
        this.baseImponible = baseImponible;
    }

    public int getCantidadNotasCredito() {
        return cantidadNotasCredito;
    }

    public void setCantidadNotasCredito(int cantidadNotasCredito) {
        this.cantidadNotasCredito = cantidadNotasCredito;
    }

    public BigDecimal getTotalNotasCredito() {
        return totalNotasCredito;
    }

    public void setTotalNotasCredito(BigDecimal totalNotasCredito) {
        this.totalNotasCredito = totalNotasCredito;
    }

    public List<Venta> getVentas() {
        return ventas;
    }

    public void setVentas(List<Venta> ventas) {
        this.ventas = ventas;
    }
}