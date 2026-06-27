package com.example.acceso.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "productos")
public class Producto extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_categoria", nullable = false)
    @NotNull(message = "La categoría del producto es obligatoria")
    private Categoria categoria;

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    @Column(nullable = false, length = 100)
    private String nombre;

    @Size(max = 1000, message = "La descripción no debe exceder los 1000 caracteres")
    @Column(length = 1000)
    private String descripcion;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", inclusive = true, message = "El precio debe ser mayor a S/ 0.00")
    @DecimalMax(value = "9999.99", inclusive = true, message = "El precio máximo permitido es S/ 9,999.99")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    @Max(value = 9999, message = "El stock máximo es 9,999 unidades")
    @Column(nullable = false)
    private Integer stock;

    @NotNull(message = "El stock mínimo es obligatorio")
    @Min(value = 0, message = "El stock mínimo no puede ser negativo")
    @Max(value = 999, message = "El stock mínimo de alerta no puede superar 999")
    @Column(nullable = false)
    private Integer stockMinimo;

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orden ASC") // Ordenar las imágenes por el nuevo campo 'orden'
    private List<ProductoImagen> imagenes = new ArrayList<>();

    @Column(nullable = false)
    private Integer estado = 1;

    public Producto() {
    }

    public Producto(Categoria categoria, String nombre, String descripcion, BigDecimal precio, Integer stock, Integer stockMinimo) {
        this.categoria = categoria;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.stock = stock;
        this.stockMinimo = stockMinimo;
        this.estado = 1;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
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

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(Integer stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public List<ProductoImagen> getImagenes() {
        return imagenes;
    }

    public void setImagenes(List<ProductoImagen> imagenes) {
        this.imagenes = imagenes;
    }

    public Integer getEstado() {
        return estado;
    }

    public void setEstado(Integer estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return "Producto{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", precio=" + precio +
                ", stock=" + stock +
                ", stockMinimo=" + stockMinimo +
                ", estado=" + estado +
                ", categoria=" + (categoria != null ? categoria.getNombre() : "null") +
                '}';
    }
}
