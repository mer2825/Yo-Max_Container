package com.example.acceso.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CierreResumenDTO {

    private Long sesionId;
    private LocalDateTime fechaApertura;

    private BigDecimal montoInicial;

    private BigDecimal totalEfectivo;
    private BigDecimal totalYape;
    private BigDecimal totalVentas;
    private BigDecimal totalIngresos;
    private BigDecimal totalRetiros;

    private BigDecimal efectivoEsperado;

    private int cantidadVentas;
    private BigDecimal ticketPromedio;
    private String duracion;

    private int cantidadMovimientos;

    public CierreResumenDTO() {
    }

    public CierreResumenDTO(Long sesionId,
                             LocalDateTime fechaApertura,
                             BigDecimal montoInicial,
                             BigDecimal totalEfectivo,
                             BigDecimal totalYape,
                             BigDecimal totalVentas,
                             BigDecimal totalIngresos,
                             BigDecimal totalRetiros,
                             BigDecimal efectivoEsperado,
                             int cantidadVentas,
                             BigDecimal ticketPromedio,
                             String duracion,
                             int cantidadMovimientos) {
        this.sesionId = sesionId;
        this.fechaApertura = fechaApertura;
        this.montoInicial = montoInicial;
        this.totalEfectivo = totalEfectivo;
        this.totalYape = totalYape;
        this.totalVentas = totalVentas;
        this.totalIngresos = totalIngresos;
        this.totalRetiros = totalRetiros;
        this.efectivoEsperado = efectivoEsperado;
        this.cantidadVentas = cantidadVentas;
        this.ticketPromedio = ticketPromedio;
        this.duracion = duracion;
        this.cantidadMovimientos = cantidadMovimientos;
    }

    public Long getSesionId() {
        return sesionId;
    }

    public void setSesionId(Long sesionId) {
        this.sesionId = sesionId;
    }

    public LocalDateTime getFechaApertura() {
        return fechaApertura;
    }

    public void setFechaApertura(LocalDateTime fechaApertura) {
        this.fechaApertura = fechaApertura;
    }

    public BigDecimal getMontoInicial() {
        return montoInicial;
    }

    public void setMontoInicial(BigDecimal montoInicial) {
        this.montoInicial = montoInicial;
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

    public BigDecimal getTotalVentas() {
        return totalVentas;
    }

    public void setTotalVentas(BigDecimal totalVentas) {
        this.totalVentas = totalVentas;
    }

    public BigDecimal getTotalIngresos() {
        return totalIngresos;
    }

    public void setTotalIngresos(BigDecimal totalIngresos) {
        this.totalIngresos = totalIngresos;
    }

    public BigDecimal getTotalRetiros() {
        return totalRetiros;
    }

    public void setTotalRetiros(BigDecimal totalRetiros) {
        this.totalRetiros = totalRetiros;
    }

    public BigDecimal getEfectivoEsperado() {
        return efectivoEsperado;
    }

    public void setEfectivoEsperado(BigDecimal efectivoEsperado) {
        this.efectivoEsperado = efectivoEsperado;
    }

    public int getCantidadVentas() {
        return cantidadVentas;
    }

    public void setCantidadVentas(int cantidadVentas) {
        this.cantidadVentas = cantidadVentas;
    }

    public BigDecimal getTicketPromedio() {
        return ticketPromedio;
    }

    public void setTicketPromedio(BigDecimal ticketPromedio) {
        this.ticketPromedio = ticketPromedio;
    }

    public String getDuracion() {
        return duracion;
    }

    public void setDuracion(String duracion) {
        this.duracion = duracion;
    }

    public int getCantidadMovimientos() {
        return cantidadMovimientos;
    }

    public void setCantidadMovimientos(int cantidadMovimientos) {
        this.cantidadMovimientos = cantidadMovimientos;
    }

    /**
     * Diferencia neta (ingresos - retiros).
     * Útil para mostrar en la vista de cierre.
     */
    public BigDecimal getMovimientosNeto() {
        BigDecimal ingresos = totalIngresos != null ? totalIngresos : BigDecimal.ZERO;
        BigDecimal retiros = totalRetiros != null ? totalRetiros : BigDecimal.ZERO;
        return ingresos.subtract(retiros);
    }
}
