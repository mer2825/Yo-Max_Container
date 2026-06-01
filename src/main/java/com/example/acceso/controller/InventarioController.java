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
}
