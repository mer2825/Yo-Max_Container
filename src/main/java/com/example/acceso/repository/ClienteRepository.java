package com.example.acceso.repository;

import com.example.acceso.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    
    // Método original (puede encontrar clientes eliminados)
    Optional<Cliente> findByNumeroDocumento(String numeroDocumento);

    // ¡NUEVO Y MEJORADO! Busca por documento y se asegura de que no esté eliminado.
    Optional<Cliente> findByNumeroDocumentoAndEstadoNot(String numeroDocumento, Integer estado);

    Optional<Cliente> findByNombre(String nombre);

    // Nuevo método para buscar clientes por tipo de documento
    List<Cliente> findByTipoDocumento(String tipoDocumento);

    // Nuevo método para buscar clientes que no estén eliminados (estado != 2)
    List<Cliente> findAllByEstadoNot(Integer estado);

    // Nuevo método para buscar clientes por estado
    List<Cliente> findByEstado(Integer estado);
}
