package com.example.acceso.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.stream.Collectors;

@Entity // Indica que es una entidad JPA
@Table(name = "proveedores") // Nombre de la tabla en la base de datos
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // 1. idProveedor
    private Long id;

    // 2. ruc o nif (identificador fiscal)
    @NotBlank(message = "El RUC o NIF es obligatorio")
    @Size(min = 8, max = 15, message = "El RUC/NIF debe tener entre 8 y 15 caracteres")
    @Column(nullable = false, unique = true, length = 15)
    private String rucNif;

    // 3. nombre (nombre con el que opera)
    @NotBlank(message = "El nombre del proveedor es obligatorio")
    @Size(min = 2, max = 150, message = "El nombre debe tener entre 2 y 150 caracteres")
    @Column(nullable = false, length = 150)
    private String nombre;

    // 4. telefono
    @Size(max = 20, message = "El teléfono no debe exceder los 20 caracteres")
    @Column(length = 20)
    private String telefono;

    // 5. paginaWeb
    @Size(max = 255, message = "La página web no debe exceder los 255 caracteres")
    @Column(length = 255)
    private String paginaWeb;

    // 6. direccion
    @Size(max = 255, message = "La dirección no debe exceder los 255 caracteres")
    @Column(length = 255)
    private String direccion;

    // 7. ciudad
    @Size(max = 50, message = "La ciudad no debe exceder los 50 caracteres")
    @Column(length = 50)
    private String ciudad;

    // 8. categoriaProductos (Se usará String/VARCHAR para almacenar una lista separada por comas)
    // Esto evita crear una tabla intermedia para el avance.
    @Column(length = 255)
    private String categoriasSuministradas;

    // 9. plazoEntrega (int)
    @NotNull(message = "El plazo de entrega es obligatorio")
    @Min(value = 0, message = "El plazo de entrega no puede ser negativo")
    @Column(nullable = false)
    private Integer plazoEntrega;

    // 10. moneda (ej. "USD", "PEN", "EUR")
    @NotBlank(message = "La moneda de transacción es obligatoria")
    @Size(min = 3, max = 5, message = "La moneda debe tener entre 3 y 5 caracteres")
    @Column(nullable = false, length = 5)
    private String moneda;

    // Estado (heredado de Producto, útil para habilitar/deshabilitar proveedores)
    @Column(nullable = false)
    private Boolean estado = true; // true (1): Activo, false (0): Inactivo

    // Constructor por defecto
    public Proveedor() {
    }

    // Constructor con parámetros (sin ID)
    public Proveedor(String rucNif, String nombre, String telefono, String paginaWeb, String direccion, String ciudad, String categoriasSuministradas, Integer plazoEntrega, String moneda) {
        this.rucNif = rucNif;
        this.nombre = nombre;
        this.telefono = telefono;
        this.paginaWeb = paginaWeb;
        this.direccion = direccion;
        this.ciudad = ciudad;
        this.categoriasSuministradas = categoriasSuministradas;
        this.plazoEntrega = plazoEntrega;
        this.moneda = moneda;
        this.estado = true;
    }

    // --- Métodos de Conveniencia para Categorías ---

    /** Convierte el String de categorías (separado por comas) a una Lista */
    @Transient // Indica a JPA que ignore este campo en la base de datos
    public List<String> getCategoriasList() {
        if (this.categoriasSuministradas == null || this.categoriasSuministradas.isEmpty()) {
            return List.of();
        }
        return List.of(this.categoriasSuministradas.split(","))
                .stream()
                .map(String::trim) // Limpia espacios
                .collect(Collectors.toList());
    }

    /** Establece las categorías desde una Lista, convirtiéndolas a String separado por comas */
    @Transient
    public void setCategoriasList(List<String> categoriasList) {
        this.categoriasSuministradas = categoriasList.stream()
                .map(String::trim)
                .collect(Collectors.joining(", "));
    }

    // --- Getters y Setters ---

    public Long getIdProveedor() {
        return id;
    }

    public void setIdProveedor(Long id) {
        this.id = id;
    }

    public String getRucNif() {
        return rucNif;
    }

    public void setRucNif(String rucNif) {
        this.rucNif = rucNif;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getPaginaWeb() {
        return paginaWeb;
    }

    public void setPaginaWeb(String paginaWeb) {
        this.paginaWeb = paginaWeb;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getCategoriasSuministradas() {
        return categoriasSuministradas;
    }

    public void setCategoriasSuministradas(String categoriasSuministradas) {
        this.categoriasSuministradas = categoriasSuministradas;
    }

    public Integer getPlazoEntrega() {
        return plazoEntrega;
    }

    public void setPlazoEntrega(Integer plazoEntrega) {
        this.plazoEntrega = plazoEntrega;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public Boolean getEstado() {
        return estado;
    }

    public void setEstado(Boolean estado) {
        this.estado = estado;
    }
}