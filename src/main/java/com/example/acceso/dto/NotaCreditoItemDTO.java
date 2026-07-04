package com.example.acceso.dto;

import java.math.BigDecimal;

public class NotaCreditoItemDTO {

    private Long detalleVentaId;
    private Long productoId;
    private String descripcion;
    private Integer cantidad;
    private BigDecimal precioUnitario;

    public NotaCreditoItemDTO() {
    }

    public NotaCreditoItemDTO(Long detalleVentaId, Long productoId, String descripcion, Integer cantidad, BigDecimal precioUnitario) {
        this.detalleVentaId = detalleVentaId;
        this.productoId = productoId;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public Long getDetalleVentaId() {
        return detalleVentaId;
    }

    public void setDetalleVentaId(Long detalleVentaId) {
        this.detalleVentaId = detalleVentaId;
    }

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }
}
