package com.example.acceso.service;

import com.example.acceso.dto.ReporteCompactoDTO;
import com.example.acceso.model.Venta;
import com.example.acceso.repository.VentaRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación del servicio para generar el reporte Compacto de terminalización de caja
 */
@Service
public class ReporteCompactoServiceImpl implements ReporteCompactoService {

    private final VentaRepository ventaRepository;

    public ReporteCompactoServiceImpl(VentaRepository ventaRepository) {
        this.ventaRepository = ventaRepository;
    }

    @Override
    public ReporteCompactoDTO generarReporteCompacto(LocalDate desde, LocalDate hasta) {
        // Validar fechas
        if (desde == null) desde = LocalDate.now().minusDays(30);
        if (hasta == null) hasta = LocalDate.now();
        if (desde.isAfter(hasta)) {
            LocalDate temp = desde;
            desde = hasta;
            hasta = temp;
        }

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.atTime(23, 59, 59);

        // Obtener ventas del período (solo activas, estado != 2)
        List<Venta> ventas = ventaRepository.findByFechaVentaBetween(inicio, fin);
        ventas = ventas.stream()
                .filter(v -> v.getEstado() != 2)
                .collect(Collectors.toList());

        // Calcular totales generales
        BigDecimal totalGeneral = ventas.stream()
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalEfectivo = ventas.stream()
                .filter(v -> esEfectivo(v.getMetodoPago()))
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalYape = ventas.stream()
                .filter(v -> esYape(v.getMetodoPago()))
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOtros = totalGeneral.subtract(totalEfectivo).subtract(totalYape);

        // Calcular total de días únicos con ventas
        Set<LocalDate> diasConVentas = ventas.stream()
                .map(v -> v.getFechaVenta().toLocalDate())
                .collect(Collectors.toSet());

        int totalDias = Math.max(diasConVentas.size(), 1);
        int totalVentas = ventas.size();

        // Generar detalle por día
        List<Map<String, Object>> detallePorDia = generarDetallePorDia(ventas, desde, hasta);
        
        // Generar detalle específico de ventas en efectivo
        List<Map<String, Object>> ventasEfectivo = generarDetalleVentas(ventas, "efectivo");
        
        // Generar detalle específico de ventas en Yape
        List<Map<String, Object>> ventasYape = generarDetalleVentas(ventas, "yape");

        return new ReporteCompactoDTO(
                desde, hasta, totalDias, totalVentas,
                totalGeneral, totalEfectivo, totalYape, totalOtros,
                detallePorDia, ventasEfectivo, ventasYape
        );
    }

    /**
     * Genera el detalle de ventas agrupado por día y método de pago
     */
    private List<Map<String, Object>> generarDetallePorDia(List<Venta> ventas, LocalDate desde, LocalDate hasta) {
        List<Map<String, Object>> detalle = new ArrayList<>();

        // Agrupar ventas por fecha
        Map<LocalDate, List<Venta>> ventasPorDia = ventas.stream()
                .collect(Collectors.groupingBy(v -> v.getFechaVenta().toLocalDate()));

        // Iterar por cada día en el rango
        LocalDate fechaActual = desde;
        while (!fechaActual.isAfter(hasta)) {
            List<Venta> ventasDelDia = ventasPorDia.getOrDefault(fechaActual, Collections.emptyList());

            // Calcular totales por método de pago para este día
            BigDecimal efectivo = ventasDelDia.stream()
                    .filter(v -> esEfectivo(v.getMetodoPago()))
                    .map(Venta::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal yape = ventasDelDia.stream()
                    .filter(v -> esYape(v.getMetodoPago()))
                    .map(Venta::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal otros = ventasDelDia.stream()
                    .filter(v -> !esEfectivo(v.getMetodoPago()) && !esYape(v.getMetodoPago()))
                    .map(Venta::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalDia = efectivo.add(yape).add(otros);

            // Solo agregar días con ventas o si es el último día del rango
            if (totalDia.compareTo(BigDecimal.ZERO) > 0 || fechaActual.equals(hasta)) {
                Map<String, Object> dia = new LinkedHashMap<>();
                dia.put("fecha", fechaActual);
                dia.put("efectivo", efectivo);
                dia.put("yape", yape);
                dia.put("otros", otros);
                dia.put("total", totalDia);
                dia.put("cantidadVentas", ventasDelDia.size());

                detalle.add(dia);
            }

            fechaActual = fechaActual.plusDays(1);
        }

        // Ordenar por fecha descendente (más reciente primero)
        detalle.sort((a, b) -> ((LocalDate) b.get("fecha")).compareTo((LocalDate) a.get("fecha")));

        return detalle;
    }

    /**
     * Determina si un método de pago es efectivo
     */
    private boolean esEfectivo(String metodoPago) {
        if (metodoPago == null) return false;
        String metodo = metodoPago.toLowerCase().trim();
        return metodo.contains("efectivo") || metodo.contains("cash") ||
               metodo.equals("ef") || metodo.equals("contado");
    }

    /**
     * Determina si un método de pago es Yape
     */
    private boolean esYape(String metodoPago) {
        if (metodoPago == null) return false;
        String metodo = metodoPago.toLowerCase().trim();
        return metodo.contains("yape") || metodo.contains("yapeplin");
    }
    
    /**
     * Genera detalle de ventas filtrado por método de pago
     */
    private List<Map<String, Object>> generarDetalleVentas(List<Venta> ventas, String tipoPago) {
        List<Venta> ventasFiltradas = ventas.stream()
                .filter(v -> {
                    if ("efectivo".equals(tipoPago)) {
                        return esEfectivo(v.getMetodoPago());
                    } else if ("yape".equals(tipoPago)) {
                        return esYape(v.getMetodoPago());
                    }
                    return false;
                })
                .collect(Collectors.toList());
        
        return ventasFiltradas.stream()
                .map(v -> {
                    Map<String, Object> detalle = new LinkedHashMap<>();
                    detalle.put("fecha", v.getFechaVenta());
                    detalle.put("numeroVenta", v.getNumeroVenta());
                    detalle.put("cliente", v.getCliente() != null ? v.getCliente().getNombre() : "Sin cliente");
                    detalle.put("metodoPago", v.getMetodoPago());
                    detalle.put("subtotal", v.getSubtotal());
                    detalle.put("descuento", v.getDescuento());
                    detalle.put("total", v.getTotal());
                    detalle.put("comprobante", v.getTipoComprobante());
                    detalle.put("serieCorrelativo", v.getSerieCorrelativo());
                    detalle.put("cajero", v.getSesionCaja() != null && v.getSesionCaja().getUsuarioApertura() != null ? 
                                 v.getSesionCaja().getUsuarioApertura().getNombre() : "Sin cajero");
                    return detalle;
                })
                .collect(Collectors.toList());
    }
}
