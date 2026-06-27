package com.example.acceso.controller;

import com.example.acceso.model.Producto;
import com.example.acceso.service.ProductoService;
import com.example.acceso.dto.MovimientoProductoDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class InventarioController {

    private final ProductoService productoService;

    public InventarioController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping("/inventario/listar")
    public String listarInventario(Model model) {
        List<Producto> productos = productoService.listarProductos();
        model.addAttribute("productos", productos);
        model.addAttribute("activeUri", "/inventario/listar");
        return "inventario";
    }

    @GetMapping("/inventario/api/movimientos/{id}")
    @ResponseBody
    public ResponseEntity<?> getMovimientosProducto(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<MovimientoProductoDTO> movimientos = productoService.getMovimientosByProductId(id);
            response.put("success", true);
            response.put("data", movimientos);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al obtener movimientos del producto: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/inventario/api/ajustar-stock/{id}")
    @ResponseBody
    public ResponseEntity<?> ajustarStockProducto(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            Integer stock = payload.get("stock") != null ? ((Number) payload.get("stock")).intValue() : null;
            Integer stockMinimo = payload.get("stockMinimo") != null ? ((Number) payload.get("stockMinimo")).intValue() : null;

            if (stock == null) {
                response.put("success", false);
                response.put("message", "El valor de stock es obligatorio.");
                return ResponseEntity.badRequest().body(response);
            }

            return productoService.obtenerProductoPorId(id)
                    .map(producto -> {
                        Integer oldStock = producto.getStock();
                        producto.setStock(stock);
                        if (stockMinimo != null) {
                            producto.setStockMinimo(stockMinimo);
                        }
                        Producto productoActualizado = productoService.guardarProducto(producto);

                        // Registrar movimiento de ajuste de stock
                        try {
                            productoService.registrarAjusteStock(productoActualizado.getId(), productoActualizado.getStock(), "Ajuste manual desde interfaz");
                        } catch (Exception ex) {
                            // no interrumpir la respuesta si falla el registro de movimiento
                            System.err.println("No se pudo registrar movimiento de stock: " + ex.getMessage());
                        }

                        response.put("success", true);
                        response.put("message", "Stock actualizado correctamente.");
                        response.put("producto", productoActualizado);
                        return ResponseEntity.ok(response);
                    })
                    .orElseGet(() -> {
                        response.put("success", false);
                        response.put("message", "Producto no encontrado.");
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                    });
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al ajustar stock: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/inventario/api/registrar-movimiento")
    @ResponseBody
    public ResponseEntity<?> registrarMovimiento(@RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long productoId = payload.get("productoId") != null ? ((Number) payload.get("productoId")).longValue() : null;
            String tipoMovimiento = (String) payload.get("tipoMovimiento");
            Integer cantidad = payload.get("cantidad") != null ? ((Number) payload.get("cantidad")).intValue() : null;
            String motivo = (String) payload.get("motivo");
            String referenciaDocumento = (String) payload.get("referenciaDocumento");
            String proveedor = (String) payload.get("proveedor");
            String observacion = (String) payload.get("observacion");
            Integer stockAnterior = payload.get("stockAnterior") != null ? ((Number) payload.get("stockAnterior")).intValue() : null;
            Integer stockResultante = payload.get("stockResultante") != null ? ((Number) payload.get("stockResultante")).intValue() : null;
            Integer stockMinimo = payload.get("stockMinimo") != null ? ((Number) payload.get("stockMinimo")).intValue() : null;

            if (productoId == null || tipoMovimiento == null || cantidad == null) {
                response.put("success", false);
                response.put("message", "Faltan datos obligatorios.");
                return ResponseEntity.badRequest().body(response);
            }

            return productoService.obtenerProductoPorId(productoId)
                    .map(producto -> {
                        // Validar stock actual para salidas
                        if ("SALIDA".equals(tipoMovimiento)) {
                            Integer stockActual = producto.getStock();
                            if (cantidad > stockActual) {
                                response.put("success", false);
                                response.put("message", "Solo hay " + stockActual + " unidades disponibles. No se puede retirar " + cantidad + ".");
                                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
                            }
                        }

                        // Actualizar stock del producto
                        producto.setStock(stockResultante);
                        if (stockMinimo != null) {
                            producto.setStockMinimo(stockMinimo);
                        }
                        Producto productoActualizado = productoService.guardarProducto(producto);

                        // Registrar movimiento con todos los campos
                        productoService.registrarMovimientoConDetalles(
                                productoId,
                                cantidad,
                                tipoMovimiento,
                                motivo,
                                referenciaDocumento,
                                proveedor,
                                observacion,
                                stockAnterior,
                                stockResultante
                        );

                        response.put("success", true);
                        response.put("message", "Movimiento registrado correctamente.");
                        response.put("producto", productoActualizado);
                        return ResponseEntity.ok(response);
                    })
                    .orElseGet(() -> {
                        response.put("success", false);
                        response.put("message", "Producto no encontrado.");
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                    });
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al registrar movimiento: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/inventario/api/actualizar-stock-minimo/{id}")
    @ResponseBody
    public ResponseEntity<?> actualizarStockMinimo(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            Integer stockMinimo = payload.get("stockMinimo") != null ? ((Number) payload.get("stockMinimo")).intValue() : null;

            if (stockMinimo == null) {
                response.put("success", false);
                response.put("message", "El valor de stock mínimo es obligatorio.");
                return ResponseEntity.badRequest().body(response);
            }

            return productoService.obtenerProductoPorId(id)
                    .map(producto -> {
                        producto.setStockMinimo(stockMinimo);
                        Producto productoActualizado = productoService.guardarProducto(producto);

                        response.put("success", true);
                        response.put("message", "Stock mínimo actualizado correctamente.");
                        response.put("producto", productoActualizado);
                        return ResponseEntity.ok(response);
                    })
                    .orElseGet(() -> {
                        response.put("success", false);
                        response.put("message", "Producto no encontrado.");
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                    });
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al actualizar stock mínimo: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
