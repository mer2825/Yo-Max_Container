package com.example.acceso.controller;

import com.example.acceso.model.PedidoWeb;
import com.example.acceso.model.EstadoPedidoWeb;
import com.example.acceso.model.Usuario;
import com.example.acceso.model.Empresa;
import com.example.acceso.service.PedidoWebService;
import com.example.acceso.service.EmpresaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/pedidos_web")
public class PedidoWebController {

    private final PedidoWebService pedidoWebService;
    private final EmpresaService empresaService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public PedidoWebController(PedidoWebService pedidoWebService, EmpresaService empresaService) {
        this.pedidoWebService = pedidoWebService;
        this.empresaService = empresaService;
    }

    @GetMapping("")
    public String listarPedidosWeb() {
        return "pedidos-web";
    }


    @PostMapping("/api/upload")
    @ResponseBody
    public ResponseEntity<?> uploadVoucher(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        
        if (file.isEmpty()) {
            response.put("success", false);
            response.put("message", "No se seleccionó ningún archivo");
            return ResponseEntity.badRequest().body(response);
        }

        // Validar tipo de archivo
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && 
            !contentType.equals("image/png") && !contentType.equals("image/webp"))) {
            response.put("success", false);
            response.put("message", "El archivo debe ser JPG, PNG o WEBP");
            return ResponseEntity.badRequest().body(response);
        }

        // Validar tamaño (5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            response.put("success", false);
            response.put("message", "El archivo no debe superar 5MB");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // Crear directorio si no existe
            File uploadDirectory = new File(uploadDir + "vouchers/");
            if (!uploadDirectory.exists()) {
                uploadDirectory.mkdirs();
            }

            // Generar nombre único
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = "voucher_" + UUID.randomUUID().toString() + extension;
            
            Path path = Paths.get(uploadDir + "vouchers/" + filename);
            Files.write(path, file.getBytes());

            response.put("success", true);
            response.put("message", "Archivo subido correctamente");
            response.put("filePath", "/uploads/vouchers/" + filename);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Error al subir el archivo: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/api/crear")
    @ResponseBody
    public ResponseEntity<?> crearPedido(@RequestBody PedidoWeb pedido) {
        try {
            PedidoWeb pedidoGuardado = pedidoWebService.crearPedido(pedido);
            return ResponseEntity.ok(Map.of("success", true, "message", "Pedido web registrado con éxito", "pedidoId", pedidoGuardado.getNumeroPedido()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/api/usuario-actual")
    @ResponseBody
    public ResponseEntity<?> obtenerUsuarioActual(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario != null) {
            return ResponseEntity.ok(Map.of("id", usuario.getId(), "nombre", usuario.getNombre()));
        } else {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "No hay usuario autenticado"));
        }
    }

    @GetMapping("/mis-pedidos")
    public String verMisPedidos(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        Empresa empresa = empresaService.getEmpresaInfo();
        model.addAttribute("usuarioLogueado", usuario);
        model.addAttribute("empresaGlobal", empresa);
        return "mis-pedidos";
    }

    @GetMapping("/api/mis-pedidos")
    @ResponseBody
    public ResponseEntity<?> obtenerMisPedidos(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "No hay usuario autenticado"));
        }
        
        List<PedidoWeb> pedidos = pedidoWebService.listarPedidosPorCliente(usuario.getId());
        return ResponseEntity.ok(Map.of("success", true, "data", pedidos));
    }

    @GetMapping("/api/listar")
    @ResponseBody
    public ResponseEntity<?> listarPedidosWebApi() {
        System.out.println("=== ENDPOINT /api/listar LLAMADO ===");
        List<PedidoWeb> pedidos = pedidoWebService.listarTodosLosPedidos();
        System.out.println("=== RESPUESTA ENVIADA CON " + pedidos.size() + " PEDIDOS ===");
        return ResponseEntity.ok(Map.of("success", true, "data", pedidos));
    }

    @GetMapping("/api/cliente/{clienteId}")
    @ResponseBody
    public ResponseEntity<?> listarPedidosPorCliente(@PathVariable Long clienteId) {
        List<PedidoWeb> pedidos = pedidoWebService.listarPedidosPorCliente(clienteId);
        return ResponseEntity.ok(Map.of("success", true, "data", pedidos));
    }

    @GetMapping("/api/estado/{estado}")
    @ResponseBody
    public ResponseEntity<?> listarPedidosPorEstado(@PathVariable EstadoPedidoWeb estado) {
        List<PedidoWeb> pedidos = pedidoWebService.listarPedidosPorEstado(estado);
        return ResponseEntity.ok(Map.of("success", true, "data", pedidos));
    }

    @GetMapping("/api/detalle/{id}")
    @ResponseBody
    public ResponseEntity<?> obtenerDetallePedido(@PathVariable Long id) {
        return pedidoWebService.obtenerPedidoPorId(id)
                .map(pedido -> ResponseEntity.ok(Map.of("success", true, "data", pedido)))
                .orElse(ResponseEntity.badRequest().body(Map.of("success", false, "message", "Pedido no encontrado")));
    }

    @GetMapping("/api/numero/{numeroPedido}")
    @ResponseBody
    public ResponseEntity<?> obtenerPedidoPorNumero(@PathVariable String numeroPedido) {
        return pedidoWebService.obtenerPedidoPorNumero(numeroPedido)
                .map(pedido -> ResponseEntity.ok(Map.of("success", true, "data", pedido)))
                .orElse(ResponseEntity.badRequest().body(Map.of("success", false, "message", "Pedido no encontrado")));
    }

    @PostMapping("/api/aprobar/{id}")
    @ResponseBody
    public ResponseEntity<?> aprobarPedido(@PathVariable Long id, @RequestParam Long verificadoPorId) {
        try {
            PedidoWeb pedido = pedidoWebService.aprobarPedido(id, verificadoPorId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Pedido aprobado con éxito", "data", pedido));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/api/rechazar/{id}")
    @ResponseBody
    public ResponseEntity<?> rechazarPedido(@PathVariable Long id, @RequestParam Long verificadoPorId, @RequestParam(required = false) String motivoRechazo) {
        try {
            PedidoWeb pedido = pedidoWebService.rechazarPedido(id, verificadoPorId, motivoRechazo);
            return ResponseEntity.ok(Map.of("success", true, "message", "Pedido rechazado con éxito", "data", pedido));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/api/procesar/{id}")
    @ResponseBody
    public ResponseEntity<?> marcarComoProcesado(@PathVariable Long id) {
        try {
            PedidoWeb pedido = pedidoWebService.marcarComoProcesado(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Pedido marcado como procesado con éxito", "data", pedido));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/api/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarPedido(@PathVariable Long id) {
        try {
            pedidoWebService.eliminarPedido(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Pedido eliminado con éxito"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
