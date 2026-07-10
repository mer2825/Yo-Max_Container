package com.example.acceso.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "pedidos_web")
public class PedidoWeb extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El número de pedido es obligatorio")
    @Size(min = 5, max = 20, message = "El número de pedido debe tener entre 5 y 20 caracteres")
    @Column(nullable = false, unique = true, length = 20)
    private String numeroPedido;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id", nullable = true)
    private Usuario cliente;

    @NotBlank(message = "El nombre del cliente es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre del cliente debe tener entre 2 y 100 caracteres")
    @Column(nullable = false, length = 100)
    private String nombreCliente;

    @NotBlank(message = "El DNI del cliente es obligatorio")
    @Size(min = 8, max = 20, message = "El DNI del cliente debe tener entre 8 y 20 caracteres")
    @Column(nullable = false, length = 20)
    private String dniCliente;

    @NotBlank(message = "El teléfono del cliente es obligatorio")
    @Pattern(regexp = "^[0-9]{7,15}$", message = "El teléfono debe tener entre 7 y 15 dígitos")
    @Column(nullable = false, length = 20)
    private String telefonoCliente;

    @OneToMany(mappedBy = "pedidoWeb", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<DetallePedidoWeb> items;

    @NotNull(message = "El subtotal es obligatorio")
    @DecimalMin(value = "0.00", message = "El subtotal debe ser mayor o igual a 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @DecimalMin(value = "0.00", message = "El descuento debe ser mayor o igual a 0")
    @Column(precision = 10, scale = 2)
    private BigDecimal descuento;

    @NotNull(message = "El total es obligatorio")
    @DecimalMin(value = "0.01", message = "El total debe ser mayor a 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @NotBlank(message = "El método de pago es obligatorio")
    @Size(min = 3, max = 50, message = "El método de pago debe tener entre 3 y 50 caracteres")
    @Column(nullable = false, length = 50)
    private String metodoPago;

    @Size(max = 500, message = "La imagen del voucher no puede exceder 500 caracteres")
    @Column(length = 500)
    private String voucherImagen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPedidoWeb estado = EstadoPedidoWeb.PENDIENTE;

    @Size(max = 500, message = "El motivo de rechazo no puede exceder 500 caracteres")
    @Column(length = 500)
    private String motivoRechazo;

    @NotNull(message = "La fecha del pedido es obligatoria")
    @Column(nullable = false)
    private LocalDateTime fechaPedido;

    @Column
    private LocalDateTime fechaVerificacion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "verificado_por")
    private Usuario verificadoPor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "venta_id")
    @JsonIgnore
    private Venta venta;

    @Column(length = 20)
    private String pdfKey;

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumeroPedido() {
        return numeroPedido;
    }

    public void setNumeroPedido(String numeroPedido) {
        this.numeroPedido = numeroPedido;
    }

    public Usuario getCliente() {
        return cliente;
    }

    public void setCliente(Usuario cliente) {
        this.cliente = cliente;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getDniCliente() {
        return dniCliente;
    }

    public void setDniCliente(String dniCliente) {
        this.dniCliente = dniCliente;
    }

    public String getTelefonoCliente() {
        return telefonoCliente;
    }

    public void setTelefonoCliente(String telefonoCliente) {
        this.telefonoCliente = telefonoCliente;
    }

    public List<DetallePedidoWeb> getItems() {
        return items;
    }

    public void setItems(List<DetallePedidoWeb> items) {
        this.items = items;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDescuento() {
        return descuento;
    }

    public void setDescuento(BigDecimal descuento) {
        this.descuento = descuento;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getVoucherImagen() {
        return voucherImagen;
    }

    public void setVoucherImagen(String voucherImagen) {
        this.voucherImagen = voucherImagen;
    }

    public EstadoPedidoWeb getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedidoWeb estado) {
        this.estado = estado;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public LocalDateTime getFechaPedido() {
        return fechaPedido;
    }

    public void setFechaPedido(LocalDateTime fechaPedido) {
        this.fechaPedido = fechaPedido;
    }

    public LocalDateTime getFechaVerificacion() {
        return fechaVerificacion;
    }

    public void setFechaVerificacion(LocalDateTime fechaVerificacion) {
        this.fechaVerificacion = fechaVerificacion;
    }

    public Usuario getVerificadoPor() {
        return verificadoPor;
    }

    public void setVerificadoPor(Usuario verificadoPor) {
        this.verificadoPor = verificadoPor;
    }

    public Venta getVenta() {
        return venta;
    }

    public void setVenta(Venta venta) {
        this.venta = venta;
    }

    public String getPdfKey() {
        return pdfKey;
    }

    public void setPdfKey(String pdfKey) {
        this.pdfKey = pdfKey;
    }
}