package com.example.acceso.service;

import com.example.acceso.dto.EventoAuditoriaDTO;
import com.example.acceso.model.Empresa;
import com.example.acceso.model.SesionCaja;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final RestTemplate restTemplate;
    private final EmpresaService empresaService;

    @Value("${mailersend.api.token}")
    private String apiToken;

    @Value("${mailersend.from.email}")
    private String fromEmail;

    @Value("${mailersend.from.name}")
    private String fromName;

    private static final String MAILERSEND_API_URL = "https://api.mailersend.com/v1/email";

    public EmailServiceImpl(EmpresaService empresaService) {
        this.restTemplate = new RestTemplate();
        this.empresaService = empresaService;
    }

    @Override
    public void enviarEmailAprobacion(String emailDestino, String numeroPedido) {
        String subject = "¡Tu pedido ha sido confirmado!";
        String text = "Hola,\n\nTu pedido #" + numeroPedido + " ha sido aprobado exitosamente.\n" +
                      "Pronto nos comunicaremos contigo para coordinar la entrega.\n\n" +
                      "Gracias por tu compra.";

        enviarEmailSimple(emailDestino, subject, text);
    }

    @Override
    public void enviarEmailRechazo(String emailDestino, String numeroPedido, String motivo) {
        String subject = "Información sobre tu pedido #" + numeroPedido;
        String text = "Hola,\n\nLamentamos informarte que tu pedido #" + numeroPedido +
                      " no pudo procesarse por el siguiente motivo:\n\n" +
                      motivo + "\n\n" +
                      "Por favor contáctanos si tienes alguna duda o deseas realizar el pedido nuevamente.\n\n" +
                      "Gracias por tu comprensión.";

        enviarEmailSimple(emailDestino, subject, text);
    }

    @Override
    public void enviarEmailConfirmacionConPdf(String emailDestino, String numeroPedido, String nombreCliente, ByteArrayInputStream pdfBytes) {
        Empresa empresa = empresaService.getEmpresaInfo();
        String nombreEmpresa = (empresa != null && empresa.getNombre() != null) ? empresa.getNombre() : "Empresa";

        String subject = "✅ ¡Tu pedido #" + numeroPedido + " ha sido aprobado - " + nombreEmpresa;
        String htmlBody = construirHtmlConfirmacion(nombreCliente, numeroPedido, nombreEmpresa, null);

        List<MailerSendAttachment> attachments = new ArrayList<>();
        if (pdfBytes != null) {
            attachments.add(crearAttachment(pdfBytes, "especificacion-compra-" + numeroPedido + ".pdf"));
        }

        enviarEmailConAdjuntos(emailDestino, subject, htmlBody, attachments);
    }

    @Override
    public void enviarEmailConfirmacionConPdf(String emailDestino, String numeroPedido, String nombreCliente,
                                               ByteArrayInputStream especPdfBytes, ByteArrayInputStream boletaPdfBytes,
                                               String serieCorrelativo) {
        Empresa empresa = empresaService.getEmpresaInfo();
        String nombreEmpresa = (empresa != null && empresa.getNombre() != null) ? empresa.getNombre() : "Empresa";

        String comprobanteInfo = (serieCorrelativo != null) ? " - Comp. " + serieCorrelativo : "";
        String subject = "✅ Pedido #" + numeroPedido + " aprobado" + comprobanteInfo + " - " + nombreEmpresa;
        String htmlBody = construirHtmlConfirmacionConBoleta(nombreCliente, numeroPedido, nombreEmpresa, serieCorrelativo);

        List<MailerSendAttachment> attachments = new ArrayList<>();
        if (especPdfBytes != null) {
            attachments.add(crearAttachment(especPdfBytes, "especificacion-compra-" + numeroPedido + ".pdf"));
        }
        if (boletaPdfBytes != null) {
            String nombreArchivo = (serieCorrelativo != null ? "comprobante-" + serieCorrelativo : "boleta-" + numeroPedido) + ".pdf";
            attachments.add(crearAttachment(boletaPdfBytes, nombreArchivo));
        }

        enviarEmailConAdjuntos(emailDestino, subject, htmlBody, attachments);
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

        Empresa empresa = empresaService.getEmpresaInfo();
        String nombreEmpresa = (empresa != null && empresa.getNombre() != null) ? empresa.getNombre() : "Empresa";

        String subject = "📊 Reporte de Cierre de Caja - " + nombreEmpresa;
        String htmlBody = construirCuerpoHtml(sesion, detalle, nombreEmpresa);

        List<MailerSendAttachment> attachments = new ArrayList<>();
        if (pdfBytes != null) {
            String fechaFormateada = sesion.getFechaCierre() != null
                    ? sesion.getFechaCierre().format(DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm"))
                    : "sin-fecha";
            attachments.add(crearAttachment(pdfBytes, "reporte-caja-" + sesion.getId() + "_" + fechaFormateada + ".pdf"));
        }

        enviarEmailConAdjuntos(destinatario, subject, htmlBody, attachments);
    }

    // ========================================================================
    // MÉTODOS PRIVADOS PARA CONSTRUIR Y ENVIAR PETICIONES A LA API DE MAILERSEND
    // ========================================================================

    /**
     * Envía un email simple (solo texto plano, sin HTML ni adjuntos)
     */
    private void enviarEmailSimple(String emailDestino, String subject, String text) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("from", Map.of("email", fromEmail, "name", fromName));
            payload.put("to", List.of(Map.of("email", emailDestino)));
            payload.put("subject", subject);
            payload.put("text", text);

            ejecutarPeticion(payload);
            logger.info("Email simple enviado a {}: {}", emailDestino, subject);
        } catch (Exception e) {
            logger.error("Error al enviar email simple a {}: {}", emailDestino, e.getMessage());
        }
    }

    /**
     * Envía un email con contenido HTML y adjuntos opcionales
     */
    private void enviarEmailConAdjuntos(String emailDestino, String subject, String htmlBody,
                                         List<MailerSendAttachment> attachments) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("from", Map.of("email", fromEmail, "name", fromName));
            payload.put("to", List.of(Map.of("email", emailDestino)));
            payload.put("subject", subject);
            payload.put("html", htmlBody);

            if (attachments != null && !attachments.isEmpty()) {
                List<Map<String, String>> attachList = new ArrayList<>();
                for (MailerSendAttachment att : attachments) {
                    attachList.add(Map.of(
                        "content", att.getContent(),
                        "filename", att.getFilename()
                    ));
                }
                payload.put("attachments", attachList);
            }

            ejecutarPeticion(payload);
            logger.info("Email con adjuntos enviado a {}: {}", emailDestino, subject);
        } catch (Exception e) {
            logger.error("Error al enviar email con adjuntos a {}: {}", emailDestino, e.getMessage());
        }
    }

    /**
     * Ejecuta la petición HTTP POST a la API de MailerSend
     */
    private void ejecutarPeticion(Map<String, Object> payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiToken);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    MAILERSEND_API_URL,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("MailerSend API respondió con status: {} - Email enviado exitosamente", response.getStatusCode());
            } else {
                logger.warn("MailerSend API respondió con status: {} - body: {}", response.getStatusCode(), response.getBody());
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            logger.error("Error HTTP {} al llamar a MailerSend API. Cuerpo de la respuesta: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            logger.error("Verifica que MAILERSEND_API_TOKEN sea un token de API REST de MailerSend (no la contraseña SMTP).");
            logger.error("Para obtener un token API: ve a https://app.mailersend.com/ → API Tokens → Generate new token");
        } catch (Exception e) {
            logger.error("Error inesperado al llamar a MailerSend API: {}", e.getMessage());
        }
    }

    /**
     * Convierte un ByteArrayInputStream a un attachment de MailerSend (base64)
     */
    private MailerSendAttachment crearAttachment(ByteArrayInputStream bais, String filename) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = bais.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            byte[] fileBytes = baos.toByteArray();
            String base64Content = Base64.getEncoder().encodeToString(fileBytes);
            return new MailerSendAttachment(base64Content, filename);
        } catch (IOException e) {
            logger.error("Error al leer bytes del PDF para adjuntar: {}", e.getMessage());
            throw new RuntimeException("Error al procesar attachment", e);
        }
    }

    // ========================================================================
    // CONSTRUCCIÓN DE HTML
    // ========================================================================

    private String construirHtmlConfirmacion(String nombreCliente, String numeroPedido,
                                              String nombreEmpresa, String serieCorrelativo) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><style>"
            + "body { font-family: Arial, sans-serif; background-color: #f8f9fa; margin: 0; padding: 20px; }"
            + ".container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }"
            + ".header { background: linear-gradient(135deg, #28a745 0%, #20c997 100%); color: white; padding: 30px; text-align: center; }"
            + ".header h1 { margin: 0; font-size: 24px; }"
            + ".content { padding: 30px; }"
            + ".content p { color: #495057; line-height: 1.6; }"
            + ".pedido-number { font-size: 1.5rem; font-weight: bold; color: #28a745; text-align: center; margin: 20px 0; }"
            + ".footer { text-align: center; padding: 20px; color: #6c757d; font-size: 12px; border-top: 1px solid #eee; }"
            + "</style></head><body>"
            + "<div class='container'>"
            + "<div class='header'><h1>✅ ¡Pedido Aprobado!</h1></div>"
            + "<div class='content'>"
            + "<p>Hola <strong>" + nombreCliente + "</strong>,</p>"
            + "<p>¡Tu pedido ha sido aprobado exitosamente!</p>"
            + "<div class='pedido-number'>Pedido #" + numeroPedido + "</div>"
            + "<p>Adjuntamos la especificación de compra con los detalles de tu pedido.</p>"
            + "<p>Si tienes alguna pregunta, no dudes en contactarnos.</p>"
            + "<p>¡Gracias por tu compra!</p>"
            + "</div>"
            + "<div class='footer'>"
            + "<p>© " + java.time.LocalDate.now().getYear() + " " + nombreEmpresa + "</p>"
            + "</div></div></body></html>";
    }

    private String construirHtmlConfirmacionConBoleta(String nombreCliente, String numeroPedido,
                                                       String nombreEmpresa, String serieCorrelativo) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><style>"
            + "body { font-family: Arial, sans-serif; background-color: #f8f9fa; margin: 0; padding: 20px; }"
            + ".container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }"
            + ".header { background: linear-gradient(135deg, #28a745 0%, #20c997 100%); color: white; padding: 30px; text-align: center; }"
            + ".header h1 { margin: 0; font-size: 24px; }"
            + ".content { padding: 30px; }"
            + ".content p { color: #495057; line-height: 1.6; }"
            + ".pedido-number { font-size: 1.5rem; font-weight: bold; color: #28a745; text-align: center; margin: 20px 0; }"
            + ".comprobante-info { background: #f0fdf4; border: 1px solid #28a745; border-radius: 8px; padding: 12px; margin: 15px 0; text-align: center; }"
            + ".comprobante-info span { color: #155724; font-size: 0.9rem; }"
            + ".footer { text-align: center; padding: 20px; color: #6c757d; font-size: 12px; border-top: 1px solid #eee; }"
            + "</style></head><body>"
            + "<div class='container'>"
            + "<div class='header'><h1>✅ ¡Pedido Aprobado!</h1></div>"
            + "<div class='content'>"
            + "<p>Hola <strong>" + nombreCliente + "</strong>,</p>"
            + "<p>¡Tu pedido ha sido aprobado y el comprobante de pago ha sido emitido exitosamente!</p>"
            + "<div class='pedido-number'>Pedido #" + numeroPedido + "</div>"
            + (serieCorrelativo != null ? "<div class='comprobante-info'><span>🧾 Comprobante: <strong>" + serieCorrelativo + "</strong></span></div>" : "")
            + "<p>Adjuntamos los siguientes documentos:</p>"
            + "<ul>"
            + "<li><strong>Especificación de compra</strong> — detalle de los productos adquiridos</li>"
            + "<li><strong>Boleta/Factura electrónica</strong> — comprobante oficial emitido</li>"
            + "</ul>"
            + "<p>Si tienes alguna pregunta, no dudes en contactarnos.</p>"
            + "<p>¡Gracias por tu compra!</p>"
            + "</div>"
            + "<div class='footer'>"
            + "<p>© " + java.time.LocalDate.now().getYear() + " " + nombreEmpresa + "</p>"
            + "</div></div></body></html>";
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

    // ========================================================================
    // CLASE INTERNA PARA REPRESENTAR UN ATTACHMENT DE MAILERSEND
    // ========================================================================

    private static class MailerSendAttachment {
        private final String content;
        private final String filename;

        public MailerSendAttachment(String content, String filename) {
            this.content = content;
            this.filename = filename;
        }

        public String getContent() {
            return content;
        }

        public String getFilename() {
            return filename;
        }
    }
}