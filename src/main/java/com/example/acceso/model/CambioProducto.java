package com.example.acceso.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cambios_producto")
public class CambioProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "venta_original_id", nullable = false)
    private Venta ventaOriginal;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "detalle_venta_original_id", nullable = false)
    private DetalleVenta detalleVentaOriginal;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "producto_nuevo_id", nullable = false)
    private Producto productoNuevo;

    @Column(nullable = false)
    private Integer cantidadDevuelta;

    @Column(nullable = false)
    private Integer cantidadNuevoProducto;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "nota_credito_id", nullable = false)
    private NotasCredito notaCredito;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "venta_excedente_id")
    private Venta ventaExcedente;

    @Column(name = "monto_nota_credito", precision = 10, scale = 2, nullable = false)
    private BigDecimal montoNotaCredito;

    @Column(name = "monto_producto_nuevo", precision = 10, scale = 2, nullable = false)
    private BigDecimal montoProductoNuevo;

    @Column(name = "monto_excedente", precision = 10, scale = 2, nullable = false)
    private BigDecimal montoExcedente;

    @Column(length = 500)
    private String motivo;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "fecha_cambio", nullable = false)
    private LocalDateTime fechaCambio;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Venta getVentaOriginal() {
        return ventaOriginal;
    }

    public void setVentaOriginal(Venta ventaOriginal) {
        this.ventaOriginal = ventaOriginal;
    }

    public DetalleVenta getDetalleVentaOriginal() {
        return detalleVentaOriginal;
    }

    public void setDetalleVentaOriginal(DetalleVenta detalleVentaOriginal) {
        this.detalleVentaOriginal = detalleVentaOriginal;
    }

    public Producto getProductoNuevo() {
        return productoNuevo;
    }

    public void setProductoNuevo(Producto productoNuevo) {
        this.productoNuevo = productoNuevo;
    }

    public Integer getCantidadDevuelta() {
        return cantidadDevuelta;
    }

    public void setCantidadDevuelta(Integer cantidadDevuelta) {
        this.cantidadDevuelta = cantidadDevuelta;
    }

    public Integer getCantidadNuevoProducto() {
        return cantidadNuevoProducto;
    }

    public void setCantidadNuevoProducto(Integer cantidadNuevoProducto) {
        this.cantidadNuevoProducto = cantidadNuevoProducto;
    }

    public NotasCredito getNotaCredito() {
        return notaCredito;
    }

    public void setNotaCredito(NotasCredito notaCredito) {
        this.notaCredito = notaCredito;
    }

    public Venta getVentaExcedente() {
        return ventaExcedente;
    }

    public void setVentaExcedente(Venta ventaExcedente) {
        this.ventaExcedente = ventaExcedente;
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

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCambio() {
        return fechaCambio;
    }

    public void setFechaCambio(LocalDateTime fechaCambio) {
        this.fechaCambio = fechaCambio;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}

