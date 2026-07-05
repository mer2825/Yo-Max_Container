package com.example.acceso.service;

import com.example.acceso.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class VentaServiceIntegrationTest {
    
    @Autowired
    private VentaService ventaService;
    
    @Autowired
    private ProductoService productoService;
    
    @Autowired
    private ClienteService clienteService;

    @Autowired
    private CategoriaService categoriaService;

    @Test
    void testCrearVentaConMontoAlto() {
        Producto p = crearProducto("Test Prod Alto", 150, 100);
        Cliente c = crearCliente("87654330");
        
        Venta v = new Venta();
        v.setCliente(c);
        v.setMetodoPago("Efectivo");
        v.setTipoComprobante("Boleta");
        v.setOrigen("pos");
        
        DetalleVenta d = new DetalleVenta();
        d.setProducto(p);
        d.setCantidad(1);
        d.setPrecioUnitario(new BigDecimal("150.00"));
        
        List<DetalleVenta> detalles = new ArrayList<>();
        detalles.add(d);
        v.setDetalles(detalles);
        
        Venta resultado = ventaService.crearVenta(v);
        
        assertNotNull(resultado);
        assertEquals(0, new BigDecimal("150.00").compareTo(resultado.getTotal()));
        System.out.println("✓ Test venta con monto alto (S/150) - PASS");

        
    }
    
    @Test
    void testCrearVentaConMontoMuyGrande() {
        Producto p = crearProducto("Test Prod Muy Alto", 100, 20);
        Cliente c = crearCliente("87654331");
        
        Venta v = new Venta();
        v.setCliente(c);
        v.setMetodoPago("Efectivo");
        v.setTipoComprobante("Boleta");
        v.setOrigen("pos");
        
        DetalleVenta d = new DetalleVenta();
        d.setProducto(p);
        d.setCantidad(15);
        d.setPrecioUnitario(new BigDecimal("100.00"));
        
        List<DetalleVenta> detalles = new ArrayList<>();
        detalles.add(d);
        v.setDetalles(detalles);
        
        Venta resultado = ventaService.crearVenta(v);
        
        assertNotNull(resultado);
        assertEquals(0, new BigDecimal("1500").compareTo(resultado.getTotal()));
        System.out.println("✓ Test venta con monto muy grande (S/1500) - PASS");
    }
    
    @Test
    void testCrearVentaConMultiplesProductos() {
        Producto p1 = crearProducto("Test Prod 1", 100, 100);
        Producto p2 = crearProducto("Test Prod 2", 50, 100);
        Cliente c = crearCliente("87654332");
        
        Venta v = new Venta();
        v.setCliente(c);
        v.setMetodoPago("Efectivo");
        v.setTipoComprobante("Boleta");
        v.setOrigen("pos");
        
        DetalleVenta d1 = new DetalleVenta();
        d1.setProducto(p1);
        d1.setCantidad(2);
        d1.setPrecioUnitario(new BigDecimal("100.00"));
        
        DetalleVenta d2 = new DetalleVenta();
        d2.setProducto(p2);
        d2.setCantidad(3);
        d2.setPrecioUnitario(new BigDecimal("50.00"));
        
        List<DetalleVenta> detalles = new ArrayList<>();
        detalles.add(d1);
        detalles.add(d2);
        v.setDetalles(detalles);
        
        Venta resultado = ventaService.crearVenta(v);
        
        assertNotNull(resultado);
        assertEquals(0, new BigDecimal("350.00").compareTo(resultado.getTotal()));
        System.out.println("✓ Test venta con múltiples productos - PASS");
    }
    
    @Test
    void testCrearVentaConDescuento() {
        Producto p = crearProducto("Test Prod Desc", 100, 100);
        Cliente c = crearCliente("87654333");
        
        Venta v = new Venta();
        v.setCliente(c);
        v.setMetodoPago("Efectivo");
        v.setTipoComprobante("Boleta");
        v.setOrigen("pos");
        v.setDescuento(new BigDecimal("20.00"));
        
        DetalleVenta d = new DetalleVenta();
        d.setProducto(p);
        d.setCantidad(1);
        d.setPrecioUnitario(new BigDecimal("100.00"));
        
        List<DetalleVenta> detalles = new ArrayList<>();
        detalles.add(d);
        v.setDetalles(detalles);
        
        Venta resultado = ventaService.crearVenta(v);
        
        assertNotNull(resultado);
        assertEquals(0, new BigDecimal("80.00").compareTo(resultado.getTotal()));
        System.out.println("✓ Test venta con descuento - PASS");
    }
    
    @Test
    void testCrearVentaDeberiaRestarStock() {
        Producto p = crearProducto("Test Prod Stock", 100, 100);
        Cliente c = crearCliente("87654334");
        int stockInicial = p.getStock();
        
        Venta v = new Venta();
        v.setCliente(c);
        v.setMetodoPago("Efectivo");
        v.setTipoComprobante("Boleta");
        v.setOrigen("pos");
        
        DetalleVenta d = new DetalleVenta();
        d.setProducto(p);
        d.setCantidad(10);
        d.setPrecioUnitario(new BigDecimal("100.00"));
        
        List<DetalleVenta> detalles = new ArrayList<>();
        detalles.add(d);
        v.setDetalles(detalles);
        
        ventaService.crearVenta(v);
        
        Producto productoActualizado = productoService.obtenerProductoPorId(p.getId()).orElseThrow();
        assertEquals(0, new BigDecimal(String.valueOf(stockInicial - 10)).compareTo(new BigDecimal(String.valueOf(productoActualizado.getStock()))));
        System.out.println("✓ Test descuento de stock - PASS");
    }
    
    @Test
    void testCrearVentaConStockInsuficiente() {
        Producto p = crearProducto("Test Prod Stock Bajo", 100, 5);
        Cliente c = crearCliente("87654335");
        
        Venta v = new Venta();
        v.setCliente(c);
        v.setMetodoPago("Efectivo");
        v.setTipoComprobante("Boleta");
        v.setOrigen("pos");
        
        DetalleVenta d = new DetalleVenta();
        d.setProducto(p);
        d.setCantidad(10);
        d.setPrecioUnitario(new BigDecimal("100.00"));
        
        List<DetalleVenta> detalles = new ArrayList<>();
        detalles.add(d);
        v.setDetalles(detalles);
        
        assertThrows(RuntimeException.class, () -> {
            ventaService.crearVenta(v);
        });
        System.out.println("✓ Test validación de stock insuficiente - PASS");
    }
    
    @Test
    void testTiposComprobantes() {
        Producto p = crearProducto("Test Prod Comp", 100, 100);
        Cliente c = crearCliente("87654336");
        
        Venta vBoleta = new Venta();
        vBoleta.setCliente(c);
        vBoleta.setMetodoPago("Efectivo");
        vBoleta.setTipoComprobante("Boleta");
        vBoleta.setOrigen("pos");
        
        DetalleVenta d1 = new DetalleVenta();
        d1.setProducto(p);
        d1.setCantidad(1);
        d1.setPrecioUnitario(new BigDecimal("50.00"));
        
        List<DetalleVenta> detalles1 = new ArrayList<>();
        detalles1.add(d1);
        vBoleta.setDetalles(detalles1);
        
        Venta resultadoBoleta = ventaService.crearVenta(vBoleta);
        assertTrue(resultadoBoleta.getNumeroVenta().startsWith("B"));
        
        Venta vFactura = new Venta();
        vFactura.setCliente(c);
        vFactura.setMetodoPago("Transferencia");
        vFactura.setTipoComprobante("Factura");
        vFactura.setOrigen("pos");
        
        DetalleVenta d2 = new DetalleVenta();
        d2.setProducto(p);
        d2.setCantidad(1);
        d2.setPrecioUnitario(new BigDecimal("50.00"));
        
        List<DetalleVenta> detalles2 = new ArrayList<>();
        detalles2.add(d2);
        vFactura.setDetalles(detalles2);
        
        Venta resultadoFactura = ventaService.crearVenta(vFactura);
        assertTrue(resultadoFactura.getNumeroVenta().startsWith("F"));
        
        System.out.println("✓ Test tipos de comprobantes (Boleta/Factura) - PASS");
    }
    
    @Test
    void testObtenerVentaPorId() {
        Producto p = crearProducto("Test Prod Buscar", 100, 100);
        Cliente c = crearCliente("87654337");
        
        Venta v = new Venta();
        v.setCliente(c);
        v.setMetodoPago("Efectivo");
        v.setTipoComprobante("Boleta");
        v.setOrigen("pos");
        
        DetalleVenta d = new DetalleVenta();
        d.setProducto(p);
        d.setCantidad(1);
        d.setPrecioUnitario(new BigDecimal("50.00"));
        
        List<DetalleVenta> detalles = new ArrayList<>();
        detalles.add(d);
        v.setDetalles(detalles);
        
        Venta ventaCreada = ventaService.crearVenta(v);
        Long ventaId = ventaCreada.getId();
        
        Optional<Venta> ventaObtenida = ventaService.obtenerVentaPorId(ventaId);
        
        assertTrue(ventaObtenida.isPresent());
        assertEquals(0, ventaId.compareTo(ventaObtenida.get().getId()));
        System.out.println("✓ Test obtener venta por ID - PASS");
    }

    // ==================== PRUEBAS DE VALIDACIÓN ADICIONALES ====================

    @Test
    void testCrearVentaSinDetalles() {
        Cliente c = crearCliente("87654338");
        
        Venta v = new Venta();
        v.setCliente(c);
        v.setMetodoPago("Efectivo");
        v.setTipoComprobante("Boleta");
        v.setOrigen("pos");
        v.setDetalles(new ArrayList<>()); // Lista vacía
        
        // El servicio debería manejar lista vacía (no lanza excepción pero genera total 0)
        Venta resultado = ventaService.crearVenta(v);
        assertNotNull(resultado);
        assertEquals(0, new BigDecimal("0.00").compareTo(resultado.getTotal()));
        System.out.println("✓ Test venta sin detalles - PASS");
    }

    @Test
    void testCrearVentaConDescuentoMayorAlSubtotal() {
        Producto p = crearProducto("Test Prod Desc Mayor", 100, 100);
        Cliente c = crearCliente("87654339");
        
        Venta v = new Venta();
        v.setCliente(c);
        v.setMetodoPago("Efectivo");
        v.setTipoComprobante("Boleta");
        v.setOrigen("pos");
        v.setDescuento(new BigDecimal("150.00")); // Descuento mayor al subtotal
        
        DetalleVenta d = new DetalleVenta();
        d.setProducto(p);
        d.setCantidad(1);
        d.setPrecioUnitario(new BigDecimal("100.00"));
        
        List<DetalleVenta> detalles = new ArrayList<>();
        detalles.add(d);
        v.setDetalles(detalles);
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ventaService.crearVenta(v);
        });
        
        assertTrue(exception.getMessage().contains("descuento no puede ser mayor"));
        System.out.println("✓ Test validación descuento mayor al subtotal - PASS");
    }

    @Test
    void testCrearVentaConProductoInexistente() {
        Cliente c = crearCliente("87654340");
        
        Venta v = new Venta();
        v.setCliente(c);
        v.setMetodoPago("Efectivo");
        v.setTipoComprobante("Boleta");
        v.setOrigen("pos");
        
        DetalleVenta d = new DetalleVenta();
        Producto productoInexistente = new Producto();
        productoInexistente.setId(99999L);
        d.setProducto(productoInexistente);
        d.setCantidad(1);
        
        List<DetalleVenta> detalles = new ArrayList<>();
        detalles.add(d);
        v.setDetalles(detalles);
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ventaService.crearVenta(v);
        });
        
        assertTrue(exception.getMessage().contains("Producto no encontrado"));
        System.out.println("✓ Test validación producto inexistente - PASS");
    }

    @Test
    void testCrearVentaConCantidadCero() {
        Producto p = crearProducto("Test Prod Cant Cero", 100, 100);
        Cliente c = crearCliente("87654341");
        
        Venta v = new Venta();
        v.setCliente(c);
        v.setMetodoPago("Efectivo");
        v.setTipoComprobante("Boleta");
        v.setOrigen("pos");
        
        DetalleVenta d = new DetalleVenta();
        d.setProducto(p);
        d.setCantidad(0); // Cantidad cero
        d.setPrecioUnitario(new BigDecimal("100.00"));
        
        List<DetalleVenta> detalles = new ArrayList<>();
        detalles.add(d);
        v.setDetalles(detalles);
        
        // Con cantidad 0, el stock no se modifica pero la venta se crea
        Venta resultado = ventaService.crearVenta(v);
        assertNotNull(resultado);
        System.out.println("✓ Test venta con cantidad cero - PASS");
    }

    @Test
    void testCrearVentaConMetodoPagoValido() {
        Producto p = crearProducto("Test Prod Metodo Pago", 100, 100);
        Cliente c = crearCliente("87654342");
        
        Venta v = new Venta();
        v.setCliente(c);
        v.setMetodoPago("Tarjeta");
        v.setTipoComprobante("Boleta");
        v.setOrigen("pos");
        
        DetalleVenta d = new DetalleVenta();
        d.setProducto(p);
        d.setCantidad(1);
        d.setPrecioUnitario(new BigDecimal("100.00"));
        
        List<DetalleVenta> detalles = new ArrayList<>();
        detalles.add(d);
        v.setDetalles(detalles);
        
        Venta resultado = ventaService.crearVenta(v);
        assertNotNull(resultado);
        assertEquals("Tarjeta", resultado.getMetodoPago());
        System.out.println("✓ Test venta con método de pago válido - PASS");
    }

    @Test
    void testActualizarVentaConStockInsuficiente() {
        Producto p = crearProducto("Test Prod Actualizar Stock", 100, 10);
        Cliente c = crearCliente("87654343");
        
        // Crear venta inicial
        Venta v = new Venta();
        v.setCliente(c);
        v.setMetodoPago("Efectivo");
        v.setTipoComprobante("Boleta");
        v.setOrigen("pos");
        
        DetalleVenta d = new DetalleVenta();
        d.setProducto(p);
        d.setCantidad(5);
        d.setPrecioUnitario(new BigDecimal("100.00"));
        
        List<DetalleVenta> detalles = new ArrayList<>();
        detalles.add(d);
        v.setDetalles(detalles);
        
        Venta ventaCreada = ventaService.crearVenta(v);
        
        // Intentar actualizar con cantidad mayor al stock disponible
        Venta ventaActualizada = new Venta();
        ventaActualizada.setCliente(c);
        ventaActualizada.setMetodoPago("Efectivo");
        ventaActualizada.setTipoComprobante("Boleta");
        
        DetalleVenta d2 = new DetalleVenta();
        d2.setProducto(p);
        d2.setCantidad(20); // Stock disponible es 5 (10 - 5 de la primera venta)
        d2.setPrecioUnitario(new BigDecimal("100.00"));
        
        List<DetalleVenta> detalles2 = new ArrayList<>();
        detalles2.add(d2);
        ventaActualizada.setDetalles(detalles2);
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ventaService.actualizarVenta(ventaCreada.getId(), ventaActualizada);
        });
        
        assertTrue(exception.getMessage().contains("Stock insuficiente"));
        System.out.println("✓ Test actualizar venta con stock insuficiente - PASS");
    }

    @Test
    void testEliminarVentaDevuelveStock() {
        Producto p = crearProducto("Test Prod Eliminar Stock", 100, 100);
        Cliente c = crearCliente("87654344");
        
        Venta v = new Venta();
        v.setCliente(c);
        v.setMetodoPago("Efectivo");
        v.setTipoComprobante("Nota de Venta"); // Nota de Venta sí se puede eliminar directamente
        v.setOrigen("pos");
        
        DetalleVenta d = new DetalleVenta();
        d.setProducto(p);
        d.setCantidad(10);
        d.setPrecioUnitario(new BigDecimal("100.00"));
        
        List<DetalleVenta> detalles = new ArrayList<>();
        detalles.add(d);
        v.setDetalles(detalles);
        
        Venta ventaCreada = ventaService.crearVenta(v);
        int stockDespuesVenta = productoService.obtenerProductoPorId(p.getId()).orElseThrow().getStock();
        
        // Eliminar la venta
        ventaService.eliminarVenta(ventaCreada.getId());
        
        // Verificar que el stock se devolvió
        Producto productoActualizado = productoService.obtenerProductoPorId(p.getId()).orElseThrow();
        assertEquals(100, productoActualizado.getStock());
        System.out.println("✓ Test eliminar venta devuelve stock - PASS");
    }

    // ==================== MÉTODOS AUXILIARES ====================
    
    private Producto crearProducto(String nombre, double precio, int stock) {
        com.example.acceso.model.Categoria cat = crearCategoria("Cat " + nombre);
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
        c.setNombre("Cliente Test " + dni);
        c.setTipoDocumento("DNI");
        c.setNumeroDocumento(dni);
        return clienteService.guardarCliente(c);
    }
    
    private com.example.acceso.model.Categoria crearCategoria(String nombre) {
        com.example.acceso.model.Categoria cat = new com.example.acceso.model.Categoria();
        cat.setNombre(nombre);
        cat.setDescripcion("Categoría de prueba");
        return categoriaService.guardarCategoria(cat);
    }

}