package com.example.acceso.controller;

import java.util.HashMap; // Importamos el nuevo modelo Proveedor
import java.util.List;
import java.util.Map; // Asumimos la existencia de ProveedorService

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.acceso.model.Proveedor;
import com.example.acceso.service.ProveedorService;

import jakarta.validation.Valid;

// @Controller: Indica que esta clase es un controlador web.
// @RequestMapping("/proveedores"): Todas las rutas de este controlador empezarán con "/proveedores".
@Controller
@RequestMapping("/proveedores")
public class ProveedorController {

    // Inyección del servicio de Proveedor.
    private final ProveedorService proveedorService;

    // Nota: Eliminamos CategoriaService porque no es relevante para la gestión de Proveedores.
    public ProveedorController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    // -------------------------------------------------------------------------
    // ENDPOINTS HTML/THYMELEAF
    // -------------------------------------------------------------------------

    // GET /proveedores/listar: Muestra la página HTML principal de gestión de
    // proveedores.
    @GetMapping("/listar")
    public String listarProveedores(Model model) {
        // Prepara el modelo inicial y carga los datos.
        List<Proveedor> proveedores = proveedorService.listarProveedores();
        model.addAttribute("proveedores", proveedores);
        model.addAttribute("formProveedor", new Proveedor());

        // Asumimos que el nombre de la vista HTML será "proveedores"
        return "proveedores";
    }

    @GetMapping("/activos")
    public String listarProveedoresActivos(Model model) {
        // Prepara el modelo inicial y carga los datos.
        List<Proveedor> proveedores = proveedorService.listarProveedoresActivos();
        model.addAttribute("proveedores", proveedores);
        model.addAttribute("formProveedor", new Proveedor());

        // Asumimos que el nombre de la vista HTML será "proveedores"
        return "proveedores";
    }

    // -------------------------------------------------------------------------
    // ENDPOINTS REST API (JSON)
    // -------------------------------------------------------------------------

    // GET /proveedores/api/listar: Devuelve la lista completa de proveedores en JSON.
    @GetMapping("/api/listar")
    @ResponseBody
    public ResponseEntity<?> listarProveedoresApi() {
        Map<String, Object> response = new HashMap<>();
        List<Proveedor> proveedores = proveedorService.listarProveedores();
        response.put("success", true);
        response.put("data", proveedores);
        return ResponseEntity.ok(response);
    }

    // GET /proveedores/api/activos: Devuelve la lista de proveedores activos en JSON.
    @GetMapping("/api/activos")
    @ResponseBody
    public ResponseEntity<?> listarProveedoresActivosApi() {
        Map<String, Object> response = new HashMap<>();
        List<Proveedor> proveedores = proveedorService.listarProveedoresActivos();
        response.put("success", true);
        response.put("data", proveedores);
        return ResponseEntity.ok(response);
    }


    // POST /proveedores/api/guardar: Endpoint para crear o actualizar un proveedor.
    @PostMapping("/api/guardar")
    @ResponseBody
    public ResponseEntity<?> guardarProveedorAjax(@Valid @RequestBody Proveedor proveedor, BindingResult bindingResult) {
        Map<String, Object> response = new HashMap<>();

        // Si hay errores de validación.
        if (bindingResult.hasErrors()) {
            // Recopila los errores.
            Map<String, String> errores = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error -> errores.put(error.getField(), error.getDefaultMessage()));
            response.put("success", false);
            response.put("message", "Datos de proveedor inválidos");
            response.put("errors", errores);
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // Llama al servicio para guardar el proveedor.
            Proveedor proveedorGuardado = proveedorService.guardarProveedor(proveedor);
            response.put("success", true);
            response.put("proveedor", proveedorGuardado);
            response.put("message",
                    // El ID de Proveedor es idProveedor en el modelo, usamos eso.
                    proveedor.getIdProveedor() != null ? "Proveedor actualizado correctamente" : "Proveedor creado correctamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Captura cualquier excepción del servicio.
            response.put("success", false);
            response.put("message", "Error interno del servidor al guardar proveedor: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // GET /proveedores/api/{id}: Devuelve los datos de un único proveedor por su ID.
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> obtenerProveedor(@PathVariable Long id) {
        try {
            return proveedorService.obtenerProveedorPorId(id).map(proveedor -> {
                // Si el proveedor se encuentra, lo envuelve en una respuesta exitosa.
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", proveedor);
                return ResponseEntity.ok(response);
            }).orElseGet(() -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Proveedor no encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            });
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al obtener proveedor: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // DELETE /proveedores/api/eliminar/{id}: Realiza el borrado lógico de un proveedor (cambio de estado a inactivo).
    @DeleteMapping("/api/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarProveedorAjax(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Se verifica si el proveedor existe antes de intentar eliminarlo.
            if (!proveedorService.obtenerProveedorPorId(id).isPresent()) {
                response.put("success", false);
                response.put("message", "Proveedor no encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // Se asume que el método 'eliminarProveedor' en el servicio hace un borrado lógico (cambio de estado).
            proveedorService.eliminarProveedor(id);
            response.put("success", true);
            response.put("message", "Proveedor marcado como inactivo (eliminado lógicamente) correctamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al eliminar proveedor: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // POST /proveedores/api/cambiar-estado/{id}: Activa o desactiva un proveedor.
    @PostMapping("/api/cambiar-estado/{id}")
    @ResponseBody
    public ResponseEntity<?> cambiarEstadoProveedorAjax(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Llama al servicio para cambiar el estado.
            return proveedorService.cambiarEstadoProveedor(id)
                    .map(proveedor -> {
                        response.put("success", true);
                        response.put("proveedor", proveedor);
                        response.put("message", "Estado del proveedor actualizado correctamente");
                        return ResponseEntity.ok(response);
                    })
                    .orElseGet(() -> {
                        // Si el servicio no encuentra el proveedor, devuelve un error 404.
                        response.put("success", false);
                        response.put("message", "Proveedor no encontrado");
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                    });
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al cambiar estado del proveedor: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
