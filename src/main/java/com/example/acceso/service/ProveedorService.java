package com.example.acceso.service;

import com.example.acceso.model.Proveedor; // Importamos el nuevo modelo Proveedor
import com.example.acceso.repository.ProveedorRepository; // Usamos el repositorio ProveedorRepository
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;

    public ProveedorService(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    // ----------------------------------------------------------------------
    // Listado y Conteo
    // ----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Proveedor> listarProveedores() {
        // Lista TODOS los proveedores (activos e inactivos).
        return proveedorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Proveedor> listarProveedoresActivos() {
        // Lista SOLO los proveedores con estado = true (activos).
        return proveedorRepository.findByEstado(true);
    }


    // ----------------------------------------------------------------------
    // Guardar/Actualizar Proveedor
    // ----------------------------------------------------------------------

    @Transactional
    public Proveedor guardarProveedor(Proveedor proveedor) {
        try {
            // **Validaciones Específicas de Proveedor**
            if (proveedor.getNombre() == null || proveedor.getNombre().trim().isEmpty()) {
                throw new IllegalArgumentException("El nombre del proveedor es obligatorio");
            }
            if (proveedor.getRucNif() == null || proveedor.getRucNif().trim().isEmpty()) {
                throw new IllegalArgumentException("El RUC/NIF del proveedor es obligatorio");
            }
            if (proveedor.getPlazoEntrega() == null || proveedor.getPlazoEntrega() < 0) {
                throw new IllegalArgumentException("El plazo de entrega no puede ser negativo");
            }
            if (proveedor.getMoneda() == null || proveedor.getMoneda().trim().isEmpty()) {
                throw new IllegalArgumentException("La moneda de transacción es obligatoria");
            }

            // Normalizar datos
            proveedor.setNombre(proveedor.getNombre().trim());
            proveedor.setRucNif(proveedor.getRucNif().trim());
            // Normalización para otros campos de texto...

            // Validación de unicidad de RUC/NIF
            Optional<Proveedor> proveedorExistenteRuc = proveedorRepository.findByRucNif(proveedor.getRucNif());

            // Si el RUC/NIF ya existe Y no pertenece al proveedor que estamos actualizando (si tiene ID)
            if (proveedorExistenteRuc.isPresent() &&
                    (proveedor.getIdProveedor() == null || !proveedorExistenteRuc.get().getIdProveedor().equals(proveedor.getIdProveedor()))) {
                throw new IllegalArgumentException("Ya existe un proveedor con este RUC/NIF: " + proveedor.getRucNif());
            }

            // Manejo de creación/actualización
            if (proveedor.getIdProveedor() != null) {
                // Proveedor existente - actualización
                Optional<Proveedor> proveedorExistente = obtenerProveedorPorId(proveedor.getIdProveedor());
                if (!proveedorExistente.isPresent()) {
                    throw new IllegalArgumentException("Proveedor no encontrado para actualizar");
                }
                // Si el estado no se proporciona en la actualización, mantenemos el actual
                if (proveedor.getEstado() == null) {
                    proveedor.setEstado(proveedorExistente.get().getEstado());
                }
            } else {
                // Nuevo proveedor: Asignar estado activo por defecto
                proveedor.setEstado(true);
            }

            return proveedorRepository.save(proveedor);

        } catch (DataIntegrityViolationException e) {
            // Manejar violaciones de restricciones únicas o de datos
            String message = e.getMessage().toLowerCase();
            if (message.contains("rucnif") || message.contains("unique constraint")) {
                throw new IllegalArgumentException("Error de integridad: El RUC/NIF ya está registrado.");
            } else {
                throw new IllegalArgumentException("Error de integridad de datos: " + e.getMessage());
            }
        } catch (IllegalArgumentException e) {
            throw e; // Relanzar excepciones de validación
        } catch (Exception e) {
            throw new RuntimeException("Error al guardar el proveedor: " + e.getMessage(), e);
        }
    }

    // ----------------------------------------------------------------------
    // Obtener y Eliminar (Borrado Lógico)
    // ----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Optional<Proveedor> obtenerProveedorPorId(Long id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }
        return proveedorRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Proveedor> findByRucNif(String rucNif) {
        if (rucNif == null || rucNif.trim().isEmpty()) {
            return Optional.empty();
        }
        return proveedorRepository.findByRucNif(rucNif.trim());
    }

    @Transactional
    public void eliminarProveedor(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID de proveedor inválido");
        }

        // Borrado lógico: cambiamos el estado a 'false'
        Proveedor proveedor = obtenerProveedorPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado"));

        proveedor.setEstado(false);
        proveedorRepository.save(proveedor);
    }

    @Transactional
    public Optional<Proveedor> cambiarEstadoProveedor(Long id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }

        return obtenerProveedorPorId(id).map(proveedor -> {
            // Invierte el estado: true <-> false (activo <-> inactivo)
            proveedor.setEstado(!proveedor.getEstado());
            return proveedorRepository.save(proveedor);
        });
    }

    @Transactional(readOnly = true)
    public boolean existeProveedorPorRucNif(String rucNif) {
        if (rucNif == null || rucNif.trim().isEmpty()) {
            return false;
        }
        return proveedorRepository.existsByRucNif(rucNif.trim());
    }
}
