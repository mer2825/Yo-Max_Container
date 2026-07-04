package com.example.acceso.service;

import com.example.acceso.model.Cliente;
import com.example.acceso.model.DetalleVenta;
import com.example.acceso.model.Empresa;
import com.example.acceso.model.Producto;
import com.example.acceso.model.Venta;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApisunatServiceTest {

    @Test
    void convertirNumeroALetrasShouldHandleSmallAmounts() {
        ApisunatService service = new ApisunatService("https://back.apisunat.com", "token-123", "persona-123", "token-123", "20567890123", "/personas/v1/sendBill");
        
        // Test con monto pequeño (S/ 15.50)
        String resultado = service.convertirNumeroALetras(new java.math.BigDecimal("15.50"));
        assertTrue(resultado.contains("QUINCE"));
        assertTrue(resultado.contains("CON 50/100 SOLES"));
    }

    @Test
    void convertirNumeroALetrasShouldHandleLargeAmounts() {
        ApisunatService service = new ApisunatService("https://back.apisunat.com", "token-123", "persona-123", "token-123", "20567890123", "/personas/v1/sendBill");
        
        // Test con monto grande (S/ 150.00) - este causaba el error IndexOutOfBoundsException
        String resultado = service.convertirNumeroALetras(new java.math.BigDecimal("150.00"));
        // Para montos > 999 retorna formato numérico, pero 150 está en rango permitido
        assertTrue(resultado.contains("SOLES"));
        assertTrue(resultado.contains("CON 0/100 SOLES"));
    }

    @Test
    void convertirNumeroALetrasShouldHandleVeryLargeAmounts() {
        ApisunatService service = new ApisunatService("https://back.apisunat.com", "token-123", "persona-123", "token-123", "20567890123", "/personas/v1/sendBill");
        
        // Test con monto muy grande (S/ 1500.00) - mayor a 999
        String resultado = service.convertirNumeroALetras(new java.math.BigDecimal("1500.00"));
        assertTrue(resultado.contains("1500"));
        assertTrue(resultado.contains("SOLES"));
    }

    @Test
    void convertirNumeroALetrasShouldHandleEdgeCases() {
        ApisunatService service = new ApisunatService("https://back.apisunat.com", "token-123", "persona-123", "token-123", "20567890123", "/personas/v1/sendBill");
        
        // Test con S/ 100 exactos
        String resultado100 = service.convertirNumeroALetras(new java.math.BigDecimal("100.00"));
        assertTrue(resultado100.contains("CIEN"));
        
        // Test con S/ 105.75
        String resultado105 = service.convertirNumeroALetras(new java.math.BigDecimal("105.75"));
        assertTrue(resultado105.contains("CON 75/100 SOLES"));
        
        // Test con S/ 0.50 (cero soles)
        String resultadoCero = service.convertirNumeroALetras(new java.math.BigDecimal("0.50"));
        assertTrue(resultadoCero.contains("CON 50/100 SOLES"));
    }

    @Test
    void buildSendBillRequestBodyShouldUsePersonasEndpointShape() {
        ApisunatService service = new ApisunatService("https://back.apisunat.com", "token-123", "persona-123", "token-123", "20567890123", "/personas/v1/sendBill");

        Venta venta = new Venta();
        Cliente cliente = new Cliente();
        cliente.setNombre("Juan Perez");
        cliente.setTipoDocumento("DNI");
        cliente.setNumeroDocumento("12345678");
        cliente.setEmail("juan@test.com");
        venta.setCliente(cliente);

        Producto producto = new Producto();
        producto.setId(10L);
        producto.setNombre("Cafe");

        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(producto);
        detalle.setCantidad(2);
        detalle.setPrecioUnitario(BigDecimal.valueOf(10));
        venta.setDetalles(java.util.List.of(detalle));

        Empresa empresa = new Empresa();
        empresa.setRucEmpresa("20123456789");
        empresa.setEmail("empresa@test.com");

        Map<String, Object> body = service.buildSendBillRequestBody(venta, empresa, "03", "F001", 1, "1", "12345678", "Juan Perez");

        assertEquals("token-123", body.get("personaToken"));
        assertTrue(body.containsKey("fileName"));
        assertTrue(body.containsKey("documentBody"));
        assertTrue(body.get("documentBody") instanceof Map);
        Map<?, ?> documentBody = (Map<?, ?>) body.get("documentBody");
        assertTrue(documentBody.containsKey("cbc:InvoiceTypeCode"));
        assertEquals("03", ((Map<?, ?>) documentBody.get("cbc:InvoiceTypeCode")).get("_text"));
        assertEquals("juan@test.com", body.get("customerEmail"));
        assertTrue(body.get("fileName").toString().startsWith("20567890123-"));
    }
}
