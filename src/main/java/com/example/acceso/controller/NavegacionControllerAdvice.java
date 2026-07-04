package com.example.acceso.controller;

import com.example.acceso.service.CajaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class NavegacionControllerAdvice {

    private final CajaService cajaService;

    @Autowired
    public NavegacionControllerAdvice(CajaService cajaService) {
        this.cajaService = cajaService;
    }

    @ModelAttribute("sesionCajaAbierta")
    public boolean verificarSesionCajaAbierta() {
        return cajaService.obtenerSesionActiva().isPresent();
    }
}