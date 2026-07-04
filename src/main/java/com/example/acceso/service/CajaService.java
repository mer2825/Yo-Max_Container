package com.example.acceso.service;

import com.example.acceso.dto.CierreResumenDTO;
import com.example.acceso.dto.EventoAuditoriaDTO;
import com.example.acceso.dto.MovimientoLogDTO;
import com.example.acceso.dto.ReportePeriodoDTO;
import com.example.acceso.dto.ResumenSesionActivaDTO;
import com.example.acceso.model.SesionCaja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface CajaService {
    SesionCaja abrirCaja(BigDecimal montoInicial, Long usuarioId);
    SesionCaja cerrarCaja(BigDecimal montoDeclarado, String motivoDiferencia, String observaciones, Long usuarioId);
    SesionCaja registrarMovimiento(Long sesionId, String tipo, BigDecimal monto, String motivo, String categoria, Long usuarioId);
    ResumenSesionActivaDTO obtenerResumenSesionActiva();
    ReportePeriodoDTO obtenerReportePeriodo(LocalDate desde, LocalDate hasta);
    java.util.Optional<SesionCaja> obtenerSesionActiva();
    boolean haySesionActiva();
    java.util.List<SesionCaja> obtenerSesionesCerradasPorPeriodo(LocalDateTime inicio, LocalDateTime fin);
    java.util.List<SesionCaja> obtenerTodasLasSesionesCerradas();
    java.util.Optional<SesionCaja> obtenerSesionPorId(Long id);
    java.util.Map<String, Object> obtenerDetalleSesion(Long sesionId);
    java.util.List<EventoAuditoriaDTO> obtenerLogAuditoriaSesionActiva();
    java.util.List<EventoAuditoriaDTO> obtenerLogAuditoriaSesion(Long sesionId);
    java.util.List<SesionCaja> obtenerHistorialSesiones(LocalDate desde, LocalDate hasta, String filtroArqueo);
    java.util.List<MovimientoLogDTO> construirLogSesion(Long sesionId);
    java.util.List<MovimientoLogDTO> construirLogPorPeriodo(LocalDateTime desde, LocalDateTime hasta);

    CierreResumenDTO obtenerResumenParaCierre();

    java.math.BigDecimal obtenerSaldoParaApertura();
    boolean debeAlertarCierre(SesionCaja sesion);
    boolean haySesionDelDiaAnteriorSinCerrar();
    java.util.Optional<SesionCaja> obtenerUltimaSesionCerrada();
}
