package com.example.acceso.service;

import com.example.acceso.dto.ReporteCajaDTO;
import com.example.acceso.dto.ReporteComprobantesDTO;
import com.example.acceso.dto.ReporteInventarioDTO;
import com.example.acceso.dto.ReporteVentasDTO;
import com.example.acceso.model.DetalleVenta;
import com.example.acceso.model.NotasCredito;
import com.example.acceso.model.Producto;
import com.example.acceso.model.SesionCaja;
import com.example.acceso.model.Venta;
import com.example.acceso.repository.NotasCreditoRepository;
import com.example.acceso.repository.ProductoRepository;
import com.example.acceso.repository.SesionCajaRepository;
import com.example.acceso.repository.VentaRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ReporteService {

    private final VentaRepository ventaRepository;
    private final NotasCreditoRepository notaCreditoRepository;
    private final ProductoRepository productoRepository;
    private final SesionCajaRepository sesionCajaRepository;

    public ReporteService(VentaRepository ventaRepository,
                          NotasCreditoRepository notaCreditoRepository,
                          ProductoRepository productoRepository,
                          SesionCajaRepository sesionCajaRepository) {
        this.ventaRepository = ventaRepository;
        this.notaCreditoRepository = notaCreditoRepository;
        this.productoRepository = productoRepository;
        this.sesionCajaRepository = sesionCajaRepository;
    }

    // ── REPORTE DE VENTAS ──────────────────────────────────────

    public ReporteVentasDTO generarReporteVentas(
            LocalDate desde, LocalDate hasta) {

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin    = hasta.atTime(23, 59, 59);

        List<Venta> ventas = ventaRepository
            .findByFechaVentaBetween(inicio, fin);

        // KPIs principales
        BigDecimal totalVendido = ventas.stream()
            .map(Venta::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        int cantTransacciones = ventas.size();

        BigDecimal ticketPromedio = cantTransacciones == 0
            ? BigDecimal.ZERO
            : totalVendido.divide(
                BigDecimal.valueOf(cantTransacciones),
                2, RoundingMode.HALF_UP);

        // Por método de pago
        Map<String, BigDecimal> porMetodoPago = ventas.stream()
            .collect(Collectors.groupingBy(
                v -> v.getMetodoPago() != null ? v.getMetodoPago() : "Sin especificar",
                Collectors.reducing(BigDecimal.ZERO,
                    Venta::getTotal, BigDecimal::add)));

        // Por categoría de producto
        Map<String, BigDecimal> porCategoria = new LinkedHashMap<>();
        for (Venta v : ventas) {
            if (v.getDetalles() == null) continue;
            for (DetalleVenta d : v.getDetalles()) {
                String cat = d.getProducto() != null
                    && d.getProducto().getCategoria() != null
                    ? d.getProducto().getCategoria().getNombre()
                    : "Sin categoría";
                porCategoria.merge(cat, d.getSubtotal(), BigDecimal::add);
            }
        }

        // Ventas por día (para el gráfico de barras)
        Map<String, BigDecimal> porDia = ventas.stream()
            .collect(Collectors.groupingBy(
                v -> v.getFechaVenta().toLocalDate().toString(),
                Collectors.reducing(BigDecimal.ZERO,
                    Venta::getTotal, BigDecimal::add)));

        // Notas de crédito del período
        List<NotasCredito> ncs = notaCreditoRepository
            .findByFechaEmisionBetween(inicio, fin);
        BigDecimal totalNc = ncs.stream()
            .map(NotasCredito::getTotalAcreditado)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Top 5 productos más vendidos
        Map<String, Long> topProductos = ventas.stream()
            .flatMap(v -> v.getDetalles() != null
                ? v.getDetalles().stream() : Stream.empty())
            .collect(Collectors.groupingBy(
                d -> d.getProducto() != null
                    ? d.getProducto().getNombre() : "Desconocido",
                Collectors.summingLong(DetalleVenta::getCantidad)))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .collect(Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue,
                (e1, e2) -> e1, LinkedHashMap::new));

        return new ReporteVentasDTO(totalVendido, cantTransacciones,
            ticketPromedio, porMetodoPago, porCategoria,
            porDia, totalNc, ncs.size(), topProductos);
    }

    // ── REPORTE DE INVENTARIO ─────────────────────────────────

    public ReporteInventarioDTO generarReporteInventario() {
        // estado = 1 significa "activo" en el modelo Producto
        List<Producto> productos = productoRepository.findByEstado(1);

        // Valor total del inventario
        BigDecimal valorTotal = productos.stream()
            .map(p -> p.getPrecio()
                .multiply(BigDecimal.valueOf(p.getStock())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Productos bajo mínimo
        List<Producto> bajominimo = productos.stream()
            .filter(p -> p.getStock() <= p.getStockMinimo())
            .collect(Collectors.toList());

        // Productos sin movimiento en los últimos 30 días
        LocalDateTime hace30dias =
            LocalDateTime.now().minusDays(30);
        List<Long> idsConMovimiento = ventaRepository
            .findProductoIdsVendidosDespues(hace30dias);
        List<Producto> sinMovimiento = productos.stream()
            .filter(p -> !idsConMovimiento.contains(p.getId()))
            .collect(Collectors.toList());

        return new ReporteInventarioDTO(
            productos.size(), valorTotal,
            bajominimo, sinMovimiento);
    }

    // ── REPORTE DE COMPROBANTES SUNAT ─────────────────────────

    public ReporteComprobantesDTO generarReporteComprobantes(
            LocalDate desde, LocalDate hasta) {

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin    = hasta.atTime(23, 59, 59);

        List<Venta> ventas = ventaRepository
            .findByFechaVentaBetween(inicio, fin);

        long totalBoletas = ventas.stream()
            .filter(v -> "boleta".equalsIgnoreCase(v.getTipoComprobante())
                      || "03".equals(v.getTipoComprobante()))
            .count();

        BigDecimal montoBoletas = ventas.stream()
            .filter(v -> "boleta".equalsIgnoreCase(v.getTipoComprobante())
                      || "03".equals(v.getTipoComprobante()))
            .map(Venta::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalFacturas = ventas.stream()
            .filter(v -> "factura".equalsIgnoreCase(v.getTipoComprobante())
                      || "01".equals(v.getTipoComprobante()))
            .count();

        BigDecimal montoFacturas = ventas.stream()
            .filter(v -> "factura".equalsIgnoreCase(v.getTipoComprobante())
                      || "01".equals(v.getTipoComprobante()))
            .map(Venta::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalGeneral = montoBoletas.add(montoFacturas);

        // IGV generado (base imponible = total / 1.18, IGV = total - base)
        BigDecimal baseImponible = totalGeneral
            .divide(BigDecimal.valueOf(1.18), 2, RoundingMode.HALF_UP);
        BigDecimal igvGenerado = totalGeneral.subtract(baseImponible);

        // Notas de crédito
        List<NotasCredito> ncs = notaCreditoRepository
            .findByFechaEmisionBetween(inicio, fin);
        BigDecimal totalNc = ncs.stream()
            .map(NotasCredito::getTotalAcreditado)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ReporteComprobantesDTO(
            totalBoletas, montoBoletas,
            totalFacturas, montoFacturas,
            igvGenerado, baseImponible,
            ncs.size(), totalNc, ventas);
    }

    // ── REPORTE DE CAJA ───────────────────────────────────────

    public ReporteCajaDTO generarReporteCaja(
            LocalDate desde, LocalDate hasta) {

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin    = hasta.atTime(23, 59, 59);

        List<SesionCaja> sesiones = sesionCajaRepository
            .findByFechaAperturaBetweenOrderByFechaAperturaDesc(inicio, fin);

        long sesionesCerradas = sesiones.stream()
            .filter(s -> "CERRADA".equals(s.getEstado())).count();

        long sesionesConDiferencia = sesiones.stream()
            .filter(s -> s.getDiferencia() != null
                && s.getDiferencia().compareTo(BigDecimal.ZERO) != 0)
            .count();

        BigDecimal totalDiferencias = sesiones.stream()
            .filter(s -> s.getDiferencia() != null)
            .map(SesionCaja::getDiferencia)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ReporteCajaDTO(
            sesiones.size(), sesionesCerradas,
            sesionesConDiferencia, totalDiferencias, sesiones);
    }
}