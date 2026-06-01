package com.example.acceso.service;

import com.example.acceso.model.Cliente;
import com.example.acceso.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;

    @Autowired
    public ClienteServiceImpl(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cliente> listarClientes() {
        // Solo listar clientes que no estén eliminados lógicamente (estado != 2)
        return clienteRepository.findAllByEstadoNot(2);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cliente> listarClientesActivos() {
        // Listar solo clientes con estado 1 (activos)
        return clienteRepository.findByEstado(1);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cliente> obtenerClientePorId(Long id) {
        return clienteRepository.findById(id);
    }

    @Override
    @Transactional
    public Cliente guardarCliente(Cliente cliente) {
        // Si es un cliente nuevo, establecer estado 1 (activo) por defecto
        if (cliente.getId() == null) {
            cliente.setEstado(1);
        }

        // Regla de negocio: Si el tipo de documento es DNI, la dirección debe ser nula.
        if ("DNI".equalsIgnoreCase(cliente.getTipoDocumento())) {
            cliente.setDireccion(null);
        }

        return clienteRepository.save(cliente);
    }

    @Override
    @Transactional
    public void eliminarCliente(Long id) {
        // Borrado lógico: cambiamos el estado a 2
        obtenerClientePorId(id).ifPresent(cliente -> {
            cliente.setEstado(2); // 2 significa "eliminado"
            clienteRepository.save(cliente);
        });
    }

    @Override
    @Transactional
    public Optional<Cliente> cambiarEstadoCliente(Long id) {
        return obtenerClientePorId(id).map(cliente -> {
            // Solo alterna entre 0 (inactivo) y 1 (activo)
            if (cliente.getEstado() == 1) {
                cliente.setEstado(0); // Desactivar
            } else if (cliente.getEstado() == 0) {
                cliente.setEstado(1); // Activar
            }
            // No se hace nada si el estado es 2 (eliminado)
            return clienteRepository.save(cliente);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public long contarClientes() {
        // Contar solo clientes que no estén eliminados lógicamente (estado != 2)
        return clienteRepository.findAllByEstadoNot(2).size();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cliente> findByNumeroDocumento(String numeroDocumento) {
        // AHORA USA EL MÉTODO SEGURO: Busca por documento y excluye a los eliminados (estado 2)
        return clienteRepository.findByNumeroDocumentoAndEstadoNot(numeroDocumento, 2);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cliente> findByNombre(String nombre) {
        return clienteRepository.findByNombre(nombre);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cliente> listarClientesPorTipoDocumento(String tipoDocumento) {
        return clienteRepository.findByTipoDocumento(tipoDocumento);
    }
}
