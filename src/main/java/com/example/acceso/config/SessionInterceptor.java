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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
            "/pedidos_web/api/descargar-especificacion/",
            "/ventas_web/api/guardar",
            "/ventas_web/api/listar",
            "/ventas_web/api/detalle",
            "/ventas_web/api/aprobar",
            "/ventas_web/api/rechazar",
            "/clientes/api/consultar-dni/",
            "/clientes/api/consultar-ruc/",
            "/clientes/api/buscar-o-crear"
    );

    // Rutas de API internas que deben ser accesibles para usuarios autenticados (no solo admins)
    private static final Set<String> RUTAS_API_PERMITIDAS = Set.of(
            "/caja/api/",
            "/ventas/api/",
            "/clientes/api/"
    );
    
    // Rutas adicionales permitidas para usuarios autenticados
    private static final Set<String> RUTAS_ADICIONALES_PERMITIDAS = Set.of(
            "/ventas/nota-credito",
            "/reportes"
    );

    public SessionInterceptor(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    // Helper para extraer la "ruta base" o "entidad" de una URI
    // Ej: "/productos/eliminar/1" -> "/productos"
    // Ej: "/usuarios/api/listar" -> "/usuarios"
    // Ej: "/dashboard" -> "/dashboard"
    private String extractBasePath(String uri) {
        if (uri == null || uri.isEmpty() || "/".equals(uri)) {
            return "/";
        }
        // Eliminar el primer '/' si existe para facilitar el split
        String cleanUri = uri.startsWith("/") ? uri.substring(1) : uri;
        int firstSlash = cleanUri.indexOf('/');
        if (firstSlash == -1) { // No hay más slashes, es una ruta base como "/dashboard"
            return "/" + cleanUri;
        }
        return "/" + cleanUri.substring(0, firstSlash);
    }

    private Set<String> parseRutasDerivadas(String rutasDerivadas) {
        if (rutasDerivadas == null || rutasDerivadas.trim().isEmpty()) {
            return new HashSet<>();
        }
        return Arrays.stream(rutasDerivadas.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        HttpSession session = request.getSession(false); // Obtener la sesión aquí para depuración

        System.out.println("--- SessionInterceptor: Interceptando URI: " + requestURI + " ---");

        // 1. Permitir acceso a rutas públicas definidas (sin autenticación ni verificación de permisos).
        boolean esRutaPublica = RUTAS_PUBLICAS.stream().anyMatch(requestURI::startsWith);
        if (esRutaPublica) {
            System.out.println("--- SessionInterceptor: Ruta pública, acceso concedido. ---");
            return true; // Retornar inmediatamente sin más verificaciones
        }

        // 2. Si la ruta no es pública, verificar la sesión.
        if (session == null || session.getAttribute("usuarioLogueado") == null) {
            System.out.println("--- SessionInterceptor: Sesión no encontrada o usuario no logueado, redirigiendo a /login. ---");
            response.sendRedirect("/login");
            return false; // Detener la ejecución.
        }

        // --- NUEVA LÓGICA PARA ADMINISTRADORES Y RUTAS /api/ ---
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        System.out.println("--- SessionInterceptor: isAdmin en sesión: " + isAdmin + " ---");
        if (isAdmin != null && isAdmin && requestURI.contains("/api/")) {
            System.out.println("--- SessionInterceptor: Administrador accediendo a ruta /api/, acceso concedido. ---");
            return true; // Administrador tiene acceso a todas las rutas /api/
        }
        // --- FIN NUEVA LÓGICA ---

    // 2.5 Permitir rutas PDF internas para usuarios autenticados
    boolean esRutaPdfPermitida = requestURI.equals("/caja/sesion-actual/pdf") || 
                                  (requestURI.startsWith("/caja/historial/") && 
                                   (requestURI.endsWith("/pdf") || requestURI.endsWith("/pdf/download")));
    if (esRutaPdfPermitida) {
        System.out.println("--- SessionInterceptor: Ruta PDF permitida para usuario autenticado, acceso concedido. ---");
        return true;
    }

    // 2.6 Permitir rutas API internas para usuarios autenticados (no solo admins)
    boolean esRutaApiPermitida = RUTAS_API_PERMITIDAS.stream().anyMatch(requestURI::startsWith);
    if (esRutaApiPermitida) {
        System.out.println("--- SessionInterceptor: Ruta API interna permitida para usuario autenticado, acceso concedido. ---");
        return true;
    }
    
    // 2.7 Permitir rutas adicionales para usuarios autenticados
    boolean esRutaAdicionalPermitida = RUTAS_ADICIONALES_PERMITIDAS.stream().anyMatch(requestURI::startsWith);
    if (esRutaAdicionalPermitida) {
        System.out.println("--- SessionInterceptor: Ruta adicional permitida para usuario autenticado, acceso concedido. ---");
        return true;
    }

        // 3. Si hay sesión, verificar permisos para la ruta solicitada.
        // La ruta raíz ("/") se permite para todos los usuarios logueados.
        if (requestURI.equals("/")) {
            System.out.println("--- SessionInterceptor: Ruta raíz, acceso concedido. ---");
            return true;
        }

        @SuppressWarnings("unchecked")
        List<Opcion> menuOpciones = (List<Opcion>) session.getAttribute("menuOpciones");

        // Construir un conjunto de rutas base permitidas
        Set<String> allowedBasePaths = new HashSet<>();
        if (menuOpciones != null) {
            for (Opcion opcion : menuOpciones) {
                // Añadir la ruta base de la ruta principal de la opción
                String rutaPrincipal = opcion.getRuta();
                if (!rutaPrincipal.equals("/")) { // No queremos que "/" conceda acceso a todo
                    allowedBasePaths.add(extractBasePath(rutaPrincipal));
                }

                // Añadir las rutas base de las rutas derivadas de la opción
                Set<String> derivadas = parseRutasDerivadas(opcion.getRutasDerivadas());
                for (String derivada : derivadas) {
                    allowedBasePaths.add(extractBasePath(derivada));
                }
            }
        }

        // Extraer la ruta base de la URI solicitada
        String requestedBasePath = extractBasePath(requestURI);

        // Verificar si la ruta base solicitada está entre las permitidas
        boolean tienePermiso = allowedBasePaths.contains(requestedBasePath);

        System.out.println("--- SessionInterceptor: Ruta base solicitada: " + requestedBasePath + ", Rutas base permitidas: " + allowedBasePaths + " ---");
        System.out.println("--- SessionInterceptor: Tiene permiso (por ruta base): " + tienePermiso + " ---");


        // 4. Si no tiene permiso, redirigir con mensaje de error.
        if (tienePermiso) {
            System.out.println("--- SessionInterceptor: Acceso concedido por ruta base. ---");
            return true;
        } else {
            // Para las llamadas de API que fallan, es mejor no redirigir,
            // sino devolver un error 403 (Prohibido) para que el JS pueda manejarlo.
            if (requestURI.contains("/api/")) { // Esta verificación es para usuarios NO administradores
                System.out.println("--- SessionInterceptor: Acceso denegado a API para usuario no administrador. ---");
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"error\": \"Acceso denegado a la API.\"}");
                return false;
            }

            System.out.println("--- SessionInterceptor: Acceso denegado, redirigiendo a / con mensaje de error. ---");
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