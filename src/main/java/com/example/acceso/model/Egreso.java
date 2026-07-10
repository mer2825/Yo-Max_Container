package com.example.acceso.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "egresos")
public class Egreso extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, length = 100)
    private String tipoEgreso;

    @Column(length = 500)
    private String comentario;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuarioRegistro;

    @Column(length = 50)
    private String numeroOperacion;

    @Column(length = 255)
    private String comprobanteUrl;

    @Column(nullable = false)
    private Integer estado = 1; // 1: Activo, 0: Inactivo, 2: Eliminado

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sesion_caja_id")
    private SesionCaja sesionCaja;

    // Constructores
    public Egreso() {
    }

    public Egreso(LocalDateTime fecha, String tipoEgreso, BigDecimal monto, Usuario usuarioRegistro) {
        this.fecha = fecha;
        this.tipoEgreso = tipoEgreso;
        this.monto = monto;
        this.usuarioRegistro = usuarioRegistro;
        this.estado = 1;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getTipoEgreso() {
        return tipoEgreso;
    }

    public void setTipoEgreso(String tipoEgreso) {
        this.tipoEgreso = tipoEgreso;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public Usuario getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(Usuario usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public String getNumeroOperacion() {
        return numeroOperacion;
    }

    public void setNumeroOperacion(String numeroOperacion) {
        this.numeroOperacion = numeroOperacion;
    }

    public String getComprobanteUrl() {
        return comprobanteUrl;
    }

    public void setComprobanteUrl(String comprobanteUrl) {
        this.comprobanteUrl = comprobanteUrl;
    }

    public Integer getEstado() {
        return estado;
    }

    public void setEstado(Integer estado) {
        this.estado = estado;
    }

    public SesionCaja getSesionCaja() {
        return sesionCaja;
    }

    public void setSesionCaja(SesionCaja sesionCaja) {
        this.sesionCaja = sesionCaja;
    }
}

