package com.example.acceso.service;

import com.example.acceso.model.Cliente;
import com.example.acceso.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final RestTemplate restTemplate;

    @Value("${api.external.token}")
    private String apiToken;

    @Autowired
    public ClienteServiceImpl(ClienteRepository clienteRepository, RestTemplate restTemplate) {
        this.clienteRepository = clienteRepository;
        this.restTemplate = restTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cliente> listarClientes() {
        return clienteRepository.findAllByEstadoNot(2);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cliente> listarClientesActivos() {
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
        if (cliente.getId() == null) {
            cliente.setEstado(1);
        }
        if ("DNI".equalsIgnoreCase(cliente.getTipoDocumento())) {
            cliente.setDireccion(null);
        }
        return clienteRepository.save(cliente);
    }

    @Override
    @Transactional
    public void eliminarCliente(Long id) {
        obtenerClientePorId(id).ifPresent(cliente -> {
            cliente.setEstado(2);
            clienteRepository.save(cliente);
        });
    }

    @Override
    @Transactional
    public Optional<Cliente> cambiarEstadoCliente(Long id) {
        return obtenerClientePorId(id).map(cliente -> {
            if (cliente.getEstado() == 1) {
                cliente.setEstado(0);
            } else if (cliente.getEstado() == 0) {
                cliente.setEstado(1);
            }
            return clienteRepository.save(cliente);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public long contarClientes() {
        return clienteRepository.findAllByEstadoNot(2).size();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cliente> findByNumeroDocumento(String numeroDocumento) {
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

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> consultarDni(String dni) {
        Map<String, Object> response = new HashMap<>();

        // 1. Buscar en la base de datos local
        Optional<Cliente> clienteExistente = findByNumeroDocumento(dni);
        if (clienteExistente.isPresent()) {
            response.put("success", true);
            response.put("source", "local");
            response.put("data", clienteExistente.get());
            return response;
        }

        // 2. Si no existe, buscar en la API externa
        String url = "https://miapi.cloud/v1/dni/" + dni;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> apiResponse = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (apiResponse.getStatusCode() == HttpStatus.OK && apiResponse.getBody() != null) {
                Map<String, Object> externalBody = apiResponse.getBody();
                if (Boolean.TRUE.equals(externalBody.get("success"))) {
                    Map<String, Object> datos = (Map<String, Object>) externalBody.get("datos");
                    String nombreCompleto = String.format("%s %s %s", datos.get("nombres"), datos.get("ape_paterno"), datos.get("ape_materno")).trim();
                    
                    Cliente clienteNoRegistrado = new Cliente();
                    clienteNoRegistrado.setTipoDocumento("DNI");
                    clienteNoRegistrado.setNumeroDocumento(dni);
                    clienteNoRegistrado.setNombre(nombreCompleto);

                    response.put("success", true);
                    response.put("source", "external");
                    response.put("data", clienteNoRegistrado);
                    return response;
                }
            }
        } catch (HttpClientErrorException e) {
            // Si la API externa da un error (ej. 404 Not Found), lo consideramos como no encontrado
            System.err.println("API externa no encontró el DNI o hubo un error: " + e.getMessage());
        } catch (ResourceAccessException e) {
            // Error de conexión
            System.err.println("Error de conexión con la API externa: " + e.getMessage());
        }
        
        // 3. Si no se encontró en ninguna parte
        response.put("success", false);
        response.put("message", "DNI no encontrado en la base de datos local ni en el servicio externo.");
        return response;
    }
}
