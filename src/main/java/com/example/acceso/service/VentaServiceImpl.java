package com.example.acceso.service;

import com.example.acceso.model.Cliente;
import com.example.acceso.model.DetalleVenta;
import com.example.acceso.model.Producto;
import com.example.acceso.model.Venta;
import com.example.acceso.repository.ClienteRepository;
import com.example.acceso.repository.ProductoRepository;
import com.example.acceso.repository.VentaRepository;
import com.example.acceso.dto.ProductoMasVendidoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VentaServiceImpl implements VentaService {

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;

    @Autowired
    public VentaServiceImpl(VentaRepository ventaRepository, ProductoRepository productoRepository, ClienteRepository clienteRepository) {
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
        this.clienteRepository = clienteRepository;
    }

    @Override
    @Transactional
    public Venta crearVenta(Venta venta) {
        // Lógica para manejar el cliente en ventas web
        if ("web".equals(venta.getOrigen())) {
            // ...
        } else {
            // Lógica para ventas de punto de venta (POS)
            venta.setEstado(1); // Activa
        }

        // --- Lógica de cálculo y guardado ---
        BigDecimal subtotalCalculado = BigDecimal.ZERO;
        for (DetalleVenta detalle : venta.getDetalles()) {
            Producto producto = productoRepository.findById(detalle.getProducto().getId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + detalle.getProducto().getId()));
            
            detalle.setPrecioUnitario(producto.getPrecio());
            BigDecimal subtotalDetalle = producto.getPrecio().multiply(new BigDecimal(detalle.getCantidad()));
            detalle.setSubtotal(subtotalDetalle);
            subtotalCalculado = subtotalCalculado.add(subtotalDetalle);

            // Descontar stock solo para ventas que no son web (procesadas inmediatamente)
            if (!"web".equals(venta.getOrigen())) {
                int nuevoStock = producto.getStock() - detalle.getCantidad();
                if (nuevoStock < 0) {
                    throw new RuntimeException("Stock insuficiente para el producto: " + producto.getNombre());
                }
                producto.setStock(nuevoStock);
                productoRepository.save(producto);
            }
            detalle.setVenta(venta);
        }

        venta.setSubtotal(subtotalCalculado);

        // Validar y calcular descuento
        BigDecimal descuento = venta.getDescuento() != null ? venta.getDescuento() : BigDecimal.ZERO;
        if (descuento.compareTo(BigDecimal.ZERO) < 0) {
            descuento = BigDecimal.ZERO;
        }
        if (descuento.compareTo(subtotalCalculado) > 0) {
            throw new RuntimeException("El descuento no puede ser mayor que el subtotal.");
        }
        venta.setDescuento(descuento);

        // Calcular total final
        BigDecimal totalFinal = subtotalCalculado.subtract(descuento);
        venta.setTotal(totalFinal);

        // Generar número de venta y fecha
        String numeroVenta = generarNumeroVenta(venta.getTipoComprobante());
        venta.setNumeroVenta(numeroVenta);
        venta.setFechaVenta(LocalDateTime.now());

        return ventaRepository.save(venta);
    }

    private String generarNumeroVenta(String tipoComprobante) {
        String prefijo;
        switch (tipoComprobante) {
            case "Boleta":
                prefijo = "B";
                break;
            case "Factura":
                prefijo = "F";
                break;
            case "Nota de Venta":
                prefijo = "N";
                break;
            default:
                prefijo = "V";
        }

        DateTimeFormatter mesFormatter = DateTimeFormatter.ofPattern("MM");
        String mes = LocalDateTime.now().format(mesFormatter);
        String prefijoBusqueda = prefijo + mes + "-";

        Optional<Venta> ultimaVenta = ventaRepository.findTopByNumeroVentaStartingWithOrderByNumeroVentaDesc(prefijoBusqueda);

        int correlativo = 1;
        if (ultimaVenta.isPresent()) {
            String ultimoNumero = ultimaVenta.get().getNumeroVenta();
            try {
                String correlativoStr = ultimoNumero.substring(ultimoNumero.lastIndexOf('-') + 1);
                correlativo = Integer.parseInt(correlativoStr) + 1;
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                correlativo = 1;
            }
        }

        return String.format("%s%s-%04d", prefijo, mes, correlativo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Venta> obtenerVentaPorId(Long id) {
        return ventaRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarTodasLasVentas() {
        return ventaRepository.findAllByEstadoNot(2).stream()
                .map(this::convertVentaToMap)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarVentasActivas() {
        return ventaRepository.findByEstado(1).stream()
                .map(this::convertVentaToMap)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> buscarVentasPorRangoDeFechas(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return ventaRepository.findByFechaVentaBetween(fechaInicio, fechaFin).stream()
                .filter(venta -> venta.getEstado() != 2)
                .map(this::convertVentaToMap)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Optional<Venta> cambiarEstadoVenta(Long id) {
        return ventaRepository.findById(id).map(venta -> {
            if (venta.getEstado() == 1) {
                venta.setEstado(0);
            } else if (venta.getEstado() == 0) {
                venta.setEstado(1);
            }
            return ventaRepository.save(venta);
        });
    }

    @Override
    @Transactional
    public void eliminarVenta(Long id) {
        ventaRepository.findById(id).ifPresent(venta -> {
            // Si la venta no estaba ya eliminada (estado 2), procedemos a devolver stock
            if (venta.getEstado() != 2) {
                // Devolver stock de cada producto en la venta
                for (DetalleVenta detalle : venta.getDetalles()) {
                    Producto producto = detalle.getProducto();
                    if (producto != null) {
                        int nuevoStock = producto.getStock() + detalle.getCantidad();
                        producto.setStock(nuevoStock);
                        productoRepository.save(producto);
                    }
                }
                // Cambiar estado a eliminado/anulado
                venta.setEstado(2);
                ventaRepository.save(venta);
            }
        });
    }

    @Override
    @Transactional
    public void actualizarVenta(Long id, Venta ventaActualizada) {
        Venta ventaExistente = ventaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada con id: " + id));

        // Devolver stock de la venta original
        for (DetalleVenta detalleExistente : ventaExistente.getDetalles()) {
            Producto producto = detalleExistente.getProducto();
            producto.setStock(producto.getStock() + detalleExistente.getCantidad());
            productoRepository.save(producto);
        }

        // Actualizar datos maestros de la venta
        ventaExistente.setCliente(ventaActualizada.getCliente());
        ventaExistente.setMetodoPago(ventaActualizada.getMetodoPago());
        ventaExistente.setTipoComprobante(ventaActualizada.getTipoComprobante());
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String formattedDateTime = LocalDateTime.now().format(formatter);
        String nota = "COMPROBANTE MODIFICADO (" + formattedDateTime + ")";
        ventaExistente.setNota(nota);

        // Limpiar detalles antiguos y calcular nuevos
        ventaExistente.getDetalles().clear();
        BigDecimal subtotalCalculado = BigDecimal.ZERO;

        for (DetalleVenta detalleNuevo : ventaActualizada.getDetalles()) {
            Producto producto = productoRepository.findById(detalleNuevo.getProducto().getId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + detalleNuevo.getProducto().getId()));

            // Recalcular subtotal y descontar nuevo stock
            detalleNuevo.setPrecioUnitario(producto.getPrecio());
            BigDecimal subtotalDetalle = producto.getPrecio().multiply(new BigDecimal(detalleNuevo.getCantidad()));
            detalleNuevo.setSubtotal(subtotalDetalle);
            subtotalCalculado = subtotalCalculado.add(subtotalDetalle);

            int nuevoStock = producto.getStock() - detalleNuevo.getCantidad();
            if (nuevoStock < 0) {
                throw new RuntimeException("Stock insuficiente para el producto: " + producto.getNombre());
            }
            producto.setStock(nuevoStock);
            productoRepository.save(producto);

            detalleNuevo.setVenta(ventaExistente);
            ventaExistente.getDetalles().add(detalleNuevo);
        }

        ventaExistente.setSubtotal(subtotalCalculado);
        
        // Aplicar descuento
        BigDecimal descuento = ventaActualizada.getDescuento() != null ? ventaActualizada.getDescuento() : BigDecimal.ZERO;
        if (descuento.compareTo(BigDecimal.ZERO) < 0) {
            descuento = BigDecimal.ZERO;
        }
        if (descuento.compareTo(subtotalCalculado) > 0) {
            throw new RuntimeException("El descuento no puede ser mayor que el subtotal.");
        }
        ventaExistente.setDescuento(descuento);

        // Calcular total final
        BigDecimal totalFinal = subtotalCalculado.subtract(descuento);
        ventaExistente.setTotal(totalFinal);

        ventaRepository.save(ventaExistente);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> obtenerVentaDetalladaPorId(Long id) {
        return ventaRepository.findById(id).map(this::convertVentaToDetalleMap);
    }

    private Map<String, Object> convertVentaToMap(Venta venta) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", venta.getId());
        map.put("numeroVenta", venta.getNumeroVenta());

        if (venta.getCliente() != null) {
            map.put("nombreCliente", venta.getCliente().getNombre());
        } else {
            map.put("nombreCliente", "Cliente no disponible");
        }
        map.put("fechaVenta", venta.getFechaVenta() != null ? venta.getFechaVenta() : null);
        map.put("metodoPago", venta.getMetodoPago() != null ? venta.getMetodoPago() : "N/A");
        map.put("tipoComprobante", venta.getTipoComprobante() != null ? venta.getTipoComprobante() : "N/A");
        map.put("descuento", venta.getDescuento() != null ? venta.getDescuento() : BigDecimal.ZERO);
        map.put("total", venta.getTotal() != null ? venta.getTotal() : BigDecimal.ZERO);
        map.put("estado", venta.getEstado() != null ? venta.getEstado() : 1);
        map.put("nota", venta.getNota() != null ? venta.getNota() : "");
        return map;
    }

    private Map<String, Object> convertVentaToDetalleMap(Venta venta) {
        Map<String, Object> ventaMap = new HashMap<>();
        ventaMap.put("id", venta.getId());
        ventaMap.put("metodoPago", venta.getMetodoPago());
        ventaMap.put("tipoComprobante", venta.getTipoComprobante());

        if (venta.getCliente() != null) {
            Map<String, Object> clienteMap = new HashMap<>();
            clienteMap.put("id", venta.getCliente().getId());
            clienteMap.put("nombre", venta.getCliente().getNombre());
            clienteMap.put("tipoDocumento", venta.getCliente().getTipoDocumento());
            ventaMap.put("cliente", clienteMap);
        }

        List<Map<String, Object>> detallesList = venta.getDetalles().stream().map(detalle -> {
            Map<String, Object> detalleMap = new HashMap<>();
            detalleMap.put("cantidad", detalle.getCantidad());
            detalleMap.put("precioUnitario", detalle.getPrecioUnitario());

            if (detalle.getProducto() != null) {
                Map<String, Object> productoMap = new HashMap<>();
                productoMap.put("id", detalle.getProducto().getId());
                productoMap.put("nombre", detalle.getProducto().getNombre());
                productoMap.put("precio", detalle.getProducto().getPrecio());
                detalleMap.put("stock", detalle.getProducto().getStock());
                detalleMap.put("producto", productoMap);
            }
            return detalleMap;
        }).collect(Collectors.toList());

        ventaMap.put("detalles", detallesList);

        return ventaMap;
    }

    @Override
    public long obtenerNumeroVentasDiarias() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        return ventaRepository.countByFechaVenta(startOfDay, endOfDay);
    }

    @Override
    public BigDecimal obtenerTotalVentasDiarias() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        BigDecimal total = ventaRepository.sumTotalByFechaVenta(startOfDay, endOfDay);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    public long obtenerNumeroVentasMensuales() {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(LocalTime.MAX);
        return ventaRepository.countByFechaVentaMonth(startOfMonth, endOfMonth);
    }

    @Override
    public BigDecimal obtenerTotalVentasMensuales() {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(LocalTime.MAX);
        BigDecimal total = ventaRepository.sumTotalByFechaVentaMonth(startOfMonth, endOfMonth);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoMasVendidoDTO> obtenerTop5ProductosMasVendidosDeLaSemana() {
        LocalDate today = LocalDate.now();
        LocalDateTime inicioSemana = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime finSemana = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(LocalTime.MAX);

        List<Object[]> resultados = ventaRepository.findTop5ProductosMasVendidosDeLaSemana(inicioSemana, finSemana);

        return resultados.stream().map(resultado -> {
            String nombreProducto = (String) resultado[0];
            Long unidadesVendidas = ((Number) resultado[1]).longValue();
            BigDecimal totalDinero = (BigDecimal) resultado[2];
            return new ProductoMasVendidoDTO(nombreProducto, unidadesVendidas, totalDinero);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoMasVendidoDTO> obtenerProductosMasVendidosDeHoy() {
        LocalDate today = LocalDate.now();
        LocalDateTime inicioHoy = today.atStartOfDay();
        LocalDateTime finHoy = today.atTime(LocalTime.MAX);

        List<Venta> ventasHoy = ventaRepository.findByFechaVentaBetween(inicioHoy, finHoy).stream()
                .filter(v -> v.getEstado() == null || v.getEstado() != 2)
                .collect(Collectors.toList());

        Map<Long, ProductoMasVendidoDTO> acumulador = new HashMap<>();

        for (Venta venta : ventasHoy) {
            if (venta.getDetalles() == null) continue;
            for (DetalleVenta detalle : venta.getDetalles()) {
                if (detalle.getProducto() == null) continue;
                Long productoId = detalle.getProducto().getId();
                String nombre = detalle.getProducto().getNombre();
                Long unidades = (long) detalle.getCantidad();
                BigDecimal total = detalle.getPrecioUnitario() != null ? detalle.getPrecioUnitario().multiply(new BigDecimal(detalle.getCantidad())) : BigDecimal.ZERO;

                ProductoMasVendidoDTO dto = acumulador.get(productoId);
                if (dto == null) {
                    dto = new ProductoMasVendidoDTO(nombre, unidades, total);
                    acumulador.put(productoId, dto);
                } else {
                    dto.setUnidadesVendidas(dto.getUnidadesVendidas() + unidades);
                    dto.setTotalDinero(dto.getTotalDinero().add(total));
                }
            }
        }

        return acumulador.values().stream()
                .sorted((a, b) -> Long.compare(b.getUnidadesVendidas(), a.getUnidadesVendidas()))
                .limit(5)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarVentasWebPendientes() {
        return ventaRepository.findByEstado(3).stream()
                .map(this::convertVentaToMap)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void procesarVentaWeb(Long id) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada con id: " + id));

        if (venta.getEstado() != 3) {
            throw new RuntimeException("La venta no está pendiente de procesamiento.");
        }

        for (DetalleVenta detalle : venta.getDetalles()) {
            Producto producto = productoRepository.findById(detalle.getProducto().getId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + detalle.getProducto().getId()));
            int nuevoStock = producto.getStock() - detalle.getCantidad();
            if (nuevoStock < 0) {
                throw new RuntimeException("Stock insuficiente para el producto: " + producto.getNombre());
            }
            producto.setStock(nuevoStock);
            productoRepository.save(producto);
        }

        venta.setEstado(1);
        ventaRepository.save(venta);
    }
}
