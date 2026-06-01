package com.example.acceso.service;

import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    @Override
    public void enviarEmailAprobacion(String emailDestino, String numeroPedido) {
        // TODO: Implementar envío de email cuando se configure el servidor de correo
        // Requiere configuración en application.properties:
        // spring.mail.host=smtp.gmail.com
        // spring.mail.port=587
        // spring.mail.username=tu-email@gmail.com
        // spring.mail.password=tu-password
        // spring.mail.properties.mail.smtp.auth=true
        // spring.mail.properties.mail.smtp.starttls.enable=true
        
        // Ejemplo de implementación con JavaMailSender:
        /*
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(emailDestino);
        message.setSubject("¡Tu pedido ha sido confirmado!");
        message.setText("Hola,\n\nTu pedido #" + numeroPedido + " ha sido aprobado exitosamente.\n" +
                      "Pronto nos comunicaremos contigo para coordinar la entrega.\n\n" +
                      "Gracias por tu compra.");
        
        javaMailSender.send(message);
        */
        
        System.out.println("[EMAIL - NO ENVIADO] Aprobación: " + emailDestino + " - Pedido: " + numeroPedido);
    }

    @Override
    public void enviarEmailRechazo(String emailDestino, String numeroPedido, String motivo) {
        // TODO: Implementar envío de email cuando se configure el servidor de correo
        // Requiere configuración en application.properties (ver arriba)
        
        // Ejemplo de implementación con JavaMailSender:
        /*
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(emailDestino);
        message.setSubject("Información sobre tu pedido #" + numeroPedido);
        message.setText("Hola,\n\nLamentamos informarte que tu pedido #" + numeroPedido + 
                      " no pudo procesarse por el siguiente motivo:\n\n" +
                      motivo + "\n\n" +
                      "Por favor contáctanos si tienes alguna duda o deseas realizar el pedido nuevamente.\n\n" +
                      "Gracias por tu comprensión.");
        
        javaMailSender.send(message);
        */
        
        System.out.println("[EMAIL - NO ENVIADO] Rechazo: " + emailDestino + " - Pedido: " + numeroPedido + " - Motivo: " + motivo);
    }
}
