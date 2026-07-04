package com.example.acceso.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MovimientoLogDTO {
    private String tipo; // APERTURA, VENTA_EFECTIVO, VENTA_OTRO, RETIRO, INGRESO, CIERRE
    private String descripcion;
    private BigDecimal monto;
    private String signo; // + o −
    private boolean impactaEfectivo;
    private String metodoPago; // nullable
    private String categoria; // nullable
    private String usuario;
    private LocalDateTime fecha;

    public MovimientoLogDTO() {
    }

    public MovimientoLogDTO(String tipo, String descripcion, BigDecimal monto, String signo,
                            boolean impactaEfectivo, String metodoPago, String categoria,
                            String usuario, LocalDateTime fecha) {
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.monto = monto;
        this.signo = signo;
        this.impactaEfectivo = impactaEfectivo;
        this.metodoPago = metodoPago;
        this.categoria = categoria;
        this.usuario = usuario;
        this.fecha = fecha;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getSigno() {
        return signo;
    }

    public void setSigno(String signo) {
        this.signo = signo;
    }

    public boolean isImpactaEfectivo() {
        return impactaEfectivo;
    }

    public void setImpactaEfectivo(boolean impactaEfectivo) {
        this.impactaEfectivo = impactaEfectivo;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}