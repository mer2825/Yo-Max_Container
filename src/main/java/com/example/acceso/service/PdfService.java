package com.example.acceso.service;

import com.example.acceso.dto.EventoAuditoriaDTO;
import com.example.acceso.model.PedidoWeb;
import com.example.acceso.model.SesionCaja;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

public interface PdfService {
    ByteArrayInputStream generarEspecificacionCompra(PedidoWeb pedidoWeb);
    ByteArrayInputStream generarReporteSesionCaja(SesionCaja sesion, Map<String, Object> detalle, List<EventoAuditoriaDTO> logAuditoria);
}
