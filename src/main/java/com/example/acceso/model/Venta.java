package com.example.acceso.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ventas")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String numeroVenta;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false)
    private LocalDateTime fechaVenta;

    @Column(nullable = false, length = 50)
    private String metodoPago;

    @Column(precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 10, scale = 2)
    private BigDecimal descuento;

    @Column(precision = 10, scale = 2)
    private BigDecimal total;

    @Column(length = 255)
    private String nota;

    @Column(name = "tipo_comprobante", nullable = false, length = 50)
    private String tipoComprobante = "nota_venta";

    @Column(name = "serie_correlativo", length = 50)
    private String serieCorrelativo;

    @Column(name = "estado_sunat", length = 50)
    private String estadoSunat;

    @Column(name = "cdr_sunat", columnDefinition = "TEXT")
    private String cdrSunat;

    @Column(name = "pdf_url", length = 255)
    private String pdfUrl;

    @Column(name = "xml_url", length = 255)
    private String xmlUrl;

    @Column(name = "hash_cdr", length = 255)
    private String hashCdr;

    @Column(name = "nubefact_id", length = 255)
    private String nubefactId;

    @Column(nullable = false)
    private Integer estado = 1; // 0: Inactivo, 1: Activo, 2: Eliminado (soft delete), 3: Pendiente de Procesar (Venta Web)

    @Column(nullable = false, length = 20)
    private String origen = "pos"; // pos, web

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<DetalleVenta> detalles;

    @Column(length = 20)
    private String pdfKey;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sesion_caja_id")
    private SesionCaja sesionCaja;

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumeroVenta() {
        return numeroVenta;
    }

    public void setNumeroVenta(String numeroVenta) {
        this.numeroVenta = numeroVenta;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public LocalDateTime getFechaVenta() {
        return fechaVenta;
    }

    public void setFechaVenta(LocalDateTime fechaVenta) {
        this.fechaVenta = fechaVenta;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDescuento() {
        return descuento;
    }

    public void setDescuento(BigDecimal descuento) {
        this.descuento = descuento;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getNota() {
        return nota;
    }

    public void setNota(String nota) {
        this.nota = nota;
    }

    public List<DetalleVenta> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleVenta> detalles) {
        this.detalles = detalles;
    }

    public String getTipoComprobante() {
        return tipoComprobante;
    }

    public void setTipoComprobante(String tipoComprobante) {
        this.tipoComprobante = tipoComprobante;
    }

    public String getSerieCorrelativo() {
        return serieCorrelativo;
    }

    public void setSerieCorrelativo(String serieCorrelativo) {
        this.serieCorrelativo = serieCorrelativo;
    }

    public String getEstadoSunat() {
        return estadoSunat;
    }

    public void setEstadoSunat(String estadoSunat) {
        this.estadoSunat = estadoSunat;
    }

    public String getCdrSunat() {
        return cdrSunat;
    }

    public void setCdrSunat(String cdrSunat) {
        this.cdrSunat = cdrSunat;
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

    public String getNubefactId() {
        return nubefactId;
    }

    public void setNubefactId(String nubefactId) {
        this.nubefactId = nubefactId;
    }

    public Integer getEstado() {
        return estado;
    }

    public void setEstado(Integer estado) {
        this.estado = estado;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public String getPdfKey() {
        return pdfKey;
    }

    public void setPdfKey(String pdfKey) {
        this.pdfKey = pdfKey;
    }

    public SesionCaja getSesionCaja() {
        return sesionCaja;
    }

    public void setSesionCaja(SesionCaja sesionCaja) {
        this.sesionCaja = sesionCaja;
    }
}