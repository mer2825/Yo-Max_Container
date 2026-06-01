package com.example.acceso.service;

import com.example.acceso.dto.AuditDetailsDto;
import com.example.acceso.model.Categoria;
import com.example.acceso.model.Usuario;
import com.example.acceso.repository.CategoriaRepository;
import com.example.acceso.repository.ProductoRepository;
import com.example.acceso.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository; // Para verificar productos asociados
    private final UsuarioRepository usuarioRepository; // Para obtener nombres de usuarios

    @Autowired
    public CategoriaServiceImpl(CategoriaRepository categoriaRepository, ProductoRepository productoRepository, UsuarioRepository usuarioRepository) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Categoria> listarCategorias() {
        return categoriaRepository.findAllByEstadoNot(2);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Categoria> listarCategoriasActivas() {
        return categoriaRepository.findByEstado(1);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Categoria> obtenerCategoriaPorId(Long id) {
        return categoriaRepository.findById(id);
    }

    @Override
    @Transactional
    public Categoria guardarCategoria(Categoria categoria) {
        boolean isNew = categoria.getId() == null;

        if (categoria.getNombre() == null || categoria.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la categoría es obligatorio");
        }

        categoria.setNombre(categoria.getNombre().trim());

        // --- Duplicate name validation ---
        Optional<Categoria> existingCategory = categoriaRepository.findByNombreIgnoreCaseAndEstadoNot(categoria.getNombre(), 2);

        if (isNew) {
            if (existingCategory.isPresent()) {
                throw new IllegalArgumentException("No se puede crear esta categoría, porque ya existe una categoría con el mismo nombre.");
            }
            categoria.setEstado(1);
            categoria.setUltimaAccion("Creación");
        } else {
            if (existingCategory.isPresent() && !existingCategory.get().getId().equals(categoria.getId())) {
                throw new IllegalArgumentException("No se puede actualizar esta categoría, porque ya existe otra categoría con el mismo nombre.");
            }
            // Asegurarse de que la categoría existe antes de actualizar
            obtenerCategoriaPorId(categoria.getId())
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada para actualizar"));
            categoria.setUltimaAccion("Edición");
        }
        return categoriaRepository.save(categoria);
    }

    @Override
    @Transactional
    public void eliminarCategoria(Long id) {
        obtenerCategoriaPorId(id).ifPresent(categoria -> {
            if (productoRepository.existsByCategoriaAndEstadoNot(categoria, 2)) {
                throw new IllegalStateException("No se puede eliminar la categoría '" + categoria.getNombre() + "' porque tiene productos activos o inactivos asociados.");
            }
            categoria.setEstado(2);
            categoria.setUltimaAccion("Eliminación");
            categoriaRepository.save(categoria);
        });
    }

    @Override
    @Transactional
    public Optional<Categoria> cambiarEstadoCategoria(Long id) {
        return obtenerCategoriaPorId(id).map(categoria -> {
            if (categoria.getEstado() == 1) {
                categoria.setEstado(0);
                categoria.setUltimaAccion("Inactivación");
            } else if (categoria.getEstado() == 0) {
                categoria.setEstado(1);
                categoria.setUltimaAccion("Activación");
            }
            return categoriaRepository.save(categoria);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public AuditDetailsDto getAuditDetails(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada con ID: " + id));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        String creadoPorNombre = "Dato no disponible";
        if (categoria.getCreadoPor() != null) {
            creadoPorNombre = usuarioRepository.findById(categoria.getCreadoPor()).map(Usuario::getNombre).orElse("Usuario no encontrado");
        }

        String fechaCreacion = "Dato no disponible";
        if (categoria.getFechaCreacion() != null) {
            fechaCreacion = categoria.getFechaCreacion().format(formatter);
        }

        String modificadoPorNombre = "N/A";
        if (categoria.getModificadoPor() != null) {
            modificadoPorNombre = usuarioRepository.findById(categoria.getModificadoPor()).map(Usuario::getNombre).orElse("Usuario no encontrado");
        }

        String fechaModificacion = "N/A";
        if (categoria.getFechaModificacion() != null) {
            fechaModificacion = categoria.getFechaModificacion().format(formatter);
        }

        String ultimaAccion = categoria.getUltimaAccion() != null ? categoria.getUltimaAccion() : "N/A";

        return new AuditDetailsDto(creadoPorNombre, fechaCreacion, modificadoPorNombre, fechaModificacion, ultimaAccion);
    }
}