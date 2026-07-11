package com.example.acceso.service;

import com.example.acceso.dto.EventoAuditoriaDTO;
import com.example.acceso.model.SesionCaja;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

public interface EmailService {
    
    /**
     * Envía un email de confirmación cuando un pedido es aprobado
     * @param emailDestino Email del cliente
     * @param numeroPedido Número del pedido aprobado
     */
    void enviarEmailAprobacion(String emailDestino, String numeroPedido);
    
    /**
     * Envía un email de rechazo cuando un pedido es rechazado
     * @param emailDestino Email del cliente
     * @param numeroPedido Número del pedido rechazado
     * @param motivo Motivo del rechazo
     */
    void enviarEmailRechazo(String emailDestino, String numeroPedido, String motivo);

    /**
     * Envía por correo el reporte PDF de cierre de caja al email de la empresa
     * @param destinatario Email de la empresa (destinatario)
     * @param sesion La sesión de caja cerrada
     * @param detalle Mapa con el detalle de la sesión
     * @param logAuditoria Lista de eventos de auditoría
     * @param pdfBytes PDF generado del reporte de cierre
     */
    void enviarReporteCierreConAdjunto(String destinatario,
                                        SesionCaja sesion,
                                        Map<String, Object> detalle,
                                        List<EventoAuditoriaDTO> logAuditoria,
                                        ByteArrayInputStream pdfBytes);

    /**
     * Envía un email de confirmación de venta al cliente con el PDF de la especificación adjunto
     * @param emailDestino Email del cliente
     * @param numeroPedido Número del pedido aprobado
     * @param nombreCliente Nombre del cliente
     * @param pdfBytes PDF de la especificación de compra
     */
    void enviarEmailConfirmacionConPdf(String emailDestino, String numeroPedido, String nombreCliente, ByteArrayInputStream pdfBytes);

    /**
     * Envía un email de confirmación de venta al cliente con PDFs adjuntos (especificación + boleta)
     * @param emailDestino Email del cliente
     * @param numeroPedido Número del pedido aprobado
     * @param nombreCliente Nombre del cliente
     * @param especPdfBytes PDF de la especificación de compra
     * @param boletaPdfBytes PDF de la boleta/factura SUNAT (puede ser null)
     * @param serieCorrelativo Serie-Correlativo de la boleta/factura (puede ser null)
     */
    void enviarEmailConfirmacionConPdf(String emailDestino, String numeroPedido, String nombreCliente, 
                                       ByteArrayInputStream especPdfBytes, ByteArrayInputStream boletaPdfBytes, 
                                       String serieCorrelativo);
}
