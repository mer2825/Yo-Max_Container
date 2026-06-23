package com.example.acceso.controller;

import com.example.acceso.model.*;
import com.example.acceso.service.*;
import com.example.acceso.dto.ProductoMasVendidoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

@Controller
@RequestMapping("/ventas")
public class VentaController {

    private final VentaService ventaService;
    private final ProductoService productoService;
    private final CategoriaService categoriaService;
    private final EmpresaService empresaService;
    private final EmailService emailService;

    @Autowired
    public VentaController(VentaService ventaService, ProductoService productoService, CategoriaService categoriaService, EmpresaService empresaService, EmailService emailService) {
        this.ventaService = ventaService;
        this.productoService = productoService;
        this.categoriaService = categoriaService;
        this.empresaService = empresaService;
        this.emailService = emailService;
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
        try {
            Venta ventaCreada = ventaService.crearVenta(venta);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Venta registrada con éxito");
            response.put("ventaId", ventaCreada.getId());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
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

    @PostMapping("/api/enviar-sunat/{id}")
    @ResponseBody
    public ResponseEntity<?> enviarSunat(@PathVariable Long id) {
        return ventaService.obtenerVentaDetalladaPorId(id)
                .map(ventaMap -> {
                    try {
                        Map<String, Object> nf = new HashMap<>();
                        nf.put("operacion", "generar_comprobante");
                        String tipoCompStr = ventaMap.get("tipoComprobante") != null ? ventaMap.get("tipoComprobante").toString() : "";
                        int tipo_de_comprobante = 1; // 1 por defecto (factura)
                        if (tipoCompStr.toLowerCase().contains("boleta")) tipo_de_comprobante = 3;
                        else if (tipoCompStr.toLowerCase().contains("factura")) tipo_de_comprobante = 1;
                        nf.put("tipo_de_comprobante", tipo_de_comprobante);
                        nf.put("serie", "FFF1");
                        nf.put("numero", 1);
                        nf.put("sunat_transaction", 1);

                        Map<String, Object> clienteMap = (Map<String, Object>) ventaMap.get("cliente");
                        if (clienteMap != null) {
                            nf.put("cliente_tipo_de_documento", clienteMap.getOrDefault("tipoDocumento", 6));
                            nf.put("cliente_numero_de_documento", clienteMap.getOrDefault("id", ""));
                            nf.put("cliente_denominacion", clienteMap.getOrDefault("nombre", ""));
                        } else {
                            nf.put("cliente_tipo_de_documento", 6);
                            nf.put("cliente_numero_de_documento", "");
                            nf.put("cliente_denominacion", "");
                        }
                        nf.put("cliente_direccion", "");
                        nf.put("cliente_email", "");
                        nf.put("cliente_email_1", "");
                        nf.put("cliente_email_2", "");

                        nf.put("fecha_de_emision", java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                        nf.put("fecha_de_vencimiento", "");
                        nf.put("moneda", 1);
                        nf.put("tipo_de_cambio", "");
                        nf.put("porcentaje_de_igv", 18.00);
                        nf.put("descuento_global", "");
                        nf.put("total_descuento", "");
                        nf.put("total_anticipo", "");

                        List<Map<String, Object>> detalles = (List<Map<String, Object>>) ventaMap.get("detalles");
                        double total_gravada = 0.0;
                        double total_igv = 0.0;
                        double total = 0.0;
                        List<Map<String, Object>> items = new java.util.ArrayList<>();

                        if (detalles != null) {
                            for (Map<String, Object> d : detalles) {
                                double cantidad = ((Number) d.getOrDefault("cantidad", 0)).doubleValue();
                                Number precioNum = (Number) d.getOrDefault("precioUnitario", 0);
                                double valor_unitario = precioNum.doubleValue();
                                double subtotal = valor_unitario * cantidad;
                                double igv = subtotal * 0.18;
                                double totalItem = subtotal + igv;

                                Map<String, Object> item = new HashMap<>();
                                item.put("unidad_de_medida", "NIU");
                                Object productoObj = d.get("producto");
                                if (productoObj instanceof Map) {
                                    Map prod = (Map) productoObj;
                                    item.put("codigo", String.valueOf(prod.getOrDefault("id", "")));
                                    item.put("descripcion", prod.getOrDefault("nombre", "DETALLE DEL PRODUCTO"));
                                } else {
                                    item.put("codigo", "");
                                    item.put("descripcion", d.getOrDefault("descripcion", "DETALLE DEL PRODUCTO"));
                                }
                                item.put("codigo_producto_sunat", "10000000");
                                item.put("cantidad", (int) Math.round(cantidad));
                                item.put("valor_unitario", roundDouble(valor_unitario, 2));
                                item.put("precio_unitario", roundDouble(totalItem, 2));
                                item.put("descuento", "");
                                item.put("subtotal", roundDouble(subtotal, 2));
                                item.put("tipo_de_igv", 1);
                                item.put("igv", roundDouble(igv, 2));
                                item.put("total", roundDouble(totalItem, 2));
                                item.put("anticipo_regularizacion", false);
                                item.put("anticipo_documento_serie", "");
                                item.put("anticipo_documento_numero", "");

                                items.add(item);
                                total_gravada += subtotal;
                                total_igv += igv;
                                total += totalItem;
                            }
                        }

                        nf.put("total_gravada", roundDouble(total_gravada, 2));
                        nf.put("total_inafecta", "");
                        nf.put("total_exonerada", "");
                        nf.put("total_igv", roundDouble(total_igv, 2));
                        nf.put("total_gratuita", "");
                        nf.put("total_otros_cargos", "");
                        nf.put("total", roundDouble(total, 2));
                        nf.put("percepcion_tipo", "");
                        nf.put("percepcion_base_imponible", "");
                        nf.put("total_percepcion", "");
                        nf.put("total_incluido_percepcion", "");
                        nf.put("retencion_tipo", "");
                        nf.put("retencion_base_imponible", "");
                        nf.put("total_retencion", "");
                        nf.put("total_impuestos_bolsas", "");
                        nf.put("detraccion", false);
                        nf.put("observaciones", "");
                        nf.put("documento_que_se_modifica_tipo", "");
                        nf.put("documento_que_se_modifica_serie", "");
                        nf.put("documento_que_se_modifica_numero", "");
                        nf.put("tipo_de_nota_de_credito", "");
                        nf.put("tipo_de_nota_de_debito", "");
                        nf.put("enviar_automaticamente_a_la_sunat", true);
                        nf.put("enviar_automaticamente_al_cliente", false);
                        nf.put("condiciones_de_pago", "");
                        nf.put("medio_de_pago", "");
                        nf.put("placa_vehiculo", "");
                        nf.put("orden_compra_servicio", "");
                        nf.put("formato_de_pdf", "");
                        nf.put("generado_por_contingencia", "");
                        nf.put("bienes_region_selva", "");
                        nf.put("servicios_region_selva", "");

                        nf.put("items", items);
                        nf.put("guias", new java.util.ArrayList<>());
                        nf.put("venta_al_credito", new java.util.ArrayList<>());

                        ObjectMapper mapper = new ObjectMapper();
                        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(nf);

                        Path path = Paths.get(System.getProperty("user.dir"), "servicios", "NubeFact", "NFc.json");
                        Files.createDirectories(path.getParent());
                        Files.write(path, json.getBytes(StandardCharsets.UTF_8));

                        Map<String, Object> resp = new HashMap<>();
                        resp.put("success", true);
                        resp.put("message", "JSON generado y guardado en: " + path.toString());
                        resp.put("path", path.toString());
                        return ResponseEntity.ok(resp);
                    } catch (IOException e) {
                        Map<String, Object> resp = new HashMap<>();
                        resp.put("success", false);
                        resp.put("message", "Error al generar JSON: " + e.getMessage());
                        return ResponseEntity.badRequest().body(resp);
                    }
                })
                .orElseGet(() -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Venta no encontrada");
                    return ResponseEntity.badRequest().body(response);
                });
    }

    // Helper para redondear
    private static double roundDouble(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        long tmp = Math.round(value * factor);
        return (double) tmp / factor;
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

    @PostMapping("/api/enviar-email/{id}")
    @ResponseBody
    public ResponseEntity<?> enviarEmailVenta(@PathVariable Long id) {
        try {
            emailService.enviarEmailVenta(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Email enviado correctamente al cliente.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al enviar el email: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}