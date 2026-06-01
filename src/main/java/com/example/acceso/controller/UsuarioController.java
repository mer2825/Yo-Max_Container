package com.example.acceso.controller;

import com.example.acceso.dto.AuditDetailsDto;
import com.example.acceso.model.Perfil;
import com.example.acceso.model.Usuario;
import com.example.acceso.service.PerfilService;
import com.example.acceso.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final PerfilService perfilService;
    private final SimpMessagingTemplate messagingTemplate;
    private static final Long SUPER_ADMIN_ID = 1L;
    private static final String ADMIN_PROFILE_NAME = "Administrador";


    public UsuarioController(UsuarioService usuarioService, PerfilService perfilService, SimpMessagingTemplate messagingTemplate) {
        this.usuarioService = usuarioService;
        this.perfilService = perfilService;
        this.messagingTemplate = messagingTemplate;
    }

    private void notificarCambio(String tipo, String username) {
        Map<String, String> message = Map.of(
            "type", tipo,
            "username", username
        );
        messagingTemplate.convertAndSend("/topic/usuarios", message);
    }

    private boolean tienePermisoParaGestionar(HttpSession session) {
        Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioLogueado == null || usuarioLogueado.getPerfil() == null) {
            return false;
        }
        return usuarioLogueado.getPerfil().getOpciones().stream()
                .anyMatch(opcion -> "/usuarios/listar".equals(opcion.getRuta()) ||
                                     "/perfiles/listar".equals(opcion.getRuta()));
    }

    @GetMapping("/listar")
    public String listarUsuarios(Model model) {
        return "usuarios";
    }

    @GetMapping("/api/listar")
    @ResponseBody
    public ResponseEntity<?> listarUsuariosApi() {
        Map<String, Object> response = new HashMap<>();
        List<Usuario> usuarios = usuarioService.listarUsuarios();
        response.put("success", true);
        response.put("data", usuarios);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/perfiles")
    @ResponseBody
    public ResponseEntity<?> listarPerfilesActivosApi() {
        Map<String, Object> response = new HashMap<>();
        response.put("data", perfilService.listarPerfilesActivos());
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/guardar")
    @ResponseBody
    public ResponseEntity<?> guardarUsuarioAjax(@Valid @RequestBody Usuario usuario, BindingResult bindingResult, HttpSession session) {
        if (!tienePermisoParaGestionar(session)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "No tienes permiso para realizar esta acción."));
        }

        // Validaciones de unicidad
        Optional<Usuario> existingUserByUsuario = usuarioService.findByUsuario(usuario.getUsuario());
        if (existingUserByUsuario.isPresent() && (usuario.getId() == null || !existingUserByUsuario.get().getId().equals(usuario.getId()))) {
            bindingResult.rejectValue("usuario", "Unique", "El nombre de usuario ya está en uso.");
        }

        Optional<Usuario> existingUserByCorreo = usuarioService.findByCorreo(usuario.getCorreo());
        if (existingUserByCorreo.isPresent() && (usuario.getId() == null || !existingUserByCorreo.get().getId().equals(usuario.getId()))) {
            bindingResult.rejectValue("correo", "Unique", "No puede usar un correo ya vinculado.");
        }

        if (usuario.getId() != null && Objects.equals(usuario.getId(), SUPER_ADMIN_ID)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "El usuario administrador original no puede ser modificado."));
        }

        Usuario actor = (Usuario) session.getAttribute("usuarioLogueado");
        boolean actorIsAdmin = actor != null && actor.getPerfil() != null && ADMIN_PROFILE_NAME.equalsIgnoreCase(actor.getPerfil().getNombre());

        if (usuario.getId() != null) { // Al editar
            Optional<Usuario> targetOpt = usuarioService.obtenerUsuarioPorId(usuario.getId());
            if (targetOpt.isPresent() && targetOpt.get().getPerfil() != null && ADMIN_PROFILE_NAME.equalsIgnoreCase(targetOpt.get().getPerfil().getNombre())) {
                if (!actorIsAdmin) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "No tienes permiso para modificar a un administrador."));
                }
            }
        }

        // Regla de seguridad: Solo un admin puede asignar el perfil de "Administrador"
        if (usuario.getPerfil() != null && usuario.getPerfil().getId() != null) {
            Optional<Perfil> targetProfileOpt = perfilService.obtenerPerfilPorId(usuario.getPerfil().getId());
            if (targetProfileOpt.isPresent() && ADMIN_PROFILE_NAME.equalsIgnoreCase(targetProfileOpt.get().getNombre())) {
                if (!actorIsAdmin) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "Solo un administrador puede asignar el perfil de 'Administrador'."));
                }
            }
        }

        // Validación de contraseña
        boolean isNewUser = usuario.getId() == null;
        if (isNewUser && (usuario.getClave() == null || usuario.getClave().trim().isEmpty())) {
            bindingResult.rejectValue("clave", "NotBlank", "La contraseña es obligatoria para nuevos usuarios.");
        }
        if (usuario.getClave() != null && !usuario.getClave().trim().isEmpty() && usuario.getClave().length() < 6) {
            bindingResult.rejectValue("clave", "Size", "La contraseña debe tener como mínimo 6 caracteres");
        }


        Map<String, Object> response = new HashMap<>();
        if (bindingResult.hasErrors()) {
            Map<String, String> errores = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error -> errores.put(error.getField(), error.getDefaultMessage()));
            response.put("success", false);
            response.put("message", "Datos inválidos");
            response.put("errors", errores);
            return ResponseEntity.badRequest().body(response);
        }
        try {
            Usuario usuarioGuardado = usuarioService.guardarUsuario(usuario);
            
            // Notificar a los clientes WebSocket
            notificarCambio("USER_MODIFIED", usuarioGuardado.getUsuario());

            response.put("success", true);
            response.put("usuario", usuarioGuardado);
            response.put("message", usuario.getId() != null ? "Usuario actualizado" : "Usuario creado");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno al guardar el usuario.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> obtenerUsuario(@PathVariable Long id) {
        return usuarioService.obtenerUsuarioPorId(id)
                .map(usuario -> ResponseEntity.ok(Map.of("success", true, "data", usuario)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarUsuarioAjax(@PathVariable Long id, HttpSession session) {
        if (!tienePermisoParaGestionar(session)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "No tienes permiso para realizar esta acción."));
        }

        if (Objects.equals(id, SUPER_ADMIN_ID)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "El usuario administrador original no puede ser eliminado."));
        }

        Usuario actor = (Usuario) session.getAttribute("usuarioLogueado");
        if (actor != null && Objects.equals(actor.getId(), id)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", "No puedes eliminarte a ti mismo."));
        }

        Optional<Usuario> targetUserOpt = usuarioService.obtenerUsuarioPorId(id);
        if (targetUserOpt.isPresent()) {
            Usuario targetUser = targetUserOpt.get();
            boolean targetIsAdmin = targetUser.getPerfil() != null && ADMIN_PROFILE_NAME.equalsIgnoreCase(targetUser.getPerfil().getNombre());
            boolean actorIsAdmin = actor != null && actor.getPerfil() != null && ADMIN_PROFILE_NAME.equalsIgnoreCase(actor.getPerfil().getNombre());

            if (targetIsAdmin && !actorIsAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "No tienes permiso para eliminar a un administrador."));
            }
            
            try {
                usuarioService.eliminarUsuario(id);
                notificarCambio("USER_MODIFIED", targetUser.getUsuario());
                return ResponseEntity.ok(Map.of("success", true, "message", "Usuario eliminado"));
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
            }

        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Usuario no encontrado."));
        }
    }

    @PostMapping("/api/cambiar-estado/{id}")
    @ResponseBody
    public ResponseEntity<?> cambiarEstadoUsuarioAjax(@PathVariable Long id, HttpSession session) {
        if (!tienePermisoParaGestionar(session)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "No tienes permiso para realizar esta acción."));
        }

        if (Objects.equals(id, SUPER_ADMIN_ID)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "El estado del usuario administrador original no puede ser cambiado."));
        }
        
        Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioLogueado != null && Objects.equals(usuarioLogueado.getId(), id)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", "No puedes cambiar tu propio estado."));
        }

        return usuarioService.cambiarEstadoUsuario(id)
                .map(usuario -> {
                    notificarCambio("USER_MODIFIED", usuario.getUsuario());
                    return ResponseEntity.ok(Map.of("success", true, "message", "Estado actualizado"));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Usuario no encontrado")));
    }

    @GetMapping("/api/audit-details/{id}")
    @ResponseBody
    public ResponseEntity<AuditDetailsDto> getAuditDetails(@PathVariable Long id) {
        try {
            AuditDetailsDto dto = usuarioService.getAuditDetails(id);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
