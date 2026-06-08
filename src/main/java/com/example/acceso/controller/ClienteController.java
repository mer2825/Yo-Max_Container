package com.example.acceso.controller;

import com.example.acceso.model.Cliente;
import com.example.acceso.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    @Autowired
    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
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
            clientes = clienteService.listarClientes();
        }
        return ResponseEntity.ok(Map.of("success", true, "data", clientes));
    }

    @PostMapping("/api/guardar")
    @ResponseBody
    public ResponseEntity<?> guardarCliente(@RequestBody Cliente cliente) {
        Optional<Cliente> clienteExistente = clienteService.findByNumeroDocumento(cliente.getNumeroDocumento());
        if (clienteExistente.isPresent() && (cliente.getId() == null || !clienteExistente.get().getId().equals(cliente.getId()))) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("success", false, "message", "Ya existe un cliente con el número de documento '" + cliente.getNumeroDocumento() + "'."));
        }

        try {
            clienteService.guardarCliente(cliente);
            return ResponseEntity.ok(Map.of("success", true, "message", "Cliente guardado correctamente"));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("success", false, "message", "Error de integridad de datos. Es posible que el número de documento ya exista."));
        }
    }

    @GetMapping("/api/consultar-dni/{dni}")
    @ResponseBody
    public ResponseEntity<?> consultarDniEndpoint(@PathVariable String dni) {
        Map<String, Object> result = clienteService.consultarDni(dni);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
    }

    @DeleteMapping("/api/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarCliente(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
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

    // ===================================================================
    // NUEVO ENDPOINT: Ya conectado a tu lógica de base de datos y API Cloud
    // ===================================================================
    @GetMapping("/api/buscar-o-crear")
    @ResponseBody
    public ResponseEntity<?> buscarOCrearCliente(@RequestParam String numeroDocumento) {
        // Ejecuta el método que implementa la búsqueda local y externa
        Map<String, Object> resultado = clienteService.consultarDni(numeroDocumento);

        if (Boolean.TRUE.equals(resultado.get("success"))) {
            return ResponseEntity.ok(resultado);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resultado);
        }
    }

    // ===================================================================
    // ENDPOINTS CON VARIABLES DINÁMICAS: Abajo para que no interfieran
    // ===================================================================
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