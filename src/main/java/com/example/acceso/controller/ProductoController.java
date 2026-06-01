package com.example.acceso.controller;

import com.example.acceso.dto.AuditDetailsDto;
import com.example.acceso.model.Producto;
import com.example.acceso.model.ProductoImagen;
import com.example.acceso.service.CategoriaService;
import com.example.acceso.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/productos")
public class ProductoController {

    private final ProductoService productoService;
    private final CategoriaService categoriaService;

    public ProductoController(ProductoService productoService, CategoriaService categoriaService) {
        this.productoService = productoService;
        this.categoriaService = categoriaService;
    }

    @GetMapping("/listar")
    public String listarProductos(Model model) {
        List<Producto> productos = productoService.listarProductos();
        model.addAttribute("productos", productos);
        model.addAttribute("formProducto", new Producto());
        return "productos";
    }

    @GetMapping("/activos")
    public String listarProductosActivos(Model model) {
        List<Producto> productos = productoService.listarProductosActivos();
        model.addAttribute("productos", productos);
        model.addAttribute("formProducto", new Producto());
        return "productos";
    }

    @GetMapping("/api/activos")
    @ResponseBody
    public ResponseEntity<?> listarProductosActivosApi() {
        Map<String, Object> response = new HashMap<>();
        List<Producto> productos = productoService.listarProductosActivos();
        response.put("success", true);
        response.put("data", productos);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/listar")
    @ResponseBody
    public ResponseEntity<?> listarProductosApi() {
        Map<String, Object> response = new HashMap<>();
        List<Producto> productos = productoService.listarProductos();
        response.put("success", true);
        response.put("data", productos);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/categorias")
    @ResponseBody
    public ResponseEntity<?> listarCategoriasActivasApi() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", categoriaService.listarCategoriasActivas());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/guardar")
    @ResponseBody
    public ResponseEntity<?> guardarProductoAjax(@Valid @RequestBody Producto producto, BindingResult bindingResult) {
        Map<String, Object> response = new HashMap<>();
        if (bindingResult.hasErrors()) {
            Map<String, String> errores = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error -> errores.put(error.getField(), error.getDefaultMessage()));
            response.put("success", false);
            response.put("message", "Datos de producto inválidos");
            response.put("errors", errores);
            return ResponseEntity.badRequest().body(response);
        }
        try {
            Producto productoGuardado = productoService.guardarProducto(producto);
            response.put("success", true);
            response.put("producto", productoGuardado);
            response.put("message", producto.getId() != null ? "Producto actualizado correctamente" : "Producto creado correctamente");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor al guardar producto: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/api/subir-imagenes")
    @ResponseBody
    public ResponseEntity<?> subirImagenes(@RequestParam("files") MultipartFile[] files, @RequestParam("id") Long productoId) {
        Map<String, Object> response = new HashMap<>();
        if (files == null || files.length == 0) {
            response.put("success", false);
            response.put("message", "No se seleccionaron archivos.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            List<ProductoImagen> imagenesGuardadas = new ArrayList<>();
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    ProductoImagen imagen = productoService.agregarImagenAProducto(file, productoId);
                    imagenesGuardadas.add(imagen);
                }
            }
            response.put("success", true);
            response.put("message", "Imágenes subidas correctamente.");
            response.put("imagenes", imagenesGuardadas);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al subir las imágenes: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/api/imagenes/eliminar-batch")
    @ResponseBody
    public ResponseEntity<?> eliminarImagenesBatch(@RequestBody List<Long> ids) {
        Map<String, Object> response = new HashMap<>();
        try {
            productoService.eliminarImagenesBatch(ids);
            response.put("success", true);
            response.put("message", "Imágenes eliminadas correctamente.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al eliminar las imágenes: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/api/imagenes/actualizar-orden")
    @ResponseBody
    public ResponseEntity<?> actualizarOrdenImagenes(@RequestBody List<Long> idsImagenes) {
        Map<String, Object> response = new HashMap<>();
        try {
            productoService.actualizarOrdenImagenes(idsImagenes);
            response.put("success", true);
            response.put("message", "Orden de las imágenes actualizado correctamente.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al actualizar el orden de las imágenes: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/view")
    public String viewProductos() {
        return "productosView";
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> obtenerProducto(@PathVariable Long id) {
        try {
            return productoService.obtenerProductoPorId(id)
                    .map(producto -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("data", producto);
                        return ResponseEntity.ok(response);
                    })
                    .orElseGet(() -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", false);
                        response.put("message", "Producto no encontrado");
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                    });
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al obtener producto: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @DeleteMapping("/api/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarProductoAjax(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (!productoService.obtenerProductoPorId(id).isPresent()) {
                response.put("success", false);
                response.put("message", "Producto no encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            productoService.eliminarProducto(id);
            response.put("success", true);
            response.put("message", "Producto marcado como inactivo (eliminado lógicamente) correctamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al eliminar producto: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/api/cambiar-estado/{id}")
    @ResponseBody
    public ResponseEntity<?> cambiarEstadoProductoAjax(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            return productoService.cambiarEstadoProducto(id)
                    .map(producto -> {
                        response.put("success", true);
                        response.put("producto", producto);
                        response.put("message", "Estado del producto actualizado correctamente");
                        return ResponseEntity.ok(response);
                    })
                    .orElseGet(() -> {
                        response.put("success", false);
                        response.put("message", "Producto no encontrado");
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                    });
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al cambiar estado del producto: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/api/audit-details/{id}")
    @ResponseBody
    public ResponseEntity<AuditDetailsDto> getAuditDetails(@PathVariable Long id) {
        try {
            AuditDetailsDto dto = productoService.getAuditDetails(id);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
