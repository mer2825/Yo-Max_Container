package com.example.acceso.service;

import com.example.acceso.dto.ReporteCompactoDTO;

import java.time.LocalDate;

/**
 * Servicio para generar el reporte Compacto de terminalización de caja
 * Muestra el flujo de dinero por día separado por método de pago
 */
public interface ReporteCompactoService {
    
    /**
     * Genera el reporte compacto de terminalización de caja
     * @param desde fecha de inicio del período
     * @param hasta fecha de fin del período
     * @return DTO con el reporte compacto
     */
    ReporteCompactoDTO generarReporteCompacto(LocalDate desde, LocalDate hasta);
}