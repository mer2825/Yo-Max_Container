package com.example.acceso.service;

import com.example.acceso.dto.EventoAuditoriaDTO;
import com.example.acceso.model.Empresa;
import com.example.acceso.model.SesionCaja;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender javaMailSender;
    private final EmpresaService empresaService;

    @Value("${spring.mail.from}")
    private String fromEmail;

    public EmailServiceImpl(JavaMailSender javaMailSender, EmpresaService empresaService) {
        this.javaMailSender = javaMailSender;
        this.empresaService = empresaService;
    }

    @Override
    public void enviarEmailAprobacion(String emailDestino, String numeroPedido) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(emailDestino);
            message.setFrom(fromEmail);
            message.setSubject("¡Tu pedido ha sido confirmado!");
            message.setText("Hola,\n\nTu pedido #" + numeroPedido + " ha sido aprobado exitosamente.\n" +
                          "Pronto nos comunicaremos contigo para coordinar la entrega.\n\n" +
                          "Gracias por tu compra.");

            javaMailSender.send(message);
            logger.info("Email de aprobación enviado a {} para pedido {}", emailDestino, numeroPedido);
        } catch (Exception e) {
            logger.error("Error al enviar email de aprobación a {}: {}", emailDestino, e.getMessage());
        }
    }

    @Override
    public void enviarEmailRechazo(String emailDestino, String numeroPedido, String motivo) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(emailDestino);
            message.setFrom(fromEmail);
            message.setSubject("Información sobre tu pedido #" + numeroPedido);
            message.setText("Hola,\n\nLamentamos informarte que tu pedido #" + numeroPedido +
                          " no pudo procesarse por el siguiente motivo:\n\n" +
                          motivo + "\n\n" +
                          "Por favor contáctanos si tienes alguna duda o deseas realizar el pedido nuevamente.\n\n" +
                          "Gracias por tu comprensión.");

            javaMailSender.send(message);
            logger.info("Email de rechazo enviado a {} para pedido {}", emailDestino, numeroPedido);
        } catch (Exception e) {
            logger.error("Error al enviar email de rechazo a {}: {}", emailDestino, e.getMessage());
        }
    }

    @Override
    public void enviarReporteCierreConAdjunto(String destinatario,
                                               SesionCaja sesion,
                                               Map<String, Object> detalle,
                                               List<EventoAuditoriaDTO> logAuditoria,
                                               ByteArrayInputStream pdfBytes) {
        if (destinatario == null || destinatario.isBlank()) {
            logger.warn("No se pudo enviar el reporte de cierre: el email de la empresa está vacío.");
            return;
        }

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            Empresa empresa = empresaService.getEmpresaInfo();
            String nombreEmpresa = (empresa != null && empresa.getNombre() != null) ? empresa.getNombre() : "Empresa";

            helper.setTo(destinatario);
            helper.setFrom(fromEmail);
            helper.setSubject("📊 Reporte de Cierre de Caja - " + nombreEmpresa);

            // Construir cuerpo HTML del email
            String cuerpoHtml = construirCuerpoHtml(sesion, detalle, nombreEmpresa);
            helper.setText(cuerpoHtml, true);

            // Adjuntar el PDF
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = pdfBytes.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            byte[] pdfData = baos.toByteArray();

            String fechaFormateada = sesion.getFechaCierre() != null
                    ? sesion.getFechaCierre().format(DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm"))
                    : "sin-fecha";

            helper.addAttachment("reporte-caja-" + sesion.getId() + "_" + fechaFormateada + ".pdf",
                    new ByteArrayResource(pdfData));

            // Enviar
            javaMailSender.send(message);
            logger.info("Reporte de cierre de caja #{} enviado exitosamente a {}", sesion.getId(), destinatario);

        } catch (MessagingException | IOException e) {
            logger.error("Error al enviar reporte de cierre de caja #{} a {}: {}", sesion.getId(), destinatario, e.getMessage());
        }
    }

    private String construirCuerpoHtml(SesionCaja sesion, Map<String, Object> detalle, String nombreEmpresa) {
        String fechaApertura = sesion.getFechaApertura() != null
                ? sesion.getFechaApertura().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "N/A";
        String fechaCierre = sesion.getFechaCierre() != null
                ? sesion.getFechaCierre().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "N/A";

        String usuarioApertura = sesion.getUsuarioApertura() != null ? sesion.getUsuarioApertura().getNombre() : "Sistema";
        String usuarioCierre = sesion.getUsuarioCierre() != null ? sesion.getUsuarioCierre().getNombre() : "Sistema";

        double montoInicial = sesion.getMontoInicial() != null ? sesion.getMontoInicial().doubleValue() : 0.0;
        double totalVentas = detalle != null && detalle.get("totalVentas") instanceof Number
                ? ((Number) detalle.get("totalVentas")).doubleValue() : 0.0;
        double efectivoEsperado = sesion.getMontoCierreEsperado() != null ? sesion.getMontoCierreEsperado().doubleValue() : 0.0;
        double efectivoDeclarado = sesion.getMontoCierreDeclarado() != null ? sesion.getMontoCierreDeclarado().doubleValue() : 0.0;
        double diferencia = sesion.getDiferencia() != null ? sesion.getDiferencia().doubleValue() : 0.0;
        String diferenciaColor = diferencia != 0 ? "#dc3545" : "#28a745";
        String diferenciaSigno = diferencia >= 0 ? "+" : "";

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>")
          .append("<style>")
          .append("body { font-family: Arial, sans-serif; background-color: #f8f9fa; margin: 0; padding: 20px; }")
          .append(".container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }")
          .append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; }")
          .append(".header h1 { margin: 0; font-size: 24px; }")
          .append(".header p { margin: 5px 0 0; opacity: 0.9; }")
          .append(".content { padding: 30px; }")
          .append(".info-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #eee; }")
          .append(".info-row .label { color: #6c757d; font-weight: bold; }")
          .append(".info-row .value { color: #212529; }")
          .append(".totals { margin-top: 20px; background-color: #f8f9fa; border-radius: 8px; padding: 20px; }")
          .append(".total-item { display: flex; justify-content: space-between; padding: 6px 0; }")
          .append(".total-item .label { color: #495057; }")
          .append(".total-item .value { font-weight: bold; }")
          .append(".diferencia { color: ").append(diferenciaColor).append("; font-weight: bold; font-size: 18px; }")
          .append(".footer { text-align: center; padding: 20px; color: #6c757d; font-size: 12px; border-top: 1px solid #eee; }")
          .append("</style></head><body>")
          .append("<div class='container'>")
          .append("<div class='header'>")
          .append("<h1>📊 Reporte de Cierre de Caja</h1>")
          .append("<p>").append(nombreEmpresa).append("</p>")
          .append("</div>")
          .append("<div class='content'>")
          .append("<h2 style='color: #333; margin-top: 0;'>Resumen de Sesión #").append(sesion.getId()).append("</h2>")
          .append("<div class='info-row'><span class='label'>Fecha Apertura</span><span class='value'>").append(fechaApertura).append("</span></div>")
          .append("<div class='info-row'><span class='label'>Fecha Cierre</span><span class='value'>").append(fechaCierre).append("</span></div>")
          .append("<div class='info-row'><span class='label'>Cajero Apertura</span><span class='value'>").append(usuarioApertura).append("</span></div>")
          .append("<div class='info-row'><span class='label'>Cajero Cierre</span><span class='value'>").append(usuarioCierre).append("</span></div>")
          .append("<div class='totals'>")
          .append("<h3 style='margin-top: 0; color: #333;'>Montos</h3>")
          .append("<div class='total-item'><span class='label'>Fondo Inicial</span><span class='value'>S/ ").append(String.format("%.2f", montoInicial)).append("</span></div>")
          .append("<div class='total-item'><span class='label'>Total Ventas</span><span class='value'>S/ ").append(String.format("%.2f", totalVentas)).append("</span></div>")
          .append("<div class='total-item'><span class='label'>Efectivo Esperado</span><span class='value'>S/ ").append(String.format("%.2f", efectivoEsperado)).append("</span></div>")
          .append("<div class='total-item'><span class='label'>Efectivo Declarado</span><span class='value'>S/ ").append(String.format("%.2f", efectivoDeclarado)).append("</span></div>")
          .append("<div class='total-item'><span class='label'>Diferencia</span><span class='value diferencia'>").append(diferenciaSigno).append(String.format("%.2f", diferencia)).append("</span></div>")
          .append("</div>")
          .append("<p style='margin-top: 20px; color: #495057;'>Se adjunta el PDF completo con el detalle de todos los movimientos de la sesión.</p>")
          .append("</div>")
          .append("<div class='footer'>")
          .append("<p>Este es un correo generado automáticamente por el sistema de caja.</p>")
          .append("<p>© ").append(java.time.LocalDate.now().getYear()).append(" ").append(nombreEmpresa).append("</p>")
          .append("</div></div></body></html>");

        return sb.toString();
    }
}