package com.example.acceso.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "empresa")
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 255)
    private String direccion;

    @Column(length = 20)
    private String telefono;

    @Column(length = 100)
    private String email;

    @Column(length = 11)
    private String rucEmpresa;

    @Column(length = 255)
    private String razonSocialEmpresa;

    @Column(length = 255)
    private String direccionEmpresa;

    @Column(length = 20, columnDefinition = "varchar(20) default 'B001'")
    private String serieBoleta = "B001";

    @Column(length = 20, columnDefinition = "varchar(20) default 'F001'")
    private String serieFactura = "F001";

    @Column(columnDefinition = "integer default 1")
    private Integer correlativoBoleta = 1;

    @Column(columnDefinition = "integer default 1")
    private Integer correlativoFactura = 1;

    // Correlativos de Notas de Crédito (boleta / factura)
    @Column(columnDefinition = "integer default 1")
    private Integer correlativoNotaCreditoBoleta = 1;

    @Column(columnDefinition = "integer default 1")
    private Integer correlativoNotaCreditoFactura = 1;

    @Column(length = 20, columnDefinition = "varchar(20) default 'demo'")
    private String nubefactAmbiente = "demo";

    @Column(length = 255)
    private String logoUrl;

    @Column(length = 255)
    private String logoPublicId;

    @Column(columnDefinition = "TEXT")
    private String nosotros;

    @Column(length = 20)
    private String numeroYape;

    @Column(length = 100)
    private String titularYape;

    @Column(length = 255)
    private String qrYapeUrl;

    @Column(length = 255)
    private String qrYapePublicId;

    // Relación Many-to-Many con Productos
    @ManyToMany(fetch = FetchType.EAGER) // EAGER para que siempre se carguen con la empresa
    @JoinTable(
        name = "empresa_productos_destacados",
        joinColumns = @JoinColumn(name = "empresa_id"),
        inverseJoinColumns = @JoinColumn(name = "producto_id")
    )
    private List<Producto> productosDestacados;

    public Empresa() {
    }

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

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRucEmpresa() {
        return rucEmpresa;
    }

    public void setRucEmpresa(String rucEmpresa) {
        this.rucEmpresa = rucEmpresa;
    }

    public String getRazonSocialEmpresa() {
        return razonSocialEmpresa;
    }

    public void setRazonSocialEmpresa(String razonSocialEmpresa) {
        this.razonSocialEmpresa = razonSocialEmpresa;
    }

    public String getDireccionEmpresa() {
        return direccionEmpresa;
    }

    public void setDireccionEmpresa(String direccionEmpresa) {
        this.direccionEmpresa = direccionEmpresa;
    }

    public String getSerieBoleta() {
        return serieBoleta;
    }

    public void setSerieBoleta(String serieBoleta) {
        this.serieBoleta = serieBoleta;
    }

    public String getSerieFactura() {
        return serieFactura;
    }

    public void setSerieFactura(String serieFactura) {
        this.serieFactura = serieFactura;
    }

    public Integer getCorrelativoBoleta() {
        return correlativoBoleta;
    }

    public void setCorrelativoBoleta(Integer correlativoBoleta) {
        this.correlativoBoleta = correlativoBoleta;
    }

    public Integer getCorrelativoFactura() {
        return correlativoFactura;
    }

    public void setCorrelativoFactura(Integer correlativoFactura) {
        this.correlativoFactura = correlativoFactura;
    }

    public Integer getCorrelativoNotaCreditoBoleta() {
        return correlativoNotaCreditoBoleta;
    }

    public void setCorrelativoNotaCreditoBoleta(Integer correlativoNotaCreditoBoleta) {
        this.correlativoNotaCreditoBoleta = correlativoNotaCreditoBoleta;
    }

    public Integer getCorrelativoNotaCreditoFactura() {
        return correlativoNotaCreditoFactura;
    }

    public void setCorrelativoNotaCreditoFactura(Integer correlativoNotaCreditoFactura) {
        this.correlativoNotaCreditoFactura = correlativoNotaCreditoFactura;
    }

    public String getNubefactAmbiente() {
        return nubefactAmbiente;
    }

    public void setNubefactAmbiente(String nubefactAmbiente) {
        this.nubefactAmbiente = nubefactAmbiente;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getLogoPublicId() {
        return logoPublicId;
    }

    public void setLogoPublicId(String logoPublicId) {
        this.logoPublicId = logoPublicId;
    }

    public String getNosotros() {
        return nosotros;
    }

    public void setNosotros(String nosotros) {
        this.nosotros = nosotros;
    }

    public String getNumeroYape() {
        return numeroYape;
    }

    public void setNumeroYape(String numeroYape) {
        this.numeroYape = numeroYape;
    }

    public String getTitularYape() {
        return titularYape;
    }

    public void setTitularYape(String titularYape) {
        this.titularYape = titularYape;
    }

    public String getQrYapeUrl() {
        return qrYapeUrl;
    }

    public void setQrYapeUrl(String qrYapeUrl) {
        this.qrYapeUrl = qrYapeUrl;
    }

    public String getQrYapePublicId() {
        return qrYapePublicId;
    }

    public void setQrYapePublicId(String qrYapePublicId) {
        this.qrYapePublicId = qrYapePublicId;
    }

    public List<Producto> getProductosDestacados() {
        return productosDestacados;
    }

    public void setProductosDestacados(List<Producto> productosDestacados) {
        this.productosDestacados = productosDestacados;
    }
}