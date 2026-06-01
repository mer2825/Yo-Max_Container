package com.example.acceso.controller;

import com.example.acceso.model.Cliente;
import com.example.acceso.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;
    private final RestTemplate restTemplate;

    private final String API_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjozNDYsImV4cCI6MTc2MDM3Njc5NH0.Ow-U3j-J2A2cIV5Z_X9lKs-kHGasITmsD9aDhGPm37Q";

    @Autowired
    public ClienteController(ClienteService clienteService, RestTemplate restTemplate) {
        this.clienteService = clienteService;
        this.restTemplate = restTemplate;
    }

    @GetMapping("/listar")
    public String listarClientes() {
        return "clientes";
    }

    @GetMapping("/api/listar")
    @ResponseBody
    public ResponseEntity<?> listarClientesApi(@RequestParam(required = false) String tipoDocumento) {
        List<Cliente> clientes;
        if (tipoDocumento != null && !tipoDocumento.isEmpty()) {
            clientes = clienteService.listarClientesPorTipoDocumento(tipoDocumento.toUpperCase());
        } else {
            // Ahora listarClientes() en el servicio ya filtra por estado != 2
            clientes = clienteService.listarClientes();
        }
        return ResponseEntity.ok(Map.of("success", true, "data", clientes));
    }

    @PostMapping("/api/guardar")
    @ResponseBody
    public ResponseEntity<?> guardarCliente(@RequestBody Cliente cliente) {
        // Validación de duplicados
        Optional<Cliente> clienteExistente = clienteService.findByNumeroDocumento(cliente.getNumeroDocumento());
        if (clienteExistente.isPresent() && (cliente.getId() == null || !clienteExistente.get().getId().equals(cliente.getId()))) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("success", false, "message", "Ya existe un cliente con el número de documento '" + cliente.getNumeroDocumento() + "'."));
        }

        try {
            clienteService.guardarCliente(cliente);
            return ResponseEntity.ok(Map.of("success", true, "message", "Cliente guardado correctamente"));
        } catch (DataIntegrityViolationException e) {
            // Este bloque se mantiene como una segunda barrera, por si acaso
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("success", false, "message", "Error de integridad de datos. Es posible que el número de documento ya exista."));
        }
    }

    @GetMapping("/api/buscar-o-crear")
    @ResponseBody
    public ResponseEntity<?> buscarOCrearCliente(@RequestParam String tipo, @RequestParam String numero,
                                                 @RequestParam(required = false, defaultValue = "false") boolean forceCreate) {
        // 1. Buscar en la base de datos local primero
        Optional<Cliente> clienteExistente = clienteService.findByNumeroDocumento(numero);
        if (clienteExistente.isPresent()) {
            System.out.println("DEBUG: Cliente encontrado en BD local: " + clienteExistente.get().getNombre());
            // Cliente encontrado localmente, devolvemos con isNewClient: false
            return ResponseEntity.ok(Map.of("success", true, "isNewClient", false, "cliente", clienteExistente.get()));
        }

        // 2. Si no existe localmente, buscar en la API externa
        String tipoForUrl = tipo.toLowerCase(); // Asegurarse de que 'tipo' sea minúscula para la URL
        String url = "https://miapi.cloud/v1/" + tipoForUrl + "/" + numero;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + API_TOKEN);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            System.out.println("DEBUG: Llamando a API externa: " + url);
            ResponseEntity<Map> apiResponse = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            System.out.println("DEBUG: API externa Status Code: " + apiResponse.getStatusCode());
            System.out.println("DEBUG: API externa Response Body: " + apiResponse.getBody());

            if (apiResponse.getStatusCode() == HttpStatus.OK && apiResponse.getBody() != null) {
                Map<String, Object> externalBody = apiResponse.getBody();
                System.out.println("DEBUG: Respuesta de API externa (body): " + externalBody);

                if (Boolean.TRUE.equals(externalBody.get("success"))) {
                    Map<String, Object> datos = (Map<String, Object>) externalBody.get("datos");

                    Cliente clienteNoRegistrado = new Cliente();
                    clienteNoRegistrado.setTipoDocumento(tipo.toUpperCase());
                    clienteNoRegistrado.setNumeroDocumento(numero);

                    if ("dni".equalsIgnoreCase(tipo)) {
                        String nombreCompleto = String.format("%s %s %s", datos.get("nombres"), datos.get("ape_paterno"), datos.get("ape_materno")).trim();
                        clienteNoRegistrado.setNombre(nombreCompleto);
                    } else { // ruc
                        clienteNoRegistrado.setNombre((String) datos.get("razon_social"));
                    }

                    if (datos.get("domiciliado") instanceof Map) {
                        clienteNoRegistrado.setDireccion((String) ((Map<?, ?>) datos.get("domiciliado")).get("direccion"));
                    }
                    System.out.println("DEBUG: Datos de cliente nuevo para frontend: " + clienteNoRegistrado);

                    if (forceCreate) {
                        // Si forceCreate es true, guardamos el cliente en la BD local
                        Cliente clienteGuardado = clienteService.guardarCliente(clienteNoRegistrado);
                        return ResponseEntity.ok(Map.of("success", true, "isNewClient", false, "cliente", clienteGuardado, "message", "Cliente registrado y asignado con éxito."));
                    } else {
                        // Si forceCreate es false, solo devolvemos los datos de la API externa sin guardar
                        return ResponseEntity.ok(Map.of("success", true, "isNewClient", true, "cliente", clienteNoRegistrado));
                    }

                } else {
                    String message = (String) externalBody.getOrDefault("message", "La API externa no encontró el documento.");
                    System.out.println("DEBUG: API externa respondió success:false. Mensaje: " + message);
                    // Devolvemos success: false para que el frontend sepa que no se encontró
                    return ResponseEntity.ok(Map.of("success", false, "message", message));
                }
            }
            System.out.println("DEBUG: API externa respondió con estado no OK o body nulo. Estado: " + apiResponse.getStatusCode());
            return ResponseEntity.ok(Map.of("success", false, "message", "Error inesperado de la API externa. Estado: " + apiResponse.getStatusCode()));

        } catch (HttpClientErrorException e) {
            System.err.println("ERROR: HttpClientErrorException al llamar a API externa: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            String userMessage;
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                userMessage = "Documento no encontrado en el servicio externo.";
            } else if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                userMessage = "Error de autenticación con la API externa. El token podría ser inválido o expirado.";
            } else {
                userMessage = "Error del servicio externo (Código: " + e.getStatusCode().value() + ").";
            }
            return ResponseEntity.ok(Map.of("success", false, "message", userMessage));
        } catch (ResourceAccessException e) {
            System.err.println("ERROR: ResourceAccessException al llamar a API externa: " + e.getMessage());
            return ResponseEntity.ok(Map.of("success", false, "message", "No se pudo conectar con el servicio externo. Verifique su conexión o la disponibilidad del servicio."));
        } catch (Exception e) {
            System.err.println("ERROR: Excepción inesperada al buscar cliente: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "Error interno del servidor al procesar la búsqueda."));
        }
    }

    @PostMapping("/api/crear-cliente-desde-venta")
    @ResponseBody
    public ResponseEntity<?> crearClienteDesdeVenta(@RequestBody Cliente cliente) {
        try {
            Cliente clienteGuardado = clienteService.guardarCliente(cliente);
            return ResponseEntity.ok(Map.of("success", true, "cliente", clienteGuardado, "message", "Cliente registrado con éxito."));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("success", false, "message", "Ya existe un cliente con este número de documento."));
        } catch (Exception e) {
            System.err.println("ERROR: Excepción al crear cliente desde venta: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "Error interno del servidor al registrar el cliente."));
        }
    }

    @DeleteMapping("/api/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarCliente(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Ahora el servicio realiza el borrado lógico (estado = 2)
            clienteService.eliminarCliente(id);
            response.put("success", true);
            response.put("message", "Cliente eliminado lógicamente (estado = 2) correctamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al eliminar cliente: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/api/cambiar-estado/{id}")
    @ResponseBody
    public ResponseEntity<?> cambiarEstadoCliente(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            return clienteService.cambiarEstadoCliente(id)
                    .map(cliente -> {
                        response.put("success", true);
                        response.put("message", "Estado del cliente actualizado");
                        return ResponseEntity.ok(response);
                    })
                    .orElseGet(() -> {
                        response.put("success", false);
                        response.put("message", "Cliente no encontrado");
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                    });
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al cambiar estado: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> obtenerCliente(@PathVariable Long id) {
        return clienteService.obtenerClientePorId(id)
                .map(cliente -> ResponseEntity.ok(Map.of("success", true, "data", cliente)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Cliente no encontrado")));
    }

    @GetMapping("/api/verificar/{documento}")
    @ResponseBody
    public ResponseEntity<?> verificarCliente(@PathVariable String documento) {
        Optional<Cliente> cliente = clienteService.findByNumeroDocumento(documento);
        Map<String, Boolean> response = new HashMap<>();
        response.put("isValid", cliente.isPresent());
        return ResponseEntity.ok(response);
    }
}
