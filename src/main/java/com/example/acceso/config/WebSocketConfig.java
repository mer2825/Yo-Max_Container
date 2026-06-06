package com.example.acceso.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Habilita un "broker" de mensajes en memoria para enviar mensajes a los clientes
        // en destinos que comienzan con "/topic".
        config.enableSimpleBroker("/topic");
        
        // Define el prefijo para los mensajes que están destinados a ser manejados por
        // métodos en los controladores (anotados con @MessageMapping).
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Registra el endpoint "/ws" que los clientes usarán para conectarse al servidor WebSocket.
        // Permite orígenes desde cualquier host para que funcione correctamente en despliegues proxy como Railway.
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
