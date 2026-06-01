package com.example.acceso.service;

import com.example.acceso.model.Cliente;

import java.util.List;
import java.util.Optional;

public interface ClienteService {
    List<Cliente> listarClientes(); // Solo clientes no eliminados (estado != 2)
    List<Cliente> listarClientesActivos(); // Solo clientes activos (estado = 1)
    Optional<Cliente> obtenerClientePorId(Long id);
    Cliente guardarCliente(Cliente cliente);
    void eliminarCliente(Long id); // Borrado lógico (estado = 2)
    Optional<Cliente> cambiarEstadoCliente(Long id); // Alterna entre 0 y 1
    long contarClientes();
    
    // Ahora este método buscará explícitamente clientes no eliminados
    Optional<Cliente> findByNumeroDocumento(String numeroDocumento);
    
    Optional<Cliente> findByNombre(String nombre);

    // Nuevo método para listar clientes por tipo de documento
    List<Cliente> listarClientesPorTipoDocumento(String tipoDocumento);
}
