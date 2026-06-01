package com.example.acceso.service;

import com.example.acceso.model.VentaWeb;

import java.util.List;
import java.util.Optional;

public interface VentaWebService {
    VentaWeb guardarVentaWeb(VentaWeb ventaWeb);
    List<VentaWeb> listarTodasLasVentasWeb();
    void procesarVentaWeb(Long idVentaWeb);
    void eliminarVentaWeb(Long idVentaWeb);
    Optional<VentaWeb> obtenerVentaWebPorId(Long idVentaWeb);
    VentaWeb actualizarVentaWeb(Long id, VentaWeb ventaWebActualizada);
}
