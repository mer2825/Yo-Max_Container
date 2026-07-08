package com.example.acceso.scheduler;

import com.example.acceso.service.CajaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CajaScheduler {

    private final CajaService cajaService;
    private static final Logger logger =
        LoggerFactory.getLogger(CajaScheduler.class);

    public CajaScheduler(CajaService cajaService) {
        this.cajaService = cajaService;
    }

    // Corre a las 8:00 PM todos los días
    @Scheduled(cron = "0 0 20 * * *")
    public void alertarCierrePendiente() {
        cajaService.obtenerSesionActiva().ifPresent(sesion -> {
            logger.warn(
                "CAJA: Sesión {} lleva abierta desde {} sin cerrarse. "
                + "El cajero debe realizar el cierre.",
                sesion.getId(),
                sesion.getFechaApertura()
            );
            // Aquí en el futuro se podría enviar un email o notificación push
        });
    }

}