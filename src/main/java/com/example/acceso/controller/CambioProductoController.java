package com.example.acceso.controller;

import com.example.acceso.dto.CambioProductoRequestDTO;
import com.example.acceso.dto.CambioProductoResponseDTO;
import com.example.acceso.model.Usuario;
import com.example.acceso.service.CambioProductoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ventas")
public class CambioProductoController {

    private final CambioProductoService cambioProductoService;

    @Autowired
    public CambioProductoController(CambioProductoService cambioProductoService) {
        this.cambioProductoService = cambioProductoService;
    }

    @PostMapping("/{ventaId}/cambios-producto")
    public CambioProductoResponseDTO cambiarProducto(
            @PathVariable Long ventaId,
            @Valid @RequestBody CambioProductoRequestDTO request,
            @AuthenticationPrincipal Usuario usuarioActual
    ) {
        request.setVentaOriginalId(ventaId);
        return cambioProductoService.procesarCambio(request, usuarioActual);
    }


}

