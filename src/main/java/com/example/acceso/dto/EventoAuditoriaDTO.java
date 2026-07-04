package com.example.acceso.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EventoAuditoriaDTO {
    public enum TipoEvento {
        APERTURA_CAJA,
        VENTA_EFECTIVO,
        VENTA_OTRO_MEDIO,
        RETIRO_MANUAL,
        INGRESO_MANUAL,
        NOTA_CREDITO
    }
    
    private TipoEvento tipo;
    private String nombre;
    private String descripcion;
    private String usuario;
    private LocalDateTime fecha;
    private BigDecimal monto;
    private String icono;
    private String color;
    private String detalleAdicional;
    
    public EventoAuditoriaDTO() {
    }
    
    public EventoAuditoriaDTO(TipoEvento tipo, String nombre, String descripcion, String usuario, 
                             LocalDateTime fecha, BigDecimal monto, String icono, String color, String detalleAdicional) {
        this.tipo = tipo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.usuario = usuario;
        this.fecha = fecha;
        this.monto = monto;
        this.icono = icono;
        this.color = color;
        this.detalleAdicional = detalleAdicional;
    }
    
    public TipoEvento getTipo() {
        return tipo;
    }
    
    public void setTipo(TipoEvento tipo) {
        this.tipo = tipo;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
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
    
    public BigDecimal getMonto() {
        return monto;
    }
    
    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }
    
    public String getIcono() {
        return icono;
    }
    
    public void setIcono(String icono) {
        this.icono = icono;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public String getDetalleAdicional() {
        return detalleAdicional;
    }
    
    public void setDetalleAdicional(String detalleAdicional) {
        this.detalleAdicional = detalleAdicional;
    }
}