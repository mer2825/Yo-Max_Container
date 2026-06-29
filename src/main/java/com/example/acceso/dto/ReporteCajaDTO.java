package com.example.acceso.dto;

import com.example.acceso.model.SesionCaja;

import java.math.BigDecimal;
import java.util.List;

public class ReporteCajaDTO {

    private long totalSesiones;
    private long sesionesCerradas;
    private long sesionesConDiferencia;
    private BigDecimal totalDiferencias;
    private List<SesionCaja> sesiones;

    public ReporteCajaDTO() {
    }

    public ReporteCajaDTO(long totalSesiones, long sesionesCerradas,
                          long sesionesConDiferencia, BigDecimal totalDiferencias,
                          List<SesionCaja> sesiones) {
        this.totalSesiones = totalSesiones;
        this.sesionesCerradas = sesionesCerradas;
        this.sesionesConDiferencia = sesionesConDiferencia;
        this.totalDiferencias = totalDiferencias;
        this.sesiones = sesiones;
    }

    public long getTotalSesiones() {
        return totalSesiones;
    }

    public void setTotalSesiones(long totalSesiones) {
        this.totalSesiones = totalSesiones;
    }

    public long getSesionesCerradas() {
        return sesionesCerradas;
    }

    public void setSesionesCerradas(long sesionesCerradas) {
        this.sesionesCerradas = sesionesCerradas;
    }

    public long getSesionesConDiferencia() {
        return sesionesConDiferencia;
    }

    public void setSesionesConDiferencia(long sesionesConDiferencia) {
        this.sesionesConDiferencia = sesionesConDiferencia;
    }

    public BigDecimal getTotalDiferencias() {
        return totalDiferencias;
    }

    public void setTotalDiferencias(BigDecimal totalDiferencias) {
        this.totalDiferencias = totalDiferencias;
    }

    public List<SesionCaja> getSesiones() {
        return sesiones;
    }

    public void setSesiones(List<SesionCaja> sesiones) {
        this.sesiones = sesiones;
    }
}