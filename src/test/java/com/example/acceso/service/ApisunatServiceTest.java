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
