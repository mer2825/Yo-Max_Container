package com.example.acceso.config;

import com.example.acceso.model.Empresa;
import com.example.acceso.model.Opcion;
import com.example.acceso.model.Usuario;
import com.example.acceso.service.EmpresaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Set;

@Component
public class SessionInterceptor implements HandlerInterceptor {

    private final EmpresaService empresaService;
    // Lista de rutas que no requieren autenticación.
    private static final Set<String> RUTAS_PUBLICAS = Set.of(
            "/login",
            "/error",
            "/css/",
            "/js/",
            "/images/",
            "/webjars/",
            "/checkout",
            "/uploads/",
            "/api/usuario-actual",
            "/api/upload",
            "/pedidos_web/api/crear",
            "/pedidos_web/api/listar",
            "/ventas_web/api/guardar",
            "/ventas_web/api/listar",
            "/ventas_web/api/detalle",
            "/ventas_web/api/aprobar",
            "/ventas_web/api/rechazar"
    );

    public SessionInterceptor(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        String requestURI = request.getRequestURI();

        // 1. Permitir acceso a rutas públicas definidas (sin autenticación ni verificación de permisos).
        boolean esRutaPublica = RUTAS_PUBLICAS.stream().anyMatch(requestURI::startsWith);
        if (esRutaPublica) {
            return true; // Retornar inmediatamente sin más verificaciones
        }

        // 2. Si la ruta no es pública, verificar la sesión.
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            response.sendRedirect("/login");
            return false; // Detener la ejecución.
        }

        // 3. Si hay sesión, verificar permisos para la ruta solicitada.
        // La ruta raíz ("/") se permite para todos los usuarios logueados.
        if (requestURI.equals("/")) {
            return true;
        }

        @SuppressWarnings("unchecked")
        List<Opcion> menuOpciones = (List<Opcion>) session.getAttribute("menuOpciones");

        boolean tienePermiso = false;
        if (menuOpciones != null) {
            // Se excluye la ruta raíz "/" de esta comprobación `startsWith` para evitar
            // conceder acceso a todas las rutas a los usuarios que solo tienen permiso para la página de inicio.
            tienePermiso = menuOpciones.stream()
                    .map(Opcion::getRuta)
                    .filter(ruta -> !ruta.equals("/"))
                    .anyMatch(requestURI::startsWith);
        }

        // Casos especiales para rutas que no están directamente en el menú de navegación,
        // pero que deben ser accesibles si el usuario tiene el permiso de listado principal.
        // Esto incluye:
        // 1. Rutas de API (ej. /productos/api/listar)
        // 2. Rutas de acciones (ejemplo. /productos/modificar/{id})
        if (!tienePermiso && menuOpciones != null) {
            String[] parts = requestURI.split("/");
            if (parts.length > 2) { // Asegura que haya al menos /entity/action, ej. ["", "productos", "api"]
                String entity = parts[1]; // ej. "productos"
                String segment = parts[2]; // ej. "api", "modificar"

                // Si la ruta es una API, una de modificación o de detalle, se verifica el permiso de listado principal.
                if (segment.equals("api") || segment.equals("modificar") || segment.equals("detalle")) {
                    String listPermission = "/" + entity + "/listar";
                    tienePermiso = menuOpciones.stream()
                            .map(Opcion::getRuta)
                            .anyMatch(listPermission::equals);
                }
            }
        }

        // 4. Si no tiene permiso, redirigir con mensaje de error.
        if (tienePermiso) {
            return true;
        } else {
            // Para las llamadas de API que fallan, es mejor no redirigir,
            // sino devolver un error 403 (Prohibido) para que el JS pueda manejarlo.
            if (requestURI.contains("/api/")) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"error\": \"Acceso denegado a la API.\"}");
                return false;
            }

            session.setAttribute("access_denied_error", "No tienes permiso para acceder a esta página.");
            response.sendRedirect("/");
            return false; // Detener la ejecución.
        }
    }

    @Override
    public void postHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, ModelAndView modelAndView) {
        if (modelAndView != null && !isRedirectView(modelAndView)) {
            HttpSession session = request.getSession(false);

            Empresa empresa = empresaService.getEmpresaInfo();
            modelAndView.addObject("empresaGlobal", empresa);

            if (session != null) {
                Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
                if (usuario != null) {
                    modelAndView.addObject("usuarioGlobal", usuario);
                }

                if (session.getAttribute("access_denied_error") != null) {
                    modelAndView.addObject("access_denied_error", session.getAttribute("access_denied_error"));
                    session.removeAttribute("access_denied_error");
                }
            }
        }
    }

    private boolean isRedirectView(ModelAndView modelAndView) {
        String viewName = modelAndView.getViewName();
        return viewName != null && viewName.startsWith("redirect:");
    }
}
