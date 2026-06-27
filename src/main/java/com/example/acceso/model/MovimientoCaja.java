package com.example.acceso.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_caja")
public class MovimientoCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sesion_id", nullable = false)
    private SesionCaja sesion;

    @Column(nullable = false, length = 20)
    private String tipo; // RETIRO o INGRESO

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 500)
    private String motivo;

    @Column(nullable = false, length = 50)
    private String categoria;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Constructor por defecto
    public MovimientoCaja() {
    }

    // Constructor con parámetros
    public MovimientoCaja(String tipo, BigDecimal monto, String motivo, String categoria, Usuario usuario, SesionCaja sesion) {
        this.tipo = tipo;
        this.monto = monto;
        this.motivo = motivo;
        this.categoria = categoria;
        this.usuario = usuario;
        this.sesion = sesion;
        this.fecha = LocalDateTime.now();
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SesionCaja getSesion() {
        return sesion;
    }

    public void setSesion(SesionCaja sesion) {
        this.sesion = sesion;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}
