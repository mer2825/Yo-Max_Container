package com.example.acceso.controller;

import com.example.acceso.dto.AuditDetailsDto;
import com.example.acceso.model.Opcion;
import com.example.acceso.model.Perfil;
import com.example.acceso.service.OpcionService;
import com.example.acceso.service.PerfilService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/perfiles")
public class PerfilController {

    private final PerfilService perfilService;
    private final OpcionService opcionService;
    private static final String ADMIN_PROFILE_NAME = "Administrador";

    public PerfilController(PerfilService perfilService, OpcionService opcionService) {
        this.perfilService = perfilService;
        this.opcionService = opcionService;
    }

    private boolean esAdministrador(HttpSession session) {
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        return isAdmin != null && isAdmin;
    }

    @GetMapping("/listar")
    public String mostrarPaginaPerfiles() {
        return "perfiles";
    }

    @GetMapping("/api/listar")
    @ResponseBody
    public ResponseEntity<?> listarPerfilesApi() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", perfilService.listarPerfiles());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> obtenerPerfil(@PathVariable Long id) {
        return perfilService.obtenerPerfilPorId(id)
                .map(perfil -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    Map<String, Object> perfilData = new HashMap<>();
                    perfilData.put("id", perfil.getId());
                    perfilData.put("nombre", perfil.getNombre());
                    perfilData.put("descripcion", perfil.getDescripcion());
                    perfilData.put("estado", perfil.getEstado()); // Modificado de isEstado() a getEstado()
                    Set<Long> idOpciones = perfil.getOpciones().stream().map(Opcion::getId).collect(Collectors.toSet());
                    perfilData.put("opciones", idOpciones);
                    response.put("data", perfilData);
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/guardar")
    @ResponseBody
    public ResponseEntity<?> guardarPerfil(@RequestBody Perfil perfil, HttpSession session) {
        if (!esAdministrador(session)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "Acceso denegado. Solo administradores pueden realizar esta acción."));
        }

        // Prohibir la modificación del perfil Administrador
        if (perfil.getId() != null) {
            Perfil perfilExistente = perfilService.obtenerPerfilPorId(perfil.getId())
                .orElseThrow(() -> new IllegalArgumentException("Perfil no encontrado"));
            if (ADMIN_PROFILE_NAME.equalsIgnoreCase(perfilExistente.getNombre())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "El perfil 'Administrador' no puede ser modificado."));
            }
        }

        Map<String, Object> response = new HashMap<>();
        try {
            Perfil perfilGuardado = perfilService.guardarPerfil(perfil);
            response.put("success", true);
            response.put("message", perfil.getId() != null ? "Perfil actualizado" : "Perfil creado");
            response.put("perfil", perfilGuardado);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al guardar el perfil: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/api/cambiar-estado/{id}")
    @ResponseBody
    public ResponseEntity<?> cambiarEstadoPerfil(@PathVariable Long id, HttpSession session) {
        if (!esAdministrador(session)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "Acceso denegado. Solo administradores pueden realizar esta acción."));
        }

        Perfil perfil = perfilService.obtenerPerfilPorId(id)
            .orElseThrow(() -> new IllegalArgumentException("Perfil no encontrado"));
        if (ADMIN_PROFILE_NAME.equalsIgnoreCase(perfil.getNombre())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "El estado del perfil 'Administrador' no puede ser cambiado."));
        }

        return perfilService.cambiarEstadoPerfil(id)
                .map(p -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Estado del perfil actualizado");
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Perfil no encontrado");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                });
    }

    @GetMapping("/api/opciones")
    @ResponseBody
    public ResponseEntity<?> listarOpcionesApi() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", opcionService.listarOpciones());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarPerfil(@PathVariable Long id, HttpSession session) {
        if (!esAdministrador(session)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "Acceso denegado. Solo administradores pueden realizar esta acción."));
        }

        Perfil perfil = perfilService.obtenerPerfilPorId(id)
            .orElseThrow(() -> new IllegalArgumentException("Perfil no encontrado"));
        if (ADMIN_PROFILE_NAME.equalsIgnoreCase(perfil.getNombre())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "El perfil 'Administrador' no puede ser eliminado."));
        }

        Map<String, Object> response = new HashMap<>();
        try {
            perfilService.eliminarPerfil(id);
            response.put("success", true);
            response.put("message", "Perfil eliminado correctamente");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al eliminar el perfil: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/api/audit-details/{id}")
    @ResponseBody
    public ResponseEntity<AuditDetailsDto> getAuditDetails(@PathVariable Long id) {
        try {
            AuditDetailsDto dto = perfilService.getAuditDetails(id);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
