package com.example.acceso.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public class ResumenSesionActivaDTO {
    private Long sesionId;
    private LocalDateTime fechaApertura;
    private BigDecimal montoInicial;
    private BigDecimal totalVentas;
    private BigDecimal soloEfectivo;
    private BigDecimal otrosMedios;
    private Long numeroTransacciones;
    private BigDecimal ticketPromedio;
    private BigDecimal efectivoEnCaja;
    private Map<String, BigDecimal> ventasPorMetodoPago;

    public ResumenSesionActivaDTO() {
    }

    public ResumenSesionActivaDTO(Long sesionId, LocalDateTime fechaApertura, BigDecimal montoInicial,
                                   BigDecimal totalVentas, BigDecimal soloEfectivo, BigDecimal otrosMedios,
                                   Long numeroTransacciones, BigDecimal ticketPromedio, BigDecimal efectivoEnCaja,
                                   Map<String, BigDecimal> ventasPorMetodoPago) {
        this.sesionId = sesionId;
        this.fechaApertura = fechaApertura;
        this.montoInicial = montoInicial;
        this.totalVentas = totalVentas;
        this.soloEfectivo = soloEfectivo;
        this.otrosMedios = otrosMedios;
        this.numeroTransacciones = numeroTransacciones;
        this.ticketPromedio = ticketPromedio;
        this.efectivoEnCaja = efectivoEnCaja;
        this.ventasPorMetodoPago = ventasPorMetodoPago;
    }

    // Getters y Setters
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

    public BigDecimal getTotalVentas() {
        return totalVentas;
    }

    public void setTotalVentas(BigDecimal totalVentas) {
        this.totalVentas = totalVentas;
    }

    public Long getNumeroTransacciones() {
        return numeroTransacciones;
    }

    public void setNumeroTransacciones(Long numeroTransacciones) {
        this.numeroTransacciones = numeroTransacciones;
    }

    public BigDecimal getTicketPromedio() {
        return ticketPromedio;
    }

    public void setTicketPromedio(BigDecimal ticketPromedio) {
        this.ticketPromedio = ticketPromedio;
    }

    public BigDecimal getEfectivoEnCaja() {
        return efectivoEnCaja;
    }

    public void setEfectivoEnCaja(BigDecimal efectivoEnCaja) {
        this.efectivoEnCaja = efectivoEnCaja;
    }

    public BigDecimal getSoloEfectivo() {
        return soloEfectivo;
    }

    public void setSoloEfectivo(BigDecimal soloEfectivo) {
        this.soloEfectivo = soloEfectivo;
    }

    public BigDecimal getOtrosMedios() {
        return otrosMedios;
    }

    public void setOtrosMedios(BigDecimal otrosMedios) {
        this.otrosMedios = otrosMedios;
    }

    public Map<String, BigDecimal> getVentasPorMetodoPago() {
        return ventasPorMetodoPago;
    }

    public void setVentasPorMetodoPago(Map<String, BigDecimal> ventasPorMetodoPago) {
        this.ventasPorMetodoPago = ventasPorMetodoPago;
    }
}