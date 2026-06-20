package com.example.acceso.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MovimientoProductoDTO {
    private String numeroVenta;
    private LocalDateTime fechaVenta;
    private BigDecimal precioVenta;
    private Integer cantidad;
    private BigDecimal subtotal;

    // Nuevos campos para movimientos de inventario enriquecidos
    private LocalDateTime fecha;
    private String tipoMovimiento;
    private Integer stockAnterior;
    private Integer stockResultante;
    private String motivo;
    private String referenciaDocumento;
    private String observacion;
    private String usuario;

    // Constructor para ventas (mantiene compatibilidad)
    public MovimientoProductoDTO(String numeroVenta, LocalDateTime fechaVenta, BigDecimal precioVenta, Integer cantidad, BigDecimal subtotal) {
        this.numeroVenta = numeroVenta;
        this.fechaVenta = fechaVenta;
        this.precioVenta = precioVenta;
        this.cantidad = cantidad;
        this.subtotal = subtotal;
    }

    // Constructor para movimientos de inventario
    public MovimientoProductoDTO(String tipoMovimiento, LocalDateTime fecha, Integer cantidad, BigDecimal precioVenta, BigDecimal subtotal) {
        this.tipoMovimiento = tipoMovimiento;
        this.fecha = fecha;
        this.cantidad = cantidad;
        this.precioVenta = precioVenta;
        this.subtotal = subtotal;
    }

    // Getters
    public LocalDateTime getFechaVenta() {
        return fechaVenta;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getTipoMovimiento() {
        return tipoMovimiento;
    }

    public Integer getStockAnterior() {
        return stockAnterior;
    }

    public Integer getStockResultante() {
        return stockResultante;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getReferenciaDocumento() {
        return referenciaDocumento;
    }

    public String getObservacion() {
        return observacion;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getNumeroVenta() {
        return numeroVenta;
    }

    // Setters
    public void setFechaVenta(LocalDateTime fechaVenta) {
        this.fechaVenta = fechaVenta;
    }

    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public void setTipoMovimiento(String tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }

    public void setStockAnterior(Integer stockAnterior) {
        this.stockAnterior = stockAnterior;
    }

    public void setStockResultante(Integer stockResultante) {
        this.stockResultante = stockResultante;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public void setReferenciaDocumento(String referenciaDocumento) {
        this.referenciaDocumento = referenciaDocumento;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public void setNumeroVenta(String numeroVenta) {
        this.numeroVenta = numeroVenta;
    }
}
