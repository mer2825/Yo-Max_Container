package com.example.acceso.dto;

public class AuditDetailsDto {

    private String creadoPorNombre;
    private String fechaCreacion;
    private String modificadoPorNombre;
    private String fechaModificacion;
    private String ultimaAccion;

    // Constructores
    public AuditDetailsDto() {
    }

    public AuditDetailsDto(String creadoPorNombre, String fechaCreacion, String modificadoPorNombre, String fechaModificacion, String ultimaAccion) {
        this.creadoPorNombre = creadoPorNombre;
        this.fechaCreacion = fechaCreacion;
        this.modificadoPorNombre = modificadoPorNombre;
        this.fechaModificacion = fechaModificacion;
        this.ultimaAccion = ultimaAccion;
    }

    // Getters y Setters
    public String getCreadoPorNombre() {
        return creadoPorNombre;
    }

    public void setCreadoPorNombre(String creadoPorNombre) {
        this.creadoPorNombre = creadoPorNombre;
    }

    public String getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(String fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getModificadoPorNombre() {
        return modificadoPorNombre;
    }

    public void setModificadoPorNombre(String modificadoPorNombre) {
        this.modificadoPorNombre = modificadoPorNombre;
    }

    public String getFechaModificacion() {
        return fechaModificacion;
    }

    public void setFechaModificacion(String fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }

    public String getUltimaAccion() {
        return ultimaAccion;
    }

    public void setUltimaAccion(String ultimaAccion) {
        this.ultimaAccion = ultimaAccion;
    }
}