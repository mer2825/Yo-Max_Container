package com.example.acceso.dto;

import java.math.BigDecimal;
import java.util.Map;

public class ReporteVentasDTO {

    private BigDecimal totalVendido;
    private int cantTransacciones;
    private BigDecimal ticketPromedio;
    private Map<String, BigDecimal> porMetodoPago;
    private Map<String, BigDecimal> porCategoria;
    private Map<String, BigDecimal> porDia;
    private BigDecimal totalNotasCredito;
    private int cantNotasCredito;
    private Map<String, Long> topProductos;

    public ReporteVentasDTO() {
    }

    public ReporteVentasDTO(BigDecimal totalVendido, int cantTransacciones,
                            BigDecimal ticketPromedio,
                            Map<String, BigDecimal> porMetodoPago,
                            Map<String, BigDecimal> porCategoria,
                            Map<String, BigDecimal> porDia,
                            BigDecimal totalNotasCredito, int cantNotasCredito,
                            Map<String, Long> topProductos) {
        this.totalVendido = totalVendido;
        this.cantTransacciones = cantTransacciones;
        this.ticketPromedio = ticketPromedio;
        this.porMetodoPago = porMetodoPago;
        this.porCategoria = porCategoria;
        this.porDia = porDia;
        this.totalNotasCredito = totalNotasCredito;
        this.cantNotasCredito = cantNotasCredito;
        this.topProductos = topProductos;
    }

    public BigDecimal getTotalVendido() {
        return totalVendido;
    }

    public void setTotalVendido(BigDecimal totalVendido) {
        this.totalVendido = totalVendido;
    }

    public int getCantTransacciones() {
        return cantTransacciones;
    }

    public void setCantTransacciones(int cantTransacciones) {
        this.cantTransacciones = cantTransacciones;
    }

    public BigDecimal getTicketPromedio() {
        return ticketPromedio;
    }

    public void setTicketPromedio(BigDecimal ticketPromedio) {
        this.ticketPromedio = ticketPromedio;
    }

    public Map<String, BigDecimal> getPorMetodoPago() {
        return porMetodoPago;
    }

    public void setPorMetodoPago(Map<String, BigDecimal> porMetodoPago) {
        this.porMetodoPago = porMetodoPago;
    }

    public Map<String, BigDecimal> getPorCategoria() {
        return porCategoria;
    }

    public void setPorCategoria(Map<String, BigDecimal> porCategoria) {
        this.porCategoria = porCategoria;
    }

    public Map<String, BigDecimal> getPorDia() {
        return porDia;
    }

    public void setPorDia(Map<String, BigDecimal> porDia) {
        this.porDia = porDia;
    }

    public BigDecimal getTotalNotasCredito() {
        return totalNotasCredito;
    }

    public void setTotalNotasCredito(BigDecimal totalNotasCredito) {
        this.totalNotasCredito = totalNotasCredito;
    }

    public int getCantNotasCredito() {
        return cantNotasCredito;
    }

    public void setCantNotasCredito(int cantNotasCredito) {
        this.cantNotasCredito = cantNotasCredito;
    }

    public Map<String, Long> getTopProductos() {
        return topProductos;
    }

    public void setTopProductos(Map<String, Long> topProductos) {
        this.topProductos = topProductos;
    }
}