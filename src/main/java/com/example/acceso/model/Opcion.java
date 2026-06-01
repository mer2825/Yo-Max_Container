package com.example.acceso.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "opciones")
public class Opcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String ruta;

    @Column(length = 50)
    private String icono;

    @Column(name = "rutas_derivadas", length = 500)
    private String rutasDerivadas;

    @Column(nullable = false)
    private Boolean estado = true; // Añadido el campo estado

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getRuta() {
        return ruta;
    }

    public void setRuta(String ruta) {
        this.ruta = ruta;
    }

    public String getIcono() {
        return icono;
    }

    public void setIcono(String icono) {
        this.icono = icono;
    }

    public String getRutasDerivadas() {
        return rutasDerivadas;
    }

    public void setRutasDerivadas(String rutasDerivadas) {
        this.rutasDerivadas = rutasDerivadas;
    }

    public Boolean getEstado() { // Getter para estado
        return estado;
    }

    public void setEstado(Boolean estado) { // Setter para estado
        this.estado = estado;
    }
}
