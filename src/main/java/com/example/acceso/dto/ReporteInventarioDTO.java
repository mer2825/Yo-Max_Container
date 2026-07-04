package com.example.acceso.dto;

import com.example.acceso.model.Producto;

import java.math.BigDecimal;
import java.util.List;

public class ReporteInventarioDTO {

    private int totalProductos;
    private BigDecimal valorTotalInventario;
    private List<Producto> productosBajoMinimo;
    private List<Producto> productoSinMovimiento;

    public ReporteInventarioDTO() {
    }

    public ReporteInventarioDTO(int totalProductos, BigDecimal valorTotalInventario,
                                List<Producto> productosBajoMinimo,
                                List<Producto> productoSinMovimiento) {
        this.totalProductos = totalProductos;
        this.valorTotalInventario = valorTotalInventario;
        this.productosBajoMinimo = productosBajoMinimo;
        this.productoSinMovimiento = productoSinMovimiento;
    }

    public int getTotalProductos() {
        return totalProductos;
    }

    public void setTotalProductos(int totalProductos) {
        this.totalProductos = totalProductos;
    }

    public BigDecimal getValorTotalInventario() {
        return valorTotalInventario;
    }

    public void setValorTotalInventario(BigDecimal valorTotalInventario) {
        this.valorTotalInventario = valorTotalInventario;
    }

    public List<Producto> getProductosBajoMinimo() {
        return productosBajoMinimo;
    }

    public void setProductosBajoMinimo(List<Producto> productosBajoMinimo) {
        this.productosBajoMinimo = productosBajoMinimo;
    }

    public List<Producto> getProductoSinMovimiento() {
        return productoSinMovimiento;
    }

    public void setProductoSinMovimiento(List<Producto> productoSinMovimiento) {
        this.productoSinMovimiento = productoSinMovimiento;
    }
}