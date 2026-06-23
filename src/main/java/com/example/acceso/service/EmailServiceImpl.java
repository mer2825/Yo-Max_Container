package com.example.acceso.service;

import com.example.acceso.model.Venta;
import com.example.acceso.repository.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailServiceImpl implements EmailService {

    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Value("${resend.from:onboarding@resend.dev}")
    private String senderEmail;

    @Value("${resend.test.recipient:}")
    private String resendTestRecipient;

    private static final String RESEND_API_URL = "https://api.resend.com/emails";
    private static final String SENDER_NAME = "Yo-Max Store";

    private final RestTemplate restTemplate;
    private final VentaRepository ventaRepository;

    @Autowired
    public EmailServiceImpl(RestTemplate restTemplate, VentaRepository ventaRepository) {
        this.restTemplate = restTemplate;
        this.ventaRepository = ventaRepository;
    }

    @Override
    public void enviarEmailAprobacion(String emailDestino, String numeroPedido) {
        System.out.println("[EMAIL - NO ENVIADO] Aprobación: " + emailDestino + " - Pedido: " + numeroPedido);
    }

    @Override
    public void enviarEmailRechazo(String emailDestino, String numeroPedido, String motivo) {
        System.out.println("[EMAIL - NO ENVIADO] Rechazo: " + emailDestino + " - Pedido: " + numeroPedido + " - Motivo: " + motivo);
    }

    @Override
    public void enviarEmailVenta(Long ventaId) throws Exception {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new Exception("Venta no encontrada con id: " + ventaId));

        if (venta.getCliente() == null || venta.getCliente().getEmail() == null || venta.getCliente().getEmail().isEmpty()) {
            throw new Exception("El cliente no tiene email registrado.");
        }

        String emailCliente = venta.getCliente().getEmail();
        String nombreCliente = venta.getCliente().getNombre();
        String numeroVenta = venta.getNumeroVenta();
        String tipoComprobante = venta.getTipoComprobante() != null ? venta.getTipoComprobante() : "Comprobante";

        // Construir el cuerpo del email
        String htmlContent = generarHtmlEmailVenta(nombreCliente, numeroVenta, tipoComprobante, venta);

        // Si no hay API key pero hay un destinatario de prueba configurado, simular el envío (útil para pruebas locales)
        if ((resendApiKey == null || resendApiKey.trim().isEmpty()) && resendTestRecipient != null && !resendTestRecipient.trim().isEmpty()) {
            System.out.println("[EMAIL - SIMULADO] Envío simulado a: " + resendTestRecipient + ", subject: Tu " + tipoComprobante + " #" + numeroVenta);
            System.out.println("[EMAIL - SIMULADO] HTML:\n" + htmlContent);
            return;
        }

        // Preparar el payload para Resend (forma simple: from, to, subject, html)
        Map<String, Object> emailPayload = new HashMap<>();
        emailPayload.put("from", senderEmail);
        // Si se configuró un destinatario de prueba, lo usamos para evitar errores en cuentas en modo testing
        if (resendTestRecipient != null && !resendTestRecipient.trim().isEmpty()) {
            emailPayload.put("to", resendTestRecipient.trim());
            System.out.println("[EMAIL - TEST OVERRIDE] Enviando correo de prueba a: " + resendTestRecipient);
        } else {
            emailPayload.put("to", emailCliente);
        }
        emailPayload.put("subject", "Tu " + tipoComprobante + " #" + numeroVenta + " de Yo-Max Store");
        emailPayload.put("html", htmlContent);

        // Preparar headers (Authorization Bearer)
        HttpHeaders headers = new HttpHeaders();
        if (resendApiKey == null || resendApiKey.trim().isEmpty()) {
            throw new Exception("API key de Resend no configurada. Configure 'resend.api.key' en application.properties o como variable de entorno.");
        }
        headers.set("Authorization", "Bearer " + resendApiKey);
        headers.set("Content-Type", "application/json");

        // Enviar el email
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(emailPayload, headers);
        try {
            restTemplate.postForObject(RESEND_API_URL, request, String.class);
            System.out.println("[EMAIL - ENVIADO] Venta: " + emailCliente + " - Número: " + numeroVenta);
        } catch (HttpClientErrorException e) {
            // Manejar errores de validación de Resend (p.ej. cuenta en modo testing)
            String body = e.getResponseBodyAsString();
            if (e.getStatusCode().value() == 403 && body != null && body.contains("You can only send testing emails")) {
                // Intentar extraer la dirección de email permitida que viene en el mensaje y reintentar el envío a esa dirección
                Pattern emailPattern = Pattern.compile("[\\w.%+-]+@[\\w.-]+\\.[A-Za-z]{2,}");
                Matcher matcher = emailPattern.matcher(body);
                if (matcher.find()) {
                    String allowedEmail = matcher.group();
                    // Si la dirección permitida es distinta del destinatario original, reintentar
                    if (!allowedEmail.equalsIgnoreCase(emailCliente)) {
                        try {
                            emailPayload.put("to", allowedEmail);
                            HttpEntity<Map<String, Object>> retryRequest = new HttpEntity<>(emailPayload, headers);
                            restTemplate.postForObject(RESEND_API_URL, retryRequest, String.class);
                            System.out.println("[EMAIL - ENVIADO (RETRY)] Venta: " + allowedEmail + " - Número: " + numeroVenta);
                            return; // envío exitoso al correo de prueba
                        } catch (Exception retryEx) {
                            throw new Exception("Resend: fallo al reintentar envío al correo de prueba " + allowedEmail + ": " + retryEx.getMessage(), retryEx);
                        }
                    }
                }
                throw new Exception("Resend: la cuenta está en modo testing. Solo puedes enviar emails de prueba a tu propia dirección. Respuesta de Resend: " + body + ". Para enviar a clientes, verifica un dominio en https://resend.com/domains y cambia el campo 'from' a una dirección de ese dominio.");
            }
            throw new Exception("Error al enviar email a través de Resend: " + e.getMessage() + " - Response: " + body, e);
        } catch (Exception e) {
            throw new Exception("Error al enviar email a través de Resend: " + e.getMessage(), e);
        }
    }

    private String generarHtmlEmailVenta(String nombreCliente, String numeroVenta, String tipoComprobante, Venta venta) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("<meta charset='UTF-8'>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; color: #333; }\n");
        html.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }\n");
        html.append(".header { background-color: #f8f9fa; padding: 20px; border-bottom: 3px solid #007bff; }\n");
        html.append(".content { padding: 20px; }\n");
        html.append(".footer { background-color: #f8f9fa; padding: 15px; text-align: center; font-size: 12px; color: #666; }\n");
        html.append(".table { width: 100%; border-collapse: collapse; margin: 20px 0; }\n");
        html.append(".table th { background-color: #007bff; color: white; padding: 10px; text-align: left; }\n");
        html.append(".table td { padding: 10px; border-bottom: 1px solid #ddd; }\n");
        html.append(".total { font-weight: bold; font-size: 18px; color: #007bff; }\n");
        html.append(".button { display: inline-block; background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 4px; margin-top: 20px; }\n");
        html.append("</style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("<div class='container'>\n");
        html.append("<div class='header'>\n");
        html.append("<h1 style='margin: 0; color: #007bff;'>Yo-Max Store</h1>\n");
        html.append("<p style='margin: 10px 0 0 0; color: #666;'>¡Gracias por tu compra!</p>\n");
        html.append("</div>\n");
        html.append("<div class='content'>\n");
        html.append("<p>Hola <strong>").append(nombreCliente).append("</strong>,</p>\n");
        html.append("<p>Te adjuntamos tu ").append(tipoComprobante.toLowerCase()).append(" de compra:</p>\n");
        html.append("<div style='background-color: #f0f0f0; padding: 15px; border-radius: 4px; margin: 20px 0;'>\n");
        html.append("<p style='margin: 0;'><strong>Número de ").append(tipoComprobante).append(":</strong> ").append(numeroVenta).append("</p>\n");
        html.append("<p style='margin: 10px 0 0 0;'><strong>Fecha:</strong> ").append(venta.getFechaVenta() != null ? venta.getFechaVenta().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A").append("</p>\n");
        html.append("<p style='margin: 10px 0 0 0; font-size: 20px; color: #007bff;'><strong>Total: S/ ").append(String.format("%.2f", venta.getTotal())).append("</strong></p>\n");
        html.append("</div>\n");
        html.append("<p style='margin-top: 30px; color: #666;'>Si tienes alguna pregunta, no dudes en contactarnos.</p>\n");
        html.append("<p style='color: #666;'><strong>Saludos,</strong><br>El equipo de Yo-Max Store</p>\n");
        html.append("</div>\n");
        html.append("<div class='footer'>\n");
        html.append("<p>&copy; 2026 Yo-Max Store. Todos los derechos reservados.</p>\n");
        html.append("</div>\n");
        html.append("</div>\n");
        html.append("</body>\n");
        html.append("</html>\n");
        return html.toString();
    }
}
