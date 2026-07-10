package com.example.acceso.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ReporteEgresosDTO {

    private int totalRegistros;
    private BigDecimal montoTotalEgresado;
    private int cantidadTiposEgreso;
    private List<Map<String, Object>> egresos;
    private Map<String, BigDecimal> egresosPorTipo;

    public ReporteEgresosDTO() {
    }

    public ReporteEgresosDTO(int totalRegistros, BigDecimal montoTotalEgresado,
                            int cantidadTiposEgreso,
                            List<Map<String, Object>> egresos,
                            Map<String, BigDecimal> egresosPorTipo) {
        this.totalRegistros = totalRegistros;
        this.montoTotalEgresado = montoTotalEgresado;
        this.cantidadTiposEgreso = cantidadTiposEgreso;
        this.egresos = egresos;
        this.egresosPorTipo = egresosPorTipo;
    }

    public int getTotalRegistros() {
        return totalRegistros;
    }

    public void setTotalRegistros(int totalRegistros) {
        this.totalRegistros = totalRegistros;
    }

    public BigDecimal getMontoTotalEgresado() {
        return montoTotalEgresado;
    }

    public void setMontoTotalEgresado(BigDecimal montoTotalEgresado) {
        this.montoTotalEgresado = montoTotalEgresado;
    }

    public int getCantidadTiposEgreso() {
        return cantidadTiposEgreso;
    }

    public void setCantidadTiposEgreso(int cantidadTiposEgreso) {
        this.cantidadTiposEgreso = cantidadTiposEgreso;
    }

    public List<Map<String, Object>> getEgresos() {
        return egresos;
    }

    public void setEgresos(List<Map<String, Object>> egresos) {
        this.egresos = egresos;
    }

    public Map<String, BigDecimal> getEgresosPorTipo() {
        return egresosPorTipo;
    }

    public void setEgresosPorTipo(Map<String, BigDecimal> egresosPorTipo) {
        this.egresosPorTipo = egresosPorTipo;
    }
}

