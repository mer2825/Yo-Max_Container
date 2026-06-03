package com.example.acceso.controller;

import com.example.acceso.model.Empresa;
import com.example.acceso.model.Opcion;
import com.example.acceso.model.Usuario;
import com.example.acceso.service.EmpresaService;
import com.example.acceso.service.LoginAttemptService;
import com.example.acceso.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Controller
public class LoginController {
    private final UsuarioService usuarioService;
    private final EmpresaService empresaService;
    private final LoginAttemptService loginAttemptService;

    public LoginController(UsuarioService usuarioService, EmpresaService empresaService, LoginAttemptService loginAttemptService) {
        this.usuarioService = usuarioService;
        this.empresaService = empresaService;
        this.loginAttemptService = loginAttemptService;
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    @GetMapping("/login")
    public String mostrarFormularioLogin(HttpSession session, Model model, HttpServletRequest request) { // Añadir Model y HttpServletRequest
        if (session.getAttribute("usuarioLogueado") != null) {
            return "redirect:/";
        }
        
        // Verificar si la IP ya está bloqueada independientemente del usuario (o podríamos usar un indicador genérico)
        // Para que se mantenga al recargar, necesitamos la IP, pero el bloqueo en POST es por IP-Usuario
        // Una forma sencilla es revisar si *solo* la IP está asociada a algún bloqueo reciente
        // o si guardamos el último intento fallido en sesión
        
        Long bloqueoSegundos = (Long) session.getAttribute("bloqueoSegundos");
        if (bloqueoSegundos != null) {
            String lastLoginKey = (String) session.getAttribute("lastLoginKey");
            if(lastLoginKey != null && loginAttemptService.isBlocked(lastLoginKey)) {
                long remainingTime = loginAttemptService.getRemainingBlockTime(lastLoginKey);
                model.addAttribute("bloqueoSegundos", remainingTime);
            } else {
                 session.removeAttribute("bloqueoSegundos");
                 session.removeAttribute("lastLoginKey");
            }
        }
        
        // Cargar la información de la empresa y añadirla al modelo
        Empresa empresa = empresaService.getEmpresaInfo();
        model.addAttribute("empresa", empresa);

        return "login";
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam String usuario, @RequestParam String clave,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        // Validación de longitud de usuario
        if (usuario == null || usuario.trim().isEmpty() || usuario.length() < 3 || usuario.length() > 50) {
            redirectAttributes.addFlashAttribute("error", "El usuario debe tener entre 3 y 50 caracteres.");
            return "redirect:/login";
        }

        // Validación de longitud de clave
        if (clave == null || clave.trim().isEmpty() || clave.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "La contraseña debe tener como mínimo 6 caracteres.");
            return "redirect:/login";
        }

        String ip = getClientIP(request);
        String loginKey = ip + "-" + usuario; // Bloquear por IP y nombre de usuario

        if (loginAttemptService.isBlocked(loginKey)) {
            long remainingTime = loginAttemptService.getRemainingBlockTime(loginKey);
            session.setAttribute("bloqueoSegundos", remainingTime);
            session.setAttribute("lastLoginKey", loginKey);
            redirectAttributes.addFlashAttribute("bloqueoSegundos", remainingTime);
            return "redirect:/login";
        }

        Optional<Usuario> usuarioOpt = usuarioService.findByUsuario(usuario);

        if (usuarioOpt.isEmpty()) {
            loginAttemptService.loginFailed(loginKey);
            redirectAttributes.addFlashAttribute("error", "Usuario no encontrado.");
            return "redirect:/login";
        }

        Usuario usuarioEncontrado = usuarioOpt.get();

        if (usuarioEncontrado.getEstado() != 1) {
            redirectAttributes.addFlashAttribute("error", "Este usuario se encuentra inactivo.");
            return "redirect:/login";
        }

        if (usuarioService.verificarContrasena(clave, usuarioEncontrado.getClave())) {
            loginAttemptService.loginSucceeded(loginKey); // Limpiar intentos exitosos
            session.removeAttribute("bloqueoSegundos");
            session.removeAttribute("lastLoginKey");
            session.setAttribute("usuarioLogueado", usuarioEncontrado);

            // --- INICIO DE LA LÓGICA DE ADMINISTRADOR ---
            boolean isAdmin = usuarioEncontrado.getPerfil() != null && "administrador".equalsIgnoreCase(usuarioEncontrado.getPerfil().getNombre());
            session.setAttribute("isAdmin", isAdmin);
            // --- FIN DE LA LÓGICA DE ADMINISTRADOR ---

            List<Opcion> opcionesMenu = new ArrayList<>(usuarioEncontrado.getPerfil().getOpciones().stream()
                    .sorted(Comparator.comparing(Opcion::getId))
                    .toList());

            // Crear y añadir la opción de Dashboard/Inicio
            Opcion dashboardOpcion = new Opcion();
            dashboardOpcion.setId(0L); // Asumiendo que 0 lo colocará al principio o no causará conflicto
            dashboardOpcion.setNombre("Inicio");
            dashboardOpcion.setRuta("/");
            dashboardOpcion.setIcono("bi-house-door-fill"); // Un ícono de ejemplo para 'Inicio'
            opcionesMenu.add(0, dashboardOpcion);

            session.setAttribute("menuOpciones", opcionesMenu);

            return "redirect:/";
        } else {
            loginAttemptService.loginFailed(loginKey);
            
            // Después del intento fallido, revisamos si acabamos de bloquear al usuario para mostrarle los segundos directamente
            if(loginAttemptService.isBlocked(loginKey)){
                long remainingTime = loginAttemptService.getRemainingBlockTime(loginKey);
                session.setAttribute("bloqueoSegundos", remainingTime);
                session.setAttribute("lastLoginKey", loginKey);
                redirectAttributes.addFlashAttribute("bloqueoSegundos", remainingTime);
            } else {
                redirectAttributes.addFlashAttribute("error", "Contraseña incorrecta.");
            }

            return "redirect:/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("logout", "Has cerrado sesión exitosamente.");
        return "redirect:/login";
    }
}
