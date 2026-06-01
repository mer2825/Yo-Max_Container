package com.example.acceso.service;

import com.example.acceso.dto.AuditDetailsDto;
import com.example.acceso.model.Perfil;
import com.example.acceso.model.Usuario;
import com.example.acceso.repository.PerfilRepository;
import com.example.acceso.repository.UsuarioRepository; // Importar UsuarioRepository
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class PerfilServiceImpl implements PerfilService {

    private final PerfilRepository perfilRepository;
    private final UsuarioRepository usuarioRepository; // Inyectar UsuarioRepository

    @Autowired
    public PerfilServiceImpl(PerfilRepository perfilRepository, UsuarioRepository usuarioRepository) {
        this.perfilRepository = perfilRepository;
        this.usuarioRepository = usuarioRepository; // Asignar UsuarioRepository
    }

    @Override
    @Transactional(readOnly = true)
    public List<Perfil> listarPerfiles() {
        // Solo listar perfiles que no estén eliminados lógicamente (estado 2)
        return perfilRepository.findAllByEstadoNot(2);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Perfil> obtenerPerfilPorId(Long id) {
        return perfilRepository.findById(id);
    }

    @Override
    @Transactional
    public Perfil guardarPerfil(Perfil perfil) {
        boolean isNew = perfil.getId() == null;
        // Si es un perfil nuevo, establecer estado 1 (activo) por defecto
        if (isNew) {
            perfil.setEstado(1);
            perfil.setUltimaAccion("Creación");
        } else {
            perfil.setUltimaAccion("Edición");
        }
        return perfilRepository.save(perfil);
    }

    @Override
    @Transactional
    public void eliminarPerfil(Long id) {
        obtenerPerfilPorId(id).ifPresent(perfil -> {
            // Verificar si hay usuarios asociados que NO estén en estado 2 (eliminado lógicamente)
            if (usuarioRepository.existsByPerfilAndEstadoNot(perfil, 2)) {
                throw new IllegalStateException("No se puede eliminar el perfil '" + perfil.getNombre() + "' porque tiene usuarios activos o inactivos asociados.");
            }
            perfil.setEstado(2); // Borrado lógico: estado 2 (eliminado)
            perfil.setUltimaAccion("Eliminación");
            perfilRepository.save(perfil);
        });
    }

    @Override
    @Transactional
    public Optional<Perfil> cambiarEstadoPerfil(Long id) {
        return obtenerPerfilPorId(id).map(perfil -> {
            // Solo alterna entre 0 (inactivo) y 1 (activo)
            if (perfil.getEstado() == 1) {
                perfil.setEstado(0); // Desactivar
                perfil.setUltimaAccion("Inactivación");
            } else if (perfil.getEstado() == 0) {
                perfil.setEstado(1); // Activar
                perfil.setUltimaAccion("Activación");
            }
            // No se hace nada si el estado es 2 (eliminado)
            return perfilRepository.save(perfil);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Perfil> listarPerfilesActivos() {
        // Listar solo perfiles con estado 1 (activos)
        return perfilRepository.findByEstado(1);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditDetailsDto getAuditDetails(Long id) {
        Perfil perfil = perfilRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Perfil no encontrado con ID: " + id));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        String creadoPorNombre = "Dato no disponible";
        if (perfil.getCreadoPor() != null) {
            creadoPorNombre = usuarioRepository.findById(perfil.getCreadoPor()).map(Usuario::getNombre).orElse("Usuario no encontrado");
        }

        String fechaCreacion = "Dato no disponible";
        if (perfil.getFechaCreacion() != null) {
            fechaCreacion = perfil.getFechaCreacion().format(formatter);
        }

        String modificadoPorNombre = "N/A";
        if (perfil.getModificadoPor() != null) {
            modificadoPorNombre = usuarioRepository.findById(perfil.getModificadoPor()).map(Usuario::getNombre).orElse("Usuario no encontrado");
        }

        String fechaModificacion = "N/A";
        if (perfil.getFechaModificacion() != null) {
            fechaModificacion = perfil.getFechaModificacion().format(formatter);
        }

        String ultimaAccion = perfil.getUltimaAccion() != null ? perfil.getUltimaAccion() : "N/A";

        return new AuditDetailsDto(creadoPorNombre, fechaCreacion, modificadoPorNombre, fechaModificacion, ultimaAccion);
    }
}