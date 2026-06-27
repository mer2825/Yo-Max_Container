package com.example.acceso.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ResumenDiaDTO {
    private LocalDate fecha;
    private BigDecimal totalVentas;
    private Long numeroTransacciones;
    private BigDecimal ticketPromedio;

    public ResumenDiaDTO() {
    }

    public ResumenDiaDTO(LocalDate fecha, BigDecimal totalVentas, Long numeroTransacciones, BigDecimal ticketPromedio) {
        this.fecha = fecha;
        this.totalVentas = totalVentas;
        this.numeroTransacciones = numeroTransacciones;
        this.ticketPromedio = ticketPromedio;
    }

    // Getters y Setters
    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getTotalVentas() {
        return totalVentas;
    }

    public void setTotalVentas(BigDecimal totalVentas) {
        this.totalVentas = totalVentas;
    }

    public Long getNumeroTransacciones() {
        return numeroTransacciones;
    }

    public void setNumeroTransacciones(Long numeroTransacciones) {
        this.numeroTransacciones = numeroTransacciones;
    }

    public BigDecimal getTicketPromedio() {
        return ticketPromedio;
    }

    public void setTicketPromedio(BigDecimal ticketPromedio) {
        this.ticketPromedio = ticketPromedio;
    }
}