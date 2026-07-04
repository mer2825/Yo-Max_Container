package com.example.acceso.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "sesiones_caja")
public class SesionCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(name = "monto_inicial", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoInicial;

    @Column(name = "monto_cierre_declarado", precision = 10, scale = 2)
    private BigDecimal montoCierreDeclarado;

    @Column(name = "monto_cierre_esperado", precision = 10, scale = 2)
    private BigDecimal montoCierreEsperado;

    @Column(precision = 10, scale = 2)
    private BigDecimal diferencia;

    @Column(name = "saldo_traspasado", precision = 10, scale = 2)
    private BigDecimal saldoTraspasado;

    @Column(length = 500)
    private String motivoDiferencia;

    @Column(length = 1000)
    private String observaciones;

    @Column(nullable = false, length = 20)
    private String estado; // ABIERTA o CERRADA

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_apertura_id", nullable = false)
    private Usuario usuarioApertura;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_cierre_id")
    private Usuario usuarioCierre;

    @OneToMany(mappedBy = "sesion", fetch = FetchType.LAZY)
    private List<MovimientoCaja> movimientos;

    @OneToMany(mappedBy = "sesionCaja", fetch = FetchType.LAZY)
    private List<Venta> ventas;

    // Constructor por defecto
    public SesionCaja() {
    }

    // Constructor con parámetros
    public SesionCaja(BigDecimal montoInicial, Usuario usuarioApertura) {
        this.fechaApertura = LocalDateTime.now();
        this.montoInicial = montoInicial;
        this.usuarioApertura = usuarioApertura;
        this.estado = "ABIERTA";
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getFechaApertura() {
        return fechaApertura;
    }

    public void setFechaApertura(LocalDateTime fechaApertura) {
        this.fechaApertura = fechaApertura;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public BigDecimal getMontoInicial() {
        return montoInicial;
    }

    public void setMontoInicial(BigDecimal montoInicial) {
        this.montoInicial = montoInicial;
    }

    public BigDecimal getMontoCierreDeclarado() {
        return montoCierreDeclarado;
    }

    public void setMontoCierreDeclarado(BigDecimal montoCierreDeclarado) {
        this.montoCierreDeclarado = montoCierreDeclarado;
    }

    public BigDecimal getMontoCierreEsperado() {
        return montoCierreEsperado;
    }

    public void setMontoCierreEsperado(BigDecimal montoCierreEsperado) {
        this.montoCierreEsperado = montoCierreEsperado;
    }

    public BigDecimal getDiferencia() {
        return diferencia;
    }

    public void setDiferencia(BigDecimal diferencia) {
        this.diferencia = diferencia;
    }

    public BigDecimal getSaldoTraspasado() {
        return saldoTraspasado;
    }

    public void setSaldoTraspasado(BigDecimal saldoTraspasado) {
        this.saldoTraspasado = saldoTraspasado;
    }

    public String getMotivoDiferencia() {
        return motivoDiferencia;
    }

    public void setMotivoDiferencia(String motivoDiferencia) {
        this.motivoDiferencia = motivoDiferencia;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Usuario getUsuarioApertura() {
        return usuarioApertura;
    }

    public void setUsuarioApertura(Usuario usuarioApertura) {
        this.usuarioApertura = usuarioApertura;
    }

    public Usuario getUsuarioCierre() {
        return usuarioCierre;
    }

    public void setUsuarioCierre(Usuario usuarioCierre) {
        this.usuarioCierre = usuarioCierre;
    }

    public List<MovimientoCaja> getMovimientos() {
        return movimientos;
    }

    public void setMovimientos(List<MovimientoCaja> movimientos) {
        this.movimientos = movimientos;
    }

    public List<Venta> getVentas() {
        return ventas;
    }

    public void setVentas(List<Venta> ventas) {
        this.ventas = ventas;
    }
}
