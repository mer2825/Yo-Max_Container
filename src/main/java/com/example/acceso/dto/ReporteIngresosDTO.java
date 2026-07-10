package com.example.acceso.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ReporteIngresosDTO {

    private int totalRegistros;
    private BigDecimal totalRecaudado;
    private BigDecimal totalComisionesOnline;
    private List<Map<String, Object>> ingresos;

    public ReporteIngresosDTO() {
    }

    public ReporteIngresosDTO(int totalRegistros, BigDecimal totalRecaudado,
                            BigDecimal totalComisionesOnline,
                            List<Map<String, Object>> ingresos) {
        this.totalRegistros = totalRegistros;
        this.totalRecaudado = totalRecaudado;
        this.totalComisionesOnline = totalComisionesOnline;
        this.ingresos = ingresos;
    }

    public int getTotalRegistros() {
        return totalRegistros;
    }

    public void setTotalRegistros(int totalRegistros) {
        this.totalRegistros = totalRegistros;
    }

    public BigDecimal getTotalRecaudado() {
        return totalRecaudado;
    }

    public void setTotalRecaudado(BigDecimal totalRecaudado) {
        this.totalRecaudado = totalRecaudado;
    }

    public BigDecimal getTotalComisionesOnline() {
        return totalComisionesOnline;
    }

    public void setTotalComisionesOnline(BigDecimal totalComisionesOnline) {
        this.totalComisionesOnline = totalComisionesOnline;
    }

    public List<Map<String, Object>> getIngresos() {
        return ingresos;
    }

    public void setIngresos(List<Map<String, Object>> ingresos) {
        this.ingresos = ingresos;
    }
}

