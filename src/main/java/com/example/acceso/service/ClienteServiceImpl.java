package com.example.acceso.service;

import com.example.acceso.model.Cliente;
import com.example.acceso.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
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

    @Value("${miapi.base-url:https://miapi.cloud}")
    private String miapiBaseUrl;

    @Value("${miapi.token:}")
    private String miapiToken;

    @Value("${api.external.token:}")
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
        // Si es un nuevo cliente (id es null)
        if (cliente.getId() == null) {
            // Buscar si ya existe un cliente con ese numeroDocumento, incluyendo los eliminados lógicamente
            Optional<Cliente> existingClientAnyStatus = clienteRepository.findByNumeroDocumento(cliente.getNumeroDocumento());

            if (existingClientAnyStatus.isPresent()) {
                Cliente foundClient = existingClientAnyStatus.get();
                // Si el cliente encontrado está lógicamente eliminado (estado = 2), lo reactivamos
                if (foundClient.getEstado() == 2) {
                    foundClient.setEstado(1); // Reactivar (estado = 1)
                    foundClient.setTipoDocumento(cliente.getTipoDocumento());
                    foundClient.setNombre(cliente.getNombre());
                    foundClient.setDireccion(cliente.getDireccion());
                    foundClient.setTelefono(cliente.getTelefono());
                    foundClient.setEmail(cliente.getEmail());

                    // Asegurarse de que los clientes DNI no tengan dirección
                    if ("DNI".equalsIgnoreCase(foundClient.getTipoDocumento())) {
                        foundClient.setDireccion(null);
                    }
                    return clienteRepository.save(foundClient); // Guardar el cliente reactivado
                }
                // Si el cliente existe y no está eliminado lógicamente (estado 0 o 1),
                // lanzamos una excepción para evitar violación de la restricción unique.
                // Esto previene el error "Could not commit JPA transaction".
                throw new IllegalArgumentException("Ya existe un cliente con el número de documento '" + cliente.getNumeroDocumento() + "'.");
            } else {
                // No se encontró ningún cliente con ese numeroDocumento (en ningún estado),
                // así que es un cliente completamente nuevo.
                cliente.setEstado(1); // Establecer estado activo para el nuevo cliente
            }
        }

        // Asegurarse de que los clientes DNI no tengan dirección (aplica a nuevos y actualizados)
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

        if (dni == null || dni.isBlank()) {
            response.put("success", false);
            response.put("message", "El número de documento es requerido.");
            return response;
        }

        String dniNormalizado = dni.replaceAll("\\D", "");
        if (dniNormalizado.isBlank()) {
            response.put("success", false);
            response.put("message", "El número de documento debe contener solo dígitos.");
            return response;
        }

        Optional<Cliente> clienteExistente = findByNumeroDocumento(dniNormalizado);
        if (clienteExistente.isPresent()) {
            response.put("success", true);
            response.put("source", "local");
            response.put("data", clienteExistente.get());
            return response;
        }

        String url = buildMiApiUrl("/v1/dni/" + dniNormalizado);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (miapiToken != null && !miapiToken.isBlank()) {
            headers.setBearerAuth(miapiToken);
        } else if (apiToken != null && !apiToken.isBlank()) {
            headers.setBearerAuth(apiToken);
        }
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> apiResponse = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (apiResponse.getStatusCode() == HttpStatus.OK && apiResponse.getBody() != null) {
                Map<String, Object> externalBody = apiResponse.getBody();
                Map<String, Object> dataBody = extractNestedMap(externalBody, "data", "datos", "result", "resultado");

                String nombres = extractString(dataBody, externalBody, "nombres", "nombre", "fullName", "full_name", "nombres_completos", "nombres_completo");
                String apePaterno = extractString(dataBody, externalBody, "apellidoPaterno", "ape_paterno", "apellido_paterno", "apellido-paterno");
                String apeMaterno = extractString(dataBody, externalBody, "apellidoMaterno", "ape_materno", "apellido_materno", "apellido-materno");

                String nombreCompleto = String.format("%s %s %s", nombres, apePaterno, apeMaterno).trim();
                if (nombreCompleto.isBlank()) {
                    nombreCompleto = extractString(dataBody, externalBody, "nombre", "razonSocial", "razon_social", "fullName", "full_name");
                }

                if (nombreCompleto.isBlank()) {
                    response.put("success", true);
                    response.put("source", "fallback");
                    response.put("message", "No se encontraron datos externos, se creó un registro provisional para el DNI.");
                    response.put("data", crearClienteProvisional(dniNormalizado));
                    return response;
                }

                Cliente clienteNoRegistrado = new Cliente();
                clienteNoRegistrado.setTipoDocumento("DNI");
                clienteNoRegistrado.setNumeroDocumento(dniNormalizado);
                clienteNoRegistrado.setNombre(nombreCompleto);

                response.put("success", true);
                response.put("source", "external");
                response.put("data", clienteNoRegistrado);
                return response;
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            System.err.println("API MiAPI DNI devolvió error para DNI " + dniNormalizado + ": " + e.getMessage());
        } catch (ResourceAccessException e) {
            System.err.println("Error de conexión con MiAPI DNI: " + e.getMessage());
        }

        response.put("success", true);
        response.put("source", "fallback");
        response.put("message", "No se pudo validar el DNI con MiAPI; se creó un registro provisional para seguir con la venta.");
        response.put("data", crearClienteProvisional(dniNormalizado));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> consultarRuc(String ruc) {
        Map<String, Object> response = new HashMap<>();

        if (ruc == null || ruc.isBlank()) {
            response.put("success", false);
            response.put("message", "El número de documento es requerido.");
            return response;
        }

        String rucNormalizado = ruc.replaceAll("\\D", "");
        if (rucNormalizado.isBlank()) {
            response.put("success", false);
            response.put("message", "El número de documento debe contener solo dígitos.");
            return response;
        }

        Optional<Cliente> clienteExistente = findByNumeroDocumento(rucNormalizado);
        if (clienteExistente.isPresent()) {
            response.put("success", true);
            response.put("source", "local");
            response.put("data", clienteExistente.get());
            return response;
        }

        String url = buildMiApiUrl("/v1/ruc/" + rucNormalizado);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (miapiToken != null && !miapiToken.isBlank()) {
            headers.setBearerAuth(miapiToken);
        } else if (apiToken != null && !apiToken.isBlank()) {
            headers.setBearerAuth(apiToken);
        }
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> apiResponse = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (apiResponse.getStatusCode() == HttpStatus.OK && apiResponse.getBody() != null) {
                Map<String, Object> externalBody = apiResponse.getBody();
                Map<String, Object> dataBody = extractNestedMap(externalBody, "data", "datos", "result", "resultado");

                String razonSocial = extractString(dataBody, externalBody,
                        "razonSocial", "razon_social", "razon_social_empresa", "nombre", "nombre_comercial", "companyName", "fullName", "full_name");
                String direccionFiscal = extractString(dataBody, externalBody,
                        "direccion", "direccionFiscal", "direccion_fiscal", "domicilioFiscal", "direccion_domicilio", "direccion_domic");

                if (razonSocial.isBlank()) {
                    response.put("success", true);
                    response.put("source", "fallback");
                    response.put("message", "No se encontraron datos externos para RUC; se creó un registro provisional para seguir con la venta.");
                    response.put("data", crearClienteProvisionalRuc(rucNormalizado));
                    return response;
                }

                Cliente clienteNoRegistrado = new Cliente();
                clienteNoRegistrado.setTipoDocumento("RUC");
                clienteNoRegistrado.setNumeroDocumento(rucNormalizado);
                clienteNoRegistrado.setNombre(razonSocial);
                clienteNoRegistrado.setRazonSocial(razonSocial);
                clienteNoRegistrado.setDireccion(direccionFiscal.isBlank() ? null : direccionFiscal);
                clienteNoRegistrado.setDireccionFiscal(direccionFiscal.isBlank() ? null : direccionFiscal);

                response.put("success", true);
                response.put("source", "external");
                response.put("data", clienteNoRegistrado);
                return response;
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            System.err.println("API MiAPI RUC devolvió error para RUC " + rucNormalizado + ": " + e.getMessage());
        } catch (ResourceAccessException e) {
            System.err.println("Error de conexión con MiAPI RUC: " + e.getMessage());
        }

        response.put("success", true);
        response.put("source", "fallback");
        response.put("message", "No se pudo validar el RUC con MiAPI; se creó un registro provisional para seguir con la venta.");
        response.put("data", crearClienteProvisionalRuc(rucNormalizado));
        return response;
    }

    private Cliente crearClienteProvisionalRuc(String ruc) {
        Cliente cliente = new Cliente();
        cliente.setTipoDocumento("RUC");
        cliente.setNumeroDocumento(ruc);
        cliente.setNombre("RUC " + ruc);
        cliente.setRazonSocial("RUC " + ruc);
        cliente.setEstado(1);
        return cliente;
    }

    private String extractString(Map<String, Object> primary, Map<String, Object> secondary, String... keys) {
        for (String key : keys) {
            if (primary != null && primary.containsKey(key) && primary.get(key) != null) {
                return primary.get(key).toString().trim();
            }
            if (secondary != null && secondary.containsKey(key) && secondary.get(key) != null) {
                return secondary.get(key).toString().trim();
            }
        }
        return "";
    }

    private Map<String, Object> extractNestedMap(Map<String, Object> main, String... keys) {
        if (main == null) {
            return null;
        }
        for (String key : keys) {
            Object value = main.get(key);
            if (value instanceof Map) {
                return (Map<String, Object>) value;
            }
        }
        return main;
    }

    private String buildMiApiUrl(String path) {
        String base = (miapiBaseUrl != null && !miapiBaseUrl.isBlank()) ? miapiBaseUrl : "https://miapi.cloud";
        if (path.startsWith("/")) {
            return base + path;
        }
        return base + "/" + path;
    }

    private Cliente crearClienteProvisional(String dni) {
        Cliente cliente = new Cliente();
        cliente.setTipoDocumento("DNI");
        cliente.setNumeroDocumento(dni);
        cliente.setNombre("Cliente " + dni);
        cliente.setEstado(1);
        return cliente;
    }
}