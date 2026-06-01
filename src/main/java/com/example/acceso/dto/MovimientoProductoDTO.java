package com.example.acceso.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MovimientoProductoDTO {
    private String numeroVenta;
    private LocalDateTime fechaVenta;
    private BigDecimal precioVenta;
    private Integer cantidad;
    private BigDecimal subtotal;

    public MovimientoProductoDTO(String numeroVenta, LocalDateTime fechaVenta, BigDecimal precioVenta, Integer cantidad, BigDecimal subtotal) {
        this.numeroVenta = numeroVenta;
        this.fechaVenta = fechaVenta;
        this.precioVenta = precioVenta;
        this.cantidad = cantidad;
        this.subtotal = subtotal;
    }

    // Getters
    public String getNumeroVenta() {
        return numeroVenta;
    }

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

    // Setters (opcionales si solo se usa el constructor)
    public void setNumeroVenta(String numeroVenta) {
        this.numeroVenta = numeroVenta;
    }

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
}
