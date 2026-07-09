package com.example.acceso.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CambioProductoRequestDTO {

    @NotNull
    private Long ventaOriginalId;

    @NotNull
    private Long detalleVentaId;

    @NotNull
    private Long productoNuevoId;

    @NotNull
    @Min(1)
    private Integer cantidadDevuelta;

    @NotNull
    @Min(1)
    private Integer cantidadNuevoProducto;

    @Size(max = 500)
    private String motivo;

    public Long getVentaOriginalId() {
        return ventaOriginalId;
    }

    public void setVentaOriginalId(Long ventaOriginalId) {
        this.ventaOriginalId = ventaOriginalId;
    }

    public Long getDetalleVentaId() {
        return detalleVentaId;
    }

    public void setDetalleVentaId(Long detalleVentaId) {
        this.detalleVentaId = detalleVentaId;
    }

    public Long getProductoNuevoId() {
        return productoNuevoId;
    }

    public void setProductoNuevoId(Long productoNuevoId) {
        this.productoNuevoId = productoNuevoId;
    }

    public Integer getCantidadDevuelta() {
        return cantidadDevuelta;
    }

    public void setCantidadDevuelta(Integer cantidadDevuelta) {
        this.cantidadDevuelta = cantidadDevuelta;
    }

    public Integer getCantidadNuevoProducto() {
        return cantidadNuevoProducto;
    }

    public void setCantidadNuevoProducto(Integer cantidadNuevoProducto) {
        this.cantidadNuevoProducto = cantidadNuevoProducto;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}

