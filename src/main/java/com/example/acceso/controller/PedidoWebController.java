package com.example.acceso.controller;

import com.example.acceso.model.PedidoWeb;
import com.example.acceso.model.EstadoPedidoWeb;
import com.example.acceso.model.Usuario;
import com.example.acceso.model.Empresa;
import com.example.acceso.service.PedidoWebService;
import com.example.acceso.service.EmpresaService;
import com.example.acceso.service.CloudinaryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/pedidos_web")
public class PedidoWebController {

    private final PedidoWebService pedidoWebService;
    private final EmpresaService empresaService;
    private final CloudinaryService cloudinaryService;

    public PedidoWebController(PedidoWebService pedidoWebService, EmpresaService empresaService, CloudinaryService cloudinaryService) {
        this.pedidoWebService = pedidoWebService;
        this.empresaService = empresaService;
        this.cloudinaryService = cloudinaryService;
    }

    private boolean tienePermisoParaGestionar(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || usuario.getPerfil() == null) {
            return false;
        }
        return usuario.getPerfil().getOpciones().stream()
                .anyMatch(opcion -> "/pedidos_web/listar".equals(opcion.getRuta()) ||
                                     "/ventas_web/listar".equals(opcion.getRuta()));
    }

    @GetMapping("")
    public String listarPedidosWeb() {
        return "pedidos-web";
    }


    @PostMapping("/api/upload")
    @ResponseBody
    public ResponseEntity<?> uploadVoucher(@RequestParam("file") MultipartFile file, HttpSession session) {
        if (!tienePermisoParaGestionar(session)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "No tienes permiso para realizar esta acción."));
        }
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
            Map<String, String> uploadResult = cloudinaryService.uploadFile(file, "vouchers");
            String fileUrl = uploadResult.get("secure_url");

            response.put("success", true);
            response.put("message", "Archivo subido correctamente");
            response.put("filePath", fileUrl);
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
    public ResponseEntity<?> aprobarPedido(@PathVariable Long id, @RequestParam Long verificadoPorId, HttpSession session) {
        if (!tienePermisoParaGestionar(session)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "No tienes permiso para realizar esta acción."));
        }
        try {
            PedidoWeb pedido = pedidoWebService.aprobarPedido(id, verificadoPorId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Pedido aprobado con éxito", "data", pedido));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/api/rechazar/{id}")
    @ResponseBody
    public ResponseEntity<?> rechazarPedido(@PathVariable Long id, @RequestParam Long verificadoPorId, @RequestParam(required = false) String motivoRechazo, HttpSession session) {
        if (!tienePermisoParaGestionar(session)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "No tienes permiso para realizar esta acción."));
        }
        try {
            PedidoWeb pedido = pedidoWebService.rechazarPedido(id, verificadoPorId, motivoRechazo);
            return ResponseEntity.ok(Map.of("success", true, "message", "Pedido rechazado con éxito", "data", pedido));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/api/procesar/{id}")
    @ResponseBody
    public ResponseEntity<?> marcarComoProcesado(@PathVariable Long id, HttpSession session) {
        if (!tienePermisoParaGestionar(session)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "No tienes permiso para realizar esta acción."));
        }
        try {
            PedidoWeb pedido = pedidoWebService.marcarComoProcesado(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Pedido marcado como procesado con éxito", "data", pedido));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/api/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarPedido(@PathVariable Long id, HttpSession session) {
        if (!tienePermisoParaGestionar(session)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "No tienes permiso para realizar esta acción."));
        }
        try {
            pedidoWebService.eliminarPedido(id, null);
            return ResponseEntity.ok(Map.of("success", true, "message", "Pedido eliminado con éxito"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}