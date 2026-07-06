    package com.example.acceso.controller;

import com.example.acceso.model.*;
import com.example.acceso.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VentaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private CategoriaService categoriaService;

    @Autowired
    private CajaService cajaService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private VentaService ventaService;

    // ==================== MÉTODOS AUXILIARES ====================

    private void autenticarUsuario(Usuario usuario) {
        // Simular autenticación agregando el usuario a la sesión
        // Esto se hace en cada test antes de realizar las peticiones
    }

    // ==================== PRUEBAS DE CREACIÓN DE VENTA ====================

    @Test
    void testGuardarVentaConDatosValidos() throws Exception {
        // Preparar datos
        Producto p = crearProducto("Prod Controller Test", 100, 100);
        Cliente c = crearCliente("87654350");
        Usuario usuario = crearUsuario("Usuario Controller Test");

        // Abrir caja primero
        abrirCaja(usuario);

        // Autenticar usuario
        Usuario usuarioAutenticado = usuarioService.guardarUsuario(usuario);

        Venta venta = new Venta();
        venta.setCliente(c);
        venta.setMetodoPago("Efectivo");
        venta.setTipoComprobante("Boleta");
        venta.setOrigen("pos");

        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(p);
        detalle.setCantidad(2);
        detalle.setPrecioUnitario(new BigDecimal("100.00"));

        List<DetalleVenta> detalles = new ArrayList<>();
        detalles.add(detalle);
        venta.setDetalles(detalles);

        // Ejecutar request con sesión autenticada
        mockMvc.perform(post("/ventas/api/guardar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(venta))
                .sessionAttr("usuarioLogueado", usuarioAutenticado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.numeroVenta").exists())
                .andExpect(jsonPath("$.message").value("Venta registrada con éxito."));

        System.out.println("✓ Test POST /ventas/api/guardar - PASS");
    }

    @Test
    void testGuardarVentaSinCajaAbierta() throws Exception {
        // Preparar datos
        Producto p = crearProducto("Prod Sin Caja", 100, 100);
        Cliente c = crearCliente("87654351");
        Usuario usuario = crearUsuario("Usuario Sin Caja");

        // Cerrar caja si está abierta
        cerrarCajaSiExiste();

        // Autenticar usuario
        Usuario usuarioAutenticado = usuarioService.guardarUsuario(usuario);

        Venta venta = new Venta();
        venta.setCliente(c);
        venta.setMetodoPago("Efectivo");
        venta.setTipoComprobante("Boleta");
        venta.setOrigen("pos");

        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(p);
        detalle.setCantidad(1);
        detalle.setPrecioUnitario(new BigDecimal("100.00"));

        List<DetalleVenta> detalles = new ArrayList<>();
        detalles.add(detalle);
        venta.setDetalles(detalles);

        // Ejecutar request con sesión autenticada - debería fallar por caja cerrada
        mockMvc.perform(post("/ventas/api/guardar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(venta))
                .sessionAttr("usuarioLogueado", usuarioAutenticado))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("caja_cerrada"));

        System.out.println("✓ Test POST /ventas/api/guardar sin caja - PASS");
    }

    @Test
    void testGuardarVentaConStockInsuficiente() throws Exception {
        // Preparar datos
        Producto p = crearProducto("Prod Stock Insuficiente", 100, 5);
        Cliente c = crearCliente("87654352");
        Usuario usuario = crearUsuario("Usuario Stock Insuficiente");

        // Abrir caja
        abrirCaja(usuario);

        // Autenticar usuario
        Usuario usuarioAutenticado = usuarioService.guardarUsuario(usuario);

        Venta venta = new Venta();
        venta.setCliente(c);
        venta.setMetodoPago("Efectivo");
        venta.setTipoComprobante("Boleta");
        venta.setOrigen("pos");

        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(p);
        detalle.setCantidad(10); // Stock insuficiente
        detalle.setPrecioUnitario(new BigDecimal("100.00"));

        List<DetalleVenta> detalles = new ArrayList<>();
        detalles.add(detalle);
        venta.setDetalles(detalles);

        // Ejecutar request con sesión autenticada - debería fallar por stock insuficiente
        mockMvc.perform(post("/ventas/api/guardar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(venta))
                .sessionAttr("usuarioLogueado", usuarioAutenticado))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Stock insuficiente")));

        System.out.println("✓ Test POST /ventas/api/guardar stock insuficiente - PASS");
    }

    @Test
    void testGuardarVentaConProductoInexistente() throws Exception {
        // Preparar datos
        Cliente c = crearCliente("87654353");
        Usuario usuario = crearUsuario("Usuario Prod Inexistente");

        // Abrir caja
        abrirCaja(usuario);

        // Autenticar usuario
        Usuario usuarioAutenticado = usuarioService.guardarUsuario(usuario);

        Venta venta = new Venta();
        venta.setCliente(c);
        venta.setMetodoPago("Efectivo");
        venta.setTipoComprobante("Boleta");
        venta.setOrigen("pos");

        DetalleVenta detalle = new DetalleVenta();
        Producto productoInexistente = new Producto();
        productoInexistente.setId(99999L);
        detalle.setProducto(productoInexistente);
        detalle.setCantidad(1);

        List<DetalleVenta> detalles = new ArrayList<>();
        detalles.add(detalle);
        venta.setDetalles(detalles);

        // Ejecutar request con sesión autenticada - debería fallar por producto no encontrado
        mockMvc.perform(post("/ventas/api/guardar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(venta))
                .sessionAttr("usuarioLogueado", usuarioAutenticado))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Producto no encontrado")));

        System.out.println("✓ Test POST /ventas/api/guardar producto inexistente - PASS");
    }

    // ==================== PRUEBAS DE LISTADO DE VENTAS ====================

    @Test
    void testListarVentasApi() throws Exception {
        Usuario usuario = crearUsuario("Usuario Listar Ventas");
        Usuario usuarioAutenticado = usuarioService.guardarUsuario(usuario);

        mockMvc.perform(get("/ventas/api/listar")
                .sessionAttr("usuarioLogueado", usuarioAutenticado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        System.out.println("✓ Test GET /ventas/api/listar - PASS");
    }

    @Test
    void testListarVentasApiConFiltroFechas() throws Exception {
        Usuario usuario = crearUsuario("Usuario Listar Ventas Fechas");
        Usuario usuarioAutenticado = usuarioService.guardarUsuario(usuario);

        mockMvc.perform(get("/ventas/api/listar")
                .param("desde", "2024-01-01")
                .param("hasta", "2024-12-31")
                .sessionAttr("usuarioLogueado", usuarioAutenticado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        System.out.println("✓ Test GET /ventas/api/listar con fechas - PASS");
    }

    // ==================== PRUEBAS DE OBTENCIÓN DE DETALLE ====================

    @Test
    void testObtenerDetalleVentaExistente() throws Exception {
        // Crear una venta primero
        Producto p = crearProducto("Prod Detalle", 100, 100);
        Cliente c = crearCliente("87654354");
        Usuario usuario = crearUsuario("Usuario Detalle");

        abrirCaja(usuario);

        // Autenticar usuario
        Usuario usuarioAutenticado = usuarioService.guardarUsuario(usuario);

        Venta venta = new Venta();
        venta.setCliente(c);
        venta.setMetodoPago("Efectivo");
        venta.setTipoComprobante("Boleta");
        venta.setOrigen("pos");

        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(p);
        detalle.setCantidad(1);
        detalle.setPrecioUnitario(new BigDecimal("100.00"));

        List<DetalleVenta> detalles = new ArrayList<>();
        detalles.add(detalle);
        venta.setDetalles(detalles);

        Venta ventaCreada = crearVenta(venta);

        // Obtener detalle con sesión autenticada
        mockMvc.perform(get("/ventas/api/detalle/" + ventaCreada.getId())
                .sessionAttr("usuarioLogueado", usuarioAutenticado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());

        System.out.println("✓ Test GET /ventas/api/detalle/{id} - PASS");
    }

    @Test
    void testObtenerDetalleVentaInexistente() throws Exception {
        Usuario usuario = crearUsuario("Usuario Detalle Inexistente");
        Usuario usuarioAutenticado = usuarioService.guardarUsuario(usuario);

        mockMvc.perform(get("/ventas/api/detalle/99999")
                .sessionAttr("usuarioLogueado", usuarioAutenticado))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Venta no encontrada"));

        System.out.println("✓ Test GET /ventas/api/detalle/99999 - PASS");
    }

    // ==================== PRUEBAS DE ELIMINACIÓN ====================

    @Test
    void testEliminarVentaExistente() throws Exception {
        // Crear una venta primero
        Producto p = crearProducto("Prod Eliminar", 100, 100);
        Cliente c = crearCliente("87654355");
        Usuario usuario = crearUsuario("Usuario Eliminar");

        abrirCaja(usuario);

        // Autenticar usuario
        Usuario usuarioAutenticado = usuarioService.guardarUsuario(usuario);

        Venta venta = new Venta();
        venta.setCliente(c);
        venta.setMetodoPago("Efectivo");
        venta.setTipoComprobante("Nota de Venta"); // Nota de Venta sí se puede eliminar
        venta.setOrigen("pos");

        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(p);
        detalle.setCantidad(1);
        detalle.setPrecioUnitario(new BigDecimal("100.00"));

        List<DetalleVenta> detalles = new ArrayList<>();
        detalles.add(detalle);
        venta.setDetalles(detalles);

        Venta ventaCreada = crearVenta(venta);

        // Eliminar venta con sesión autenticada
        mockMvc.perform(delete("/ventas/api/eliminar/" + ventaCreada.getId())
                .sessionAttr("usuarioLogueado", usuarioAutenticado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Venta anulada con éxito."));

        System.out.println("✓ Test DELETE /ventas/api/eliminar/{id} - PASS");
    }

    @Test
    void testEliminarVentaInexistente() throws Exception {
        Usuario usuario = crearUsuario("Usuario Eliminar Inexistente");
        Usuario usuarioAutenticado = usuarioService.guardarUsuario(usuario);

        // El controlador retorna 200 con success=true incluso para ventas inexistentes
        mockMvc.perform(delete("/ventas/api/eliminar/99999")
                .sessionAttr("usuarioLogueado", usuarioAutenticado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").exists());

        System.out.println("✓ Test DELETE /ventas/api/eliminar/99999 - PASS");
    }

    // ==================== PRUEBAS DE ACTUALIZACIÓN ====================

    @Test
    void testActualizarVentaExistente() throws Exception {
        // Crear una venta primero
        Producto p = crearProducto("Prod Actualizar", 100, 100);
        Cliente c = crearCliente("87654356");
        Usuario usuario = crearUsuario("Usuario Actualizar");

        abrirCaja(usuario);

        // Autenticar usuario
        Usuario usuarioAutenticado = usuarioService.guardarUsuario(usuario);

        Venta venta = new Venta();
        venta.setCliente(c);
        venta.setMetodoPago("Efectivo");
        venta.setTipoComprobante("Boleta");
        venta.setOrigen("pos");

        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(p);
        detalle.setCantidad(1);
        detalle.setPrecioUnitario(new BigDecimal("100.00"));

        List<DetalleVenta> detalles = new ArrayList<>();
        detalles.add(detalle);
        venta.setDetalles(detalles);

        Venta ventaCreada = crearVenta(venta);

        // Actualizar venta
        Venta ventaActualizada = new Venta();
        ventaActualizada.setCliente(c);
        ventaActualizada.setMetodoPago("Tarjeta");
        ventaActualizada.setTipoComprobante("Factura");

        DetalleVenta detalle2 = new DetalleVenta();
        detalle2.setProducto(p);
        detalle2.setCantidad(2);
        detalle2.setPrecioUnitario(new BigDecimal("100.00"));

        List<DetalleVenta> detalles2 = new ArrayList<>();
        detalles2.add(detalle2);
        ventaActualizada.setDetalles(detalles2);

        mockMvc.perform(put("/ventas/api/actualizar/" + ventaCreada.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ventaActualizada))
                .sessionAttr("usuarioLogueado", usuarioAutenticado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Venta actualizada con éxito."));

        System.out.println("✓ Test PUT /ventas/api/actualizar/{id} - PASS");
    }

    // ==================== PRUEBAS DE VERIFICACIÓN DE CAJA ====================

    @Test
    void testVerificarCaja() throws Exception {
        Usuario usuario = crearUsuario("Usuario Verificar Caja");
        Usuario usuarioAutenticado = usuarioService.guardarUsuario(usuario);

        mockMvc.perform(get("/ventas/api/verificar-caja")
                .sessionAttr("usuarioLogueado", usuarioAutenticado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.haySesionActiva").isBoolean());

        System.out.println("✓ Test GET /ventas/api/verificar-caja - PASS");
    }

    // ==================== PRUEBAS DE TOP PRODUCTOS ====================

    @Test
    void testTop5ProductosSemana() throws Exception {
        Usuario usuario = crearUsuario("Usuario Top Productos");
        Usuario usuarioAutenticado = usuarioService.guardarUsuario(usuario);

        mockMvc.perform(get("/ventas/api/top5-productos-semana")
                .sessionAttr("usuarioLogueado", usuarioAutenticado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        System.out.println("✓ Test GET /ventas/api/top5-productos-semana - PASS");
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private Venta crearVenta(Venta venta) {
        return ventaService.crearVenta(venta);
    }

    private Producto crearProducto(String nombre, double precio, int stock) {
        com.example.acceso.model.Categoria cat = crearCategoria("Cat Controller " + nombre);
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setPrecio(new BigDecimal(precio));
        p.setStock(stock);
        p.setStockMinimo(5);
        p.setCategoria(cat);
        return productoService.guardarProducto(p);
    }

    private Cliente crearCliente(String dni) {
        Cliente c = new Cliente();
        c.setNombre("Cliente Controller " + dni);
        c.setTipoDocumento("DNI");
        c.setNumeroDocumento(dni);
        return clienteService.guardarCliente(c);
    }

    private com.example.acceso.model.Categoria crearCategoria(String nombre) {
        com.example.acceso.model.Categoria cat = new com.example.acceso.model.Categoria();
        cat.setNombre(nombre);
        cat.setDescripcion("Categoría de prueba controller");
        return categoriaService.guardarCategoria(cat);
    }

    private Usuario crearUsuario(String nombre) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setUsuario(nombre.toLowerCase().replace(" ", "_"));
        usuario.setCorreo(nombre.toLowerCase().replace(" ", "_") + "@test.com");
        usuario.setClave("password123");
        return usuarioService.guardarUsuario(usuario);
    }

    private void abrirCaja(Usuario usuario) {
        try {
            if (!cajaService.haySesionActiva()) {
                cajaService.abrirCaja(new BigDecimal("100.00"), usuario.getId());
            }
        } catch (Exception e) {
            // Si falla, continuar
        }
    }

    private void cerrarCajaSiExiste() {
        try {
            if (cajaService.haySesionActiva()) {
                SesionCaja sesion = cajaService.obtenerSesionActiva().orElse(null);
                if (sesion != null) {
                    cajaService.cerrarCaja(
                            sesion.getMontoInicial() != null ? sesion.getMontoInicial() : BigDecimal.ZERO,
                            "Cierre de prueba",
                            "Cierre automático para test",
                            sesion.getUsuarioApertura() != null ? sesion.getUsuarioApertura().getId() : 1L
                    );
                }
            }
        } catch (Exception e) {
            // Si falla, continuar
        }
    }
}