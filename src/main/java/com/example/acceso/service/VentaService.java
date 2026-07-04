package com.example.acceso.service;

import com.example.acceso.model.Venta;
import com.example.acceso.dto.ProductoMasVendidoDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface VentaService {
    Venta crearVenta(Venta venta);
    Optional<Venta> obtenerVentaPorId(Long id);
    Optional<Venta> obtenerEntidadVentaPorId(Long id);
    List<Map<String, Object>> listarTodasLasVentas();
    List<Map<String, Object>> listarVentasActivas();
    List<Map<String, Object>> buscarVentasPorRangoDeFechas(LocalDateTime fechaInicio, LocalDateTime fechaFin);
    Optional<Venta> cambiarEstadoVenta(Long id);
    void eliminarVenta(Long id);
    void actualizarVenta(Long id, Venta ventaActualizada);

    /**
     * Aplica una nota de crédito sobre una venta existente SIN recalcular ni limpiar detalles.
     * Solo actualiza el estado de la nota de crédito y devuelve stock según corresponda (opcional).
     */
    void aplicarNotaCredito(Long ventaId, String estadoNotaCredito, List<com.example.acceso.dto.NotaCreditoItemDTO> itemsSeleccionados);
    Optional<Map<String, Object>> obtenerVentaDetalladaPorId(Long id);
    Venta procesarComprobanteElectronico(Venta venta);
    // Métodos para el Dashboard
    long obtenerNumeroVentasDiarias();
    BigDecimal obtenerTotalVentasDiarias();
    long obtenerNumeroVentasMensuales();
    BigDecimal obtenerTotalVentasMensuales();
    List<ProductoMasVendidoDTO> obtenerTop5ProductosMasVendidosDeLaSemana();
    List<ProductoMasVendidoDTO> obtenerProductosMasVendidosDeHoy();

    // Métodos para Ventas Web
    List<Map<String, Object>> listarVentasWebPendientes();
    void procesarVentaWeb(Long id);
    List<Map<String, Object>> listarVentasConNotasCredito();
}
