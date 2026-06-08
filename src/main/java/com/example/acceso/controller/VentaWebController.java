package com.example.acceso.controller;

import com.example.acceso.model.Categoria;
import com.example.acceso.model.PedidoWeb;
import com.example.acceso.model.Producto;
import com.example.acceso.model.Usuario;
import com.example.acceso.model.VentaWeb;
import com.example.acceso.service.CategoriaService;
import com.example.acceso.service.PedidoWebService;
import com.example.acceso.service.ProductoService;
import com.example.acceso.service.VentaWebService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page; // Import Page
import org.springframework.format.annotation.DateTimeFormat; // Import DateTimeFormat
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate; // Import LocalDate
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/ventas_web")
public class VentaWebController {

    private final VentaWebService ventaWebService;
    private final ProductoService productoService;
    private final CategoriaService categoriaService;
    private final PedidoWebService pedidoWebService;

    @Autowired
    public VentaWebController(VentaWebService ventaWebService, ProductoService productoService, CategoriaService categoriaService, PedidoWebService pedidoWebService) {
        this.ventaWebService = ventaWebService;
        this.productoService = productoService;
        this.categoriaService = categoriaService;
        this.pedidoWebService = pedidoWebService;
    }

    // Se elimina el método tienePermisoParaGestionar(HttpSession session)
    // ya que la lógica de permisos ahora se maneja en SessionInterceptor
    // y la verificación en el controlador era redundante y errónea.

    @GetMapping("")
    public String listarVentasWeb() {
        return "ventas-web";
    }

    @GetMapping("/modificar/{id}")
    public String showModificarVentaWeb(@PathVariable Long id, Model model) {
        List<Categoria> categoriasActivas = categoriaService.listarCategoriasActivas();
        List<Producto> productosActivos = productoService.listarProductosActivos();
        Map<Categoria, List<Producto>> productosPorCategoria = categoriasActivas.stream()
                .collect(Collectors.toMap(
                        categoria -> categoria,
                        categoria -> productosActivos.stream()
                                .filter(p -> p.getCategoria() != null && p.getCategoria().getId().equals(categoria.getId()))
                                .collect(Collectors.toList())
                ));
        model.addAttribute("productosPorCategoria", productosPorCategoria);
        model.addAttribute("ventaWebId", id);
        return "modificar-venta-web";
    }

    @PostMapping("/api/guardar")
    @ResponseBody
    public ResponseEntity<?> guardarVentaWeb(@RequestBody VentaWeb ventaWeb) {
        try {
            VentaWeb ventaGuardada = ventaWebService.guardarVentaWeb(ventaWeb);
            return ResponseEntity.ok(Map.of("success", true, "message", "Pedido web registrado con éxito", "ventaWebId", ventaGuardada.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/api/listar")
    @ResponseBody
    public ResponseEntity<?> listarVentasWebApi(
            @RequestParam(defaultValue = "0") int draw, // DataTables draw counter
            @RequestParam(defaultValue = "0") int start, // Start index for pagination
            @RequestParam(defaultValue = "10") int length, // Page size for pagination
            @RequestParam(name = "order[0][column]", defaultValue = "0") int orderColumn, // Column index for sorting
            @RequestParam(name = "order[0][dir]", defaultValue = "desc") String orderDir, // Sort direction
            @RequestParam(name = "columns[0][data]", defaultValue = "id") String orderColumnName, // Default column name for sorting
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) String busqueda) {
        try {
            System.out.println("=== ENDPOINT /ventas_web/api/listar LLAMADO ===");
            System.out.println("Filtros recibidos: estado=" + estado + ", fechaDesde=" + fechaDesde + ", fechaHasta=" + fechaHasta + ", busqueda=" + busqueda);

            // Convert DataTables parameters to Spring Data Pageable
            // DataTables sends start and length, Spring Data uses page and size
            int page = start / length;
            String sortBy = orderColumnName; // Assuming orderColumnName is the data property name

            Page<PedidoWeb> pedidos = pedidoWebService.listarPedidosConFiltros(page, length, sortBy, orderDir, estado, fechaDesde, fechaHasta, busqueda);

            // DataTables expects 'data', 'draw', 'recordsTotal', 'recordsFiltered'

            // DataTables expects 'data', 'draw', 'recordsTotal', 'recordsFiltered'
            return ResponseEntity.ok(Map.of(
                    "draw", draw,
                    "recordsTotal", pedidos.getTotalElements(),
                    "recordsFiltered", pedidos.getTotalElements(), // Assuming filtered equals total for now, or implement actual filtering logic in service
                    "data", pedidos.getContent()
            ));
        } catch (Exception e) {
            System.out.println("=== ERROR EN ENDPOINT /ventas_web/api/listar ===");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/api/detalle/{id}")
    @ResponseBody
    public ResponseEntity<?> obtenerDetalleVentaWeb(@PathVariable Long id) {
        return pedidoWebService.obtenerPedidoPorId(id)
                .map(pedido -> ResponseEntity.ok(Map.of("success", true, "data", pedido)))
                .orElse(ResponseEntity.badRequest().body(Map.of("success", false, "message", "Pedido no encontrado")));
    }

    @PutMapping("/api/aprobar/{id}")
    @ResponseBody
    public ResponseEntity<?> aprobarPedido(@PathVariable Long id, HttpSession session) {
        try {
            Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado"); // Corregido para usar "usuarioLogueado"
            Long verificadoPorId = usuario != null ? usuario.getId() : null;
            pedidoWebService.aprobarPedido(id, verificadoPorId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Pedido aprobado con éxito"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Nuevo endpoint para anular un pedido (cambiar estado a ANULADO)
    @PutMapping("/api/anular/{id}")
    @ResponseBody
    public ResponseEntity<?> anularPedido(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body, HttpSession session) {
        // Se elimina la verificación de permisos aquí, ya que SessionInterceptor
        // se encarga de validar el acceso para administradores a rutas /api/.
        try {
            String motivo = body != null ? body.get("motivo") : null;
            // Reusing rechazarPedido as "anulado" and "rechazado" are the same
            // Assuming rechazarPedido changes the status to ANULADO/RECHAZADO
            pedidoWebService.rechazarPedido(id, null, motivo); // Pass null for verificadoPorId if not applicable for anular
            return ResponseEntity.ok(Map.of("success", true, "message", "Pedido web anulado con éxito."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Error al anular el pedido web: " + e.getMessage()));
        }
    }

    @PutMapping("/api/rechazar/{id}")
    @ResponseBody
    public ResponseEntity<?> rechazarPedido(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body, HttpSession session) {
        // Se elimina la verificación de permisos aquí, ya que SessionInterceptor
        // se encarga de validar el acceso para administradores a rutas /api/.
        try {
            String motivo = body != null ? body.get("motivo") : null;
            pedidoWebService.rechazarPedido(id, null, motivo);
            return ResponseEntity.ok(Map.of("success", true, "message", "Pedido rechazado con éxito"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/api/procesar/{id}")
    @ResponseBody
    public ResponseEntity<?> procesarVentaWeb(@PathVariable Long id, HttpSession session) {
        // Se elimina la verificación de permisos aquí, ya que SessionInterceptor
        // se encarga de validar el acceso para administradores a rutas /api/.
        try {
            ventaWebService.procesarVentaWeb(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Venta procesada con éxito."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Error al procesar la venta: " + e.getMessage()));
        }
    }

    // Renombrado y mensajes actualizados para eliminación permanente
    @DeleteMapping("/api/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarPedidoWebPermanentemente(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body, HttpSession session) {
        // Se elimina la verificación de permisos aquí, ya que SessionInterceptor
        // se encarga de validar el acceso para administradores a rutas /api/.
        try {
            String motivo = body != null ? body.get("motivo") : null;
            pedidoWebService.eliminarPedido(id, motivo); // Assuming this performs a hard delete and Cloudinary cleanup
            return ResponseEntity.ok(Map.of("success", true, "message", "Pedido web eliminado permanentemente con éxito."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Error al eliminar el pedido web permanentemente: " + e.getMessage()));
        }
    }

    @PutMapping("/api/actualizar/{id}")
    @ResponseBody
    public ResponseEntity<?> actualizarVentaWeb(@PathVariable Long id, @RequestBody VentaWeb ventaWeb) {
        try {
            VentaWeb actualizada = ventaWebService.actualizarVentaWeb(id, ventaWeb);
            return ResponseEntity.ok(Map.of("success", true, "message", "Venta web actualizada con éxito.", "data", actualizada));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Error al actualizar la venta web: " + e.getMessage()));
        }
    }
}