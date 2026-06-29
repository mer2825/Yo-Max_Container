package com.example.acceso.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "notas_credito")
public class NotasCredito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    @JsonBackReference
    private Venta venta;

    @Column(name = "tipo_nota", nullable = false, length = 10)
    private String tipoNota; // "01","06","07","09"

    @Column(name = "descripcion_tipo", length = 255)
    private String descripcionTipo;

    @Column(nullable = false, length = 1000)
    private String motivo;

    @Column(length = 20)
    private String serie;

    @Column
    private Integer correlativo;

    @Column(name = "serie_correlativo", length = 50)
    private String serieCorrelativo;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalAcreditado;

    @Column(name = "estado_sunat", nullable = false, length = 50)
    private String estadoSunat = "pendiente"; // pendiente, aceptado, rechazado

    @Column(name = "nubefact_id", length = 255)
    private String nubefactId;

    @Column(name = "pdf_url", length = 255)
    private String pdfUrl;

    @Column(name = "xml_url", length = 255)
    private String xmlUrl;

    @Column(name = "hash_cdr", length = 255)
    private String hashCdr;

    @Column(name = "fecha_emision")
    private LocalDateTime fechaEmision;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "emitida_por_usuario_id")
    private Usuario emitidaPorUsuario;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Venta getVenta() {
        return venta;
    }

    public void setVenta(Venta venta) {
        this.venta = venta;
    }

    public String getTipoNota() {
        return tipoNota;
    }

    public void setTipoNota(String tipoNota) {
        this.tipoNota = tipoNota;
    }

    public String getDescripcionTipo() {
        return descripcionTipo;
    }

    public void setDescripcionTipo(String descripcionTipo) {
        this.descripcionTipo = descripcionTipo;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie;
    }

    public Integer getCorrelativo() {
        return correlativo;
    }

    public void setCorrelativo(Integer correlativo) {
        this.correlativo = correlativo;
    }

    public String getSerieCorrelativo() {
        return serieCorrelativo;
    }

    public void setSerieCorrelativo(String serieCorrelativo) {
        this.serieCorrelativo = serieCorrelativo;
    }

    public BigDecimal getTotalAcreditado() {
        return totalAcreditado;
    }

    public void setTotalAcreditado(BigDecimal totalAcreditado) {
        this.totalAcreditado = totalAcreditado;
    }

    public String getEstadoSunat() {
        return estadoSunat;
    }

    public void setEstadoSunat(String estadoSunat) {
        this.estadoSunat = estadoSunat;
    }

    public String getNubefactId() {
        return nubefactId;
    }

    public void setNubefactId(String nubefactId) {
        this.nubefactId = nubefactId;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public String getXmlUrl() {
        return xmlUrl;
    }

    public void setXmlUrl(String xmlUrl) {
        this.xmlUrl = xmlUrl;
    }

    public String getHashCdr() {
        return hashCdr;
    }

    public void setHashCdr(String hashCdr) {
        this.hashCdr = hashCdr;
    }

    public LocalDateTime getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDateTime fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public Usuario getEmitidaPorUsuario() {
        return emitidaPorUsuario;
    }

    public void setEmitidaPorUsuario(Usuario emitidaPorUsuario) {
        this.emitidaPorUsuario = emitidaPorUsuario;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }
}
