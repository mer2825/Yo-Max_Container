package com.example.acceso.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movimientos")
public class StockMovimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "tipo", length = 50)
    private String tipo; // 'AJUSTE', 'INICIAL', etc. (Mantenido para compatibilidad)

    @Column(name = "comentario", length = 500)
    private String comentario;

    // Nuevos campos opcionales para enriquecer el inventario
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimiento", length = 20)
    private TipoMovimiento tipoMovimiento; // ingreso, salida, ajuste, venta

    @Column(name = "motivo", length = 100)
    private String motivo; // Lista predefinida según tipo

    @Column(name = "referencia_documento", length = 50)
    private String referenciaDocumento; // Número de factura, guía, etc.

    @Column(name = "proveedor", length = 100)
    private String proveedor;

    @Column(name = "observacion", length = 1000)
    private String observacion;

    @Column(name = "stock_anterior")
    private Integer stockAnterior; // Calculado automáticamente al guardar

    @Column(name = "stock_resultante")
    private Integer stockResultante; // Calculado automáticamente

    public StockMovimiento() {
    }

    public StockMovimiento(Long productoId, LocalDateTime fecha, Integer cantidad, String tipo, String comentario) {
        this.productoId = productoId;
        this.fecha = fecha;
        this.cantidad = cantidad;
        this.tipo = tipo;
        this.comentario = comentario;
    }

    public Long getId() {
        return id;
    }

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    // Getters y setters para nuevos campos
    public TipoMovimiento getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(TipoMovimiento tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getReferenciaDocumento() {
        return referenciaDocumento;
    }

    public void setReferenciaDocumento(String referenciaDocumento) {
        this.referenciaDocumento = referenciaDocumento;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public Integer getStockAnterior() {
        return stockAnterior;
    }

    public void setStockAnterior(Integer stockAnterior) {
        this.stockAnterior = stockAnterior;
    }

    public Integer getStockResultante() {
        return stockResultante;
    }

    public void setStockResultante(Integer stockResultante) {
        this.stockResultante = stockResultante;
    }
}
