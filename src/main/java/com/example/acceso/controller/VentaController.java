package com.example.acceso.controller;

import com.example.acceso.model.*;
import com.example.acceso.service.*;
import com.example.acceso.dto.ProductoMasVendidoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/ventas")
public class VentaController {

    private static final Logger logger = LoggerFactory.getLogger(VentaController.class);

    private final VentaService ventaService;
    private final ProductoService productoService;
    private final CategoriaService categoriaService;
    private final EmpresaService empresaService;
    private final CajaService cajaService;
    private final com.example.acceso.repository.VentaRepository ventaRepository;

    @Autowired
    public VentaController(VentaService ventaService, ProductoService productoService, CategoriaService categoriaService, EmpresaService empresaService, CajaService cajaService, com.example.acceso.repository.VentaRepository ventaRepository) {
        this.ventaService = ventaService;
        this.productoService = productoService;
        this.categoriaService = categoriaService;
        this.empresaService = empresaService;
        this.cajaService = cajaService;
        this.ventaRepository = ventaRepository;
    }

    @GetMapping("/listar")
    public String listarVentas() {
        return "ventas";
    }

    @GetMapping("/nueva")
    public String nuevaVenta(Model model) {
        List<Producto> productosActivos = productoService.listarProductosActivos();
        Map<Categoria, List<Producto>> productosPorCategoria = productosActivos.stream()
                .collect(Collectors.groupingBy(Producto::getCategoria));

        model.addAttribute("productosPorCategoria", productosPorCategoria);
        return "nueva-venta";
    }

    @GetMapping("/modificar/{id}")
    public String modificarVenta(@PathVariable Long id, Model model) {
        List<Categoria> categoriasActivas = categoriaService.listarCategoriasActivas();
        List<Producto> productosActivos = productoService.listarProductosActivos();
        Map<Categoria, List<Producto>> productosPorCategoria = categoriasActivas.stream()
                .collect(Collectors.toMap(
                        categoria -> categoria,
                        categoria -> productosActivos.stream()
                                .filter(p -> p.getCategoria() != null && p.getCategoria().getId().equals(categoria.getId()))
                                .collect(Collectors.toList())
                ));
        model.addAttribute("productosPorCategoria", productosPorCategoria);
        model.addAttribute("ventaId", id); // Pasar el ID de la venta a la vista
        return "modificar-venta";
    }

    @PostMapping("/api/guardar")
    @ResponseBody
    public ResponseEntity<?> guardarVenta(@RequestBody Venta venta) {
        Venta ventaCreada = null;
        try {
            ventaCreada = ventaService.crearVenta(venta);
            
            // Vincular la venta a la sesión de caja activa si existe
            Optional<com.example.acceso.model.SesionCaja> sesionActiva = cajaService.obtenerSesionActiva();
            if (sesionActiva.isPresent()) {
                ventaCreada.setSesionCaja(sesionActiva.get());
                ventaCreada = ventaRepository.save(ventaCreada); // Guardar la relación con la sesión de caja
            }
            
            Venta ventaProcesada = ventaService.procesarComprobanteElectronico(ventaCreada);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("ventaId", ventaProcesada.getId());
            response.put("numeroVenta", ventaProcesada.getNumeroVenta());
            response.put("estadoSunat", ventaProcesada.getEstadoSunat());
            response.put("message", "Venta registrada con éxito.");

            if ("aceptado".equalsIgnoreCase(ventaProcesada.getEstadoSunat())) {
                response.put("pdfUrl", ventaProcesada.getPdfUrl());
                response.put("xmlUrl", ventaProcesada.getXmlUrl());
            } else {
                response.put("errorMessage", ventaProcesada.getNota() != null ? ventaProcesada.getNota() : "Ocurrió un error al emitir el comprobante.");
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error al guardar venta", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error interno al procesar la venta: " + e.getMessage());
            if (ventaCreada != null) {
                response.put("ventaId", ventaCreada.getId());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/api/listar")
    @ResponseBody
    public ResponseEntity<?> listarVentasApi(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        List<Map<String, Object>> ventas;
        if (desde != null && hasta != null) {
            LocalDateTime fechaInicio = desde.atStartOfDay();
            LocalDateTime fechaFin = hasta.atTime(23, 59, 59);
            ventas = ventaService.buscarVentasPorRangoDeFechas(fechaInicio, fechaFin);
        } else {
            ventas = ventaService.listarTodasLasVentas();
        }
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", ventas);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarVenta(@PathVariable Long id) {
        try {
            ventaService.eliminarVenta(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Venta anulada con éxito.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al anular la venta: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/api/detalle/{id}")
    @ResponseBody
    public ResponseEntity<?> obtenerDetalleVenta(@PathVariable Long id) {
        return ventaService.obtenerVentaDetalladaPorId(id)
                .map(ventaMap -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("data", ventaMap);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Venta no encontrada");
                    return ResponseEntity.badRequest().body(response);
                });
    }

    @PutMapping("/api/actualizar/{id}")
    @ResponseBody
    public ResponseEntity<?> actualizarVenta(@PathVariable Long id, @RequestBody Venta venta) {
        try {
            ventaService.actualizarVenta(id, venta);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Venta actualizada con éxito.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al actualizar la venta: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/imprimir/{id}")
    public String imprimirBoleta(@PathVariable Long id, Model model) {
        return ventaService.obtenerVentaPorId(id)
                .map(venta -> {
                    model.addAttribute("venta", venta);
                    model.addAttribute("empresa", empresaService.getEmpresaInfo());
                    return "boleta";
                })
                .orElseGet(() -> {
                    model.addAttribute("errorMessage", "Venta no encontrada para imprimir.");
                    return "error";
                });
    }

    @GetMapping("/api/top5-productos-semana")
    @ResponseBody
    public ResponseEntity<List<ProductoMasVendidoDTO>> getTop5ProductosMasVendidosDeLaSemana() {
        List<ProductoMasVendidoDTO> topProductos = ventaService.obtenerTop5ProductosMasVendidosDeLaSemana();
        return ResponseEntity.ok(topProductos);
    }

    @GetMapping("/api/debug/ventas-hoy")
    @ResponseBody
    public ResponseEntity<?> debugVentasDeHoy() {
        // Devuelve ventas del día y conteo de detalles para diagnóstico
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDateTime inicioHoy = today.atStartOfDay();
        java.time.LocalDateTime finHoy = today.atTime(23, 59, 59);

        java.util.List<java.util.Map<String, Object>> ventas = ventaService.buscarVentasPorRangoDeFechas(inicioHoy, finHoy).stream().map(v -> {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", v.get("id"));
            m.put("numeroVenta", v.get("numeroVenta"));
            m.put("total", v.get("total"));
            // detalles list might not be directly present in map, return detalle count if available
            Object detalles = v.get("detalles");
            if (detalles instanceof java.util.List) {
                m.put("detallesCount", ((java.util.List) detalles).size());
            } else {
                m.put("detallesCount", "unknown");
            }
            return m;
        }).collect(java.util.stream.Collectors.toList());

        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("ventasHoy", ventas);
        resp.put("top5ProductosService", ventaService.obtenerProductosMasVendidosDeHoy());
        return ResponseEntity.ok(resp);
    }
}