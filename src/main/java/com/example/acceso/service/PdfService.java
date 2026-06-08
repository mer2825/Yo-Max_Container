package com.example.acceso.service;

import com.example.acceso.model.PedidoWeb;

import java.io.ByteArrayInputStream;

public interface PdfService {
    ByteArrayInputStream generarEspecificacionCompra(PedidoWeb pedidoWeb);
}
