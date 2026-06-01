package com.example.acceso.config;

import com.example.acceso.model.Usuario;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Component
public class AuditorAwareImpl implements AuditorAware<Long> {

    @Override
    public Optional<Long> getCurrentAuditor() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return Optional.empty();
        }

        HttpSession session = attributes.getRequest().getSession(false);
        if (session == null) {
            return Optional.empty();
        }

        Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioLogueado == null) {
            return Optional.empty(); // O puedes devolver un ID de sistema por defecto, ej. Optional.of(-1L)
        }

        return Optional.of(usuarioLogueado.getId());
    }
}