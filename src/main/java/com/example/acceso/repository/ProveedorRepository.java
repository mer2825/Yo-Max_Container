package com.example.acceso.repository;

import com.example.acceso.model.Proveedor; // Importamos el nuevo modelo Proveedor
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    // 1. Métodos para encontrar proveedores por su Identificación Fiscal (RUC/NIF), que debe ser única.
    Optional<Proveedor> findByRucNif(String rucNif);

    // 2. Métodos para verificar la existencia del Proveedor por RUC/NIF
    boolean existsByRucNif(String rucNif);

    // 3. Método para encontrar proveedores por su Nombre (aunque no es único, es útil para búsquedas).
    Optional<Proveedor> findByNombre(String nombre);

    // 4. Métodos para el borrado lógico (filtrado por estado)

    /**
     * Encuentra todos los proveedores cuyo estado NO es el especificado.
     * Útil para listar activos (si se pasa 'false') o inactivos (si se pasa 'true').
     * @param estado El estado a excluir.
     * @return Lista de proveedores.
     */
    List<Proveedor> findAllByEstadoNot(Boolean estado);

    /**
     * Encuentra todos los proveedores por su estado.
     * Úparam estado 'true' para Activos, 'false' para Inactivos.
     * @return Lista de proveedores con el estado especificado.
     */
    List<Proveedor> findByEstado(Boolean estado);

    /**
     * Cuenta cuántos proveedores tienen un estado diferente al especificado.
     * @param estado El estado a excluir.
     * @return El conteo.
     */
    long countByEstadoNot(Boolean estado);
}
