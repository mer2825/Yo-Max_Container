package com.example.acceso.service;

import com.example.acceso.dto.CambioProductoRequestDTO;
import com.example.acceso.dto.CambioProductoResponseDTO;
import com.example.acceso.model.Usuario;

public interface CambioProductoService {

    CambioProductoResponseDTO procesarCambio(CambioProductoRequestDTO request, Usuario usuarioActual);
}

