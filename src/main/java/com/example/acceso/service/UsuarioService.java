package com.example.acceso.service;

import com.example.acceso.dto.AuditDetailsDto;
import com.example.acceso.model.Usuario;
import com.example.acceso.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Long SUPER_ADMIN_ID = 1L;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findByIdNotAndEstadoNot(SUPER_ADMIN_ID, 2);
    }

    @Transactional
    public Usuario guardarUsuario(Usuario usuario) {
        boolean isNew = usuario.getId() == null;

        usuario.setNombre(usuario.getNombre().trim());
        usuario.setUsuario(usuario.getUsuario().trim().toLowerCase());
        usuario.setCorreo(usuario.getCorreo().trim().toLowerCase());

        if (!isNew) { // --- Lógica de Edición ---
            usuario.setUltimaAccion("Edición");
            Usuario usuarioExistente = usuarioRepository.findById(usuario.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado para actualizar"));

            if (usuario.getClave() == null || usuario.getClave().trim().isEmpty()) {
                usuario.setClave(usuarioExistente.getClave());
            } else {
                usuario.setClave(passwordEncoder.encode(usuario.getClave().trim()));
            }
        } else { // --- Lógica de Creación ---
            usuario.setUltimaAccion("Creación");
            usuario.setClave(passwordEncoder.encode(usuario.getClave().trim()));
            usuario.setEstado(1);
        }

        return usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public long contarUsuarios() {
        return usuarioRepository.countByEstadoNot(2);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> obtenerUsuarioPorId(Long id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }
        return usuarioRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> findByUsuario(String usuario) {
        return usuarioRepository.findByUsuario(usuario.trim().toLowerCase());
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> findByCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo.trim().toLowerCase());
    }

    @Transactional
    public void eliminarUsuario(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID de usuario inválido");
        }
        Usuario usuario = obtenerUsuarioPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        usuario.setEstado(2);
        usuario.setUltimaAccion("Eliminación");
        usuarioRepository.save(usuario);
    }

    @Transactional
    public Optional<Usuario> cambiarEstadoUsuario(Long id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }
        return obtenerUsuarioPorId(id).map(usuario -> {
            if (usuario.getEstado() == 1) {
                usuario.setEstado(0);
                usuario.setUltimaAccion("Inactivación");
            } else if (usuario.getEstado() == 0) {
                usuario.setEstado(1);
                usuario.setUltimaAccion("Activación");
            }
            return usuarioRepository.save(usuario);
        });
    }

    @Transactional(readOnly = true)
    public boolean existeUsuario(String nombreUsuario) {
        if (nombreUsuario == null || nombreUsuario.trim().isEmpty()) {
            return false;
        }
        return usuarioRepository.existsByUsuario(nombreUsuario.trim().toLowerCase());
    }

    @Transactional(readOnly = true)
    public boolean existeCorreo(String correo) {
        if (correo == null || correo.trim().isEmpty()) {
            return false;
        }
        return usuarioRepository.existsByCorreo(correo.trim().toLowerCase());
    }

    public boolean verificarContrasena(String contrasenaTextoPlano, String contrasenaEncriptada) {
        return passwordEncoder.matches(contrasenaTextoPlano, contrasenaEncriptada);
    }

    @Transactional(readOnly = true)
    public AuditDetailsDto getAuditDetails(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + id));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        String creadoPorNombre = "Dato no disponible";
        if (usuario.getCreadoPor() != null) {
            creadoPorNombre = usuarioRepository.findById(usuario.getCreadoPor()).map(Usuario::getNombre).orElse("Usuario no encontrado");
        }

        String fechaCreacion = "Dato no disponible";
        if (usuario.getFechaCreacion() != null) {
            fechaCreacion = usuario.getFechaCreacion().format(formatter);
        }

        String modificadoPorNombre = "N/A";
        if (usuario.getModificadoPor() != null) {
            modificadoPorNombre = usuarioRepository.findById(usuario.getModificadoPor()).map(Usuario::getNombre).orElse("Usuario no encontrado");
        }

        String fechaModificacion = "N/A";
        if (usuario.getFechaModificacion() != null) {
            fechaModificacion = usuario.getFechaModificacion().format(formatter);
        }

        String ultimaAccion = usuario.getUltimaAccion() != null ? usuario.getUltimaAccion() : "N/A";

        return new AuditDetailsDto(creadoPorNombre, fechaCreacion, modificadoPorNombre, fechaModificacion, ultimaAccion);
    }
}
