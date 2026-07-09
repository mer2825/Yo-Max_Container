package com.example.acceso.dto;

import java.math.BigDecimal;

public class CambioProductoResponseDTO {

    private Long cambioProductoId;

    private String estado;

    private String ncSerieCorrelativo;
    private String ncEstadoSunat;
    private String ncPdfUrl;

    private BigDecimal montoNotaCredito;
    private BigDecimal montoProductoNuevo;
    private BigDecimal montoExcedente;

    private Long ventaExcedenteId;
    private String ventaExcedenteSerieCorrelativo;
    private String ventaExcedenteEstadoSunat;
    private String ventaExcedentePdfUrl;

    // getters/setters ya existentes

    private String mensaje;

    public Long getCambioProductoId() {
        return cambioProductoId;
    }

    public void setCambioProductoId(Long cambioProductoId) {
        this.cambioProductoId = cambioProductoId;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getNcSerieCorrelativo() {
        return ncSerieCorrelativo;
    }

    public void setNcSerieCorrelativo(String ncSerieCorrelativo) {
        this.ncSerieCorrelativo = ncSerieCorrelativo;
    }

    public String getNcEstadoSunat() {
        return ncEstadoSunat;
    }

    public void setNcEstadoSunat(String ncEstadoSunat) {
        this.ncEstadoSunat = ncEstadoSunat;
    }

    public String getNcPdfUrl() {
        return ncPdfUrl;
    }

    public void setNcPdfUrl(String ncPdfUrl) {
        this.ncPdfUrl = ncPdfUrl;
    }

    public BigDecimal getMontoNotaCredito() {
        return montoNotaCredito;
    }

    public void setMontoNotaCredito(BigDecimal montoNotaCredito) {
        this.montoNotaCredito = montoNotaCredito;
    }

    public BigDecimal getMontoProductoNuevo() {
        return montoProductoNuevo;
    }

    public void setMontoProductoNuevo(BigDecimal montoProductoNuevo) {
        this.montoProductoNuevo = montoProductoNuevo;
    }

    public BigDecimal getMontoExcedente() {
        return montoExcedente;
    }

    public void setMontoExcedente(BigDecimal montoExcedente) {
        this.montoExcedente = montoExcedente;
    }

    public Long getVentaExcedenteId() {
        return ventaExcedenteId;
    }

    public void setVentaExcedenteId(Long ventaExcedenteId) {
        this.ventaExcedenteId = ventaExcedenteId;
    }

    public String getVentaExcedenteSerieCorrelativo() {
        return ventaExcedenteSerieCorrelativo;
    }

    public void setVentaExcedenteSerieCorrelativo(String ventaExcedenteSerieCorrelativo) {
        this.ventaExcedenteSerieCorrelativo = ventaExcedenteSerieCorrelativo;
    }

    public String getVentaExcedenteEstadoSunat() {
        return ventaExcedenteEstadoSunat;
    }

    public void setVentaExcedenteEstadoSunat(String ventaExcedenteEstadoSunat) {
        this.ventaExcedenteEstadoSunat = ventaExcedenteEstadoSunat;
    }

    public String getVentaExcedentePdfUrl() {
        return ventaExcedentePdfUrl;
    }

    public void setVentaExcedentePdfUrl(String ventaExcedentePdfUrl) {
        this.ventaExcedentePdfUrl = ventaExcedentePdfUrl;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}

