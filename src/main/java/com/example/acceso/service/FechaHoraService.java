package com.example.acceso.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Servicio centralizado para manejo de fechas y horas.
 * Asegura que todas las operaciones usen la zona horaria de Perú (America/Lima).
 * Esto es crítico para despliegues en Railway que usan UTC por defecto.
 */
@Service
public class FechaHoraService {

    private static final ZoneId ZONA_PERU = ZoneId.of("America/Lima");

    /**
     * Obtiene la fecha y hora actual en la zona horaria de Perú.
     * Usar este método en lugar de LocalDateTime.now() para evitar
     * problemas de zona horaria en producción (Railway usa UTC).
     *
     * @return LocalDateTime con la fecha/hora actual en Perú
     */
    public LocalDateTime ahora() {
        return LocalDateTime.now(ZONA_PERU);
    }

    /**
     * Obtiene la fecha actual en la zona horaria de Perú.
     *
     * @return LocalDate con la fecha actual en Perú
     */
    public java.time.LocalDate hoy() {
        return java.time.LocalDate.now(ZONA_PERU);
    }

    /**
     * Obtiene la hora actual en la zona horaria de Perú.
     *
     * @return LocalTime con la hora actual en Perú
     */
    public java.time.LocalTime ahoraHora() {
        return java.time.LocalTime.now(ZONA_PERU);
    }

    /**
     * Obtiene la zona horaria configurada para Perú.
     *
     * @return ZoneId de America/Lima
     */
    public ZoneId getZonaHoraria() {
        return ZONA_PERU;
    }
}