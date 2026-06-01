package com.example.acceso.dto;

import java.math.BigDecimal;

public class ProductoMasVendidoDTO {
    private String nombreProducto;
    private Long unidadesVendidas;
    private BigDecimal totalDinero;

    public ProductoMasVendidoDTO(String nombreProducto, Long unidadesVendidas, BigDecimal totalDinero) {
        this.nombreProducto = nombreProducto;
        this.unidadesVendidas = unidadesVendidas;
        this.totalDinero = totalDinero;
    }

    // Getters y Setters
    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public Long getUnidadesVendidas() {
        return unidadesVendidas;
    }

    public void setUnidadesVendidas(Long unidadesVendidas) {
        this.unidadesVendidas = unidadesVendidas;
    }

    public BigDecimal getTotalDinero() {
        return totalDinero;
    }

    public void setTotalDinero(BigDecimal totalDinero) {
        this.totalDinero = totalDinero;
    }
}
