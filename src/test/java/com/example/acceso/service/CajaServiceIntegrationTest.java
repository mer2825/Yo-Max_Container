package com.example.acceso.service;

import com.example.acceso.model.SesionCaja;
import com.example.acceso.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CajaServiceIntegrationTest {
    
    @Autowired
    private CajaService cajaService;
    
    @Autowired
    private UsuarioService usuarioService;
    
    // Método auxiliar para limpiar la sesión de caja activa
    private void limpiarSesionActiva() {
        if (cajaService.haySesionActiva()) {
            SesionCaja sesion = cajaService.obtenerSesionActiva().orElse(null);
            if (sesion != null) {
                try {
                    cajaService.cerrarCaja(
                        sesion.getMontoInicial() != null ? sesion.getMontoInicial() : BigDecimal.ZERO,
                        "Cierre de prueba",
                        "Cierre automático para limpiar test",
                        sesion.getUsuarioApertura() != null ? sesion.getUsuarioApertura().getId() : 1L
                    );
                } catch (Exception e) {
                    // Si falla el cierre, continuar de todas formas
                }
            }
        }
    }
    
    // ==================== PRUEBAS DE APERTURA DE CAJA ====================
    
    @Test
    void testAbrirCajaConDatosValidos() {
        // Crear usuario
        Usuario usuario = crearUsuario("Usuario Caja Test");
        
        // Limpiar sesión activa si existe
        limpiarSesionActiva();
        
        // Abrir caja
        SesionCaja sesion = cajaService.abrirCaja(new BigDecimal("100.00"), usuario.getId());
        
        assertNotNull(sesion);
        assertNotNull(sesion.getId());
        assertEquals("ABIERTA", sesion.getEstado());
        assertEquals(0, new BigDecimal("100.00").compareTo(sesion.getMontoInicial()));
        System.out.println("✓ Test abrir caja con datos válidos - PASS");
    }
    
    @Test
    void testAbrirCajaSinMontoInicial() {
        // Crear usuario
        Usuario usuario = crearUsuario("Usuario Caja Sin Monto");
        
        // Limpiar sesión activa si existe
        limpiarSesionActiva();
        
        // Abrir caja con monto cero (la base de datos no permite null)
        SesionCaja sesion = cajaService.abrirCaja(BigDecimal.ZERO, usuario.getId());
        
        assertNotNull(sesion);
        assertEquals("ABIERTA", sesion.getEstado());
        System.out.println("✓ Test abrir caja sin monto inicial - PASS");
    }
    
    @Test
    void testAbrirCajaConMontoCero() {
        // Crear usuario
        Usuario usuario = crearUsuario("Usuario Caja Monto Cero");
        
        // Limpiar sesión activa si existe
        limpiarSesionActiva();
        
        // Abrir caja con monto cero
        SesionCaja sesion = cajaService.abrirCaja(BigDecimal.ZERO, usuario.getId());
        
        assertNotNull(sesion);
        assertEquals("ABIERTA", sesion.getEstado());
        System.out.println("✓ Test abrir caja con monto cero - PASS");
    }
    
    @Test
    void testAbrirCajaConUsuarioInexistente() {
        // Limpiar sesión activa si existe
        limpiarSesionActiva();
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cajaService.abrirCaja(new BigDecimal("100.00"), 99999L);
        });
        
        assertTrue(exception.getMessage().contains("no encontrado"));
        System.out.println("✓ Test abrir caja con usuario inexistente - PASS");
    }
    
    @Test
    void testNoAbrirCajaSiYaExisteSesionAbierta() {
        // Crear usuario
        Usuario usuario = crearUsuario("Usuario Caja Sesion Abierta");
        
        // Limpiar sesión activa si existe
        limpiarSesionActiva();
        
        // Abrir primera sesión
        cajaService.abrirCaja(new BigDecimal("100.00"), usuario.getId());
        
        // Intentar abrir segunda sesión
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cajaService.abrirCaja(new BigDecimal("100.00"), usuario.getId());
        });
        
        assertTrue(exception.getMessage().contains("Ya existe una sesión"));
        System.out.println("✓ Test no abrir caja si ya existe sesión abierta - PASS");
    }
    
    // ==================== PRUEBAS DE CIERRE DE CAJA ====================
    
    @Test
    void testCerrarCajaConDatosValidos() {
        // Crear usuario
        Usuario usuario = crearUsuario("Usuario Caja Cierre");
        
        // Limpiar sesión activa si existe
        limpiarSesionActiva();
        
        // Abrir caja
        SesionCaja sesion = cajaService.abrirCaja(new BigDecimal("100.00"), usuario.getId());
        
        // Cerrar caja
        SesionCaja sesionCerrada = cajaService.cerrarCaja(
            new BigDecimal("100.00"), 
            "Todo correcto", 
            "Sin observaciones", 
            usuario.getId()
        );
        
        assertNotNull(sesionCerrada);
        assertEquals("CERRADA", sesionCerrada.getEstado());
        assertNotNull(sesionCerrada.getFechaCierre());
        System.out.println("✓ Test cerrar caja con datos válidos - PASS");
    }
    
    @Test
    void testCerrarCajaSinSesionAbierta() {
        // Limpiar sesión activa si existe
        limpiarSesionActiva();
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cajaService.cerrarCaja(new BigDecimal("100.00"), "Motivo", "Obs", 1L);
        });
        
        assertTrue(exception.getMessage().contains("No hay sesión abierta"));
        System.out.println("✓ Test cerrar caja sin sesión abierta - PASS");
    }
    
    @Test
    void testCerrarCajaConMontoNegativo() {
        // Crear usuario
        Usuario usuario = crearUsuario("Usuario Caja Monto Negativo");
        
        // Limpiar sesión activa si existe
        limpiarSesionActiva();
        
        // Abrir caja
        cajaService.abrirCaja(new BigDecimal("100.00"), usuario.getId());
        
        // Intentar cerrar con monto negativo
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cajaService.cerrarCaja(new BigDecimal("-50.00"), "Motivo", "Obs", usuario.getId());
        });
        
        assertTrue(exception.getMessage().contains("no puede ser negativo"));
        System.out.println("✓ Test cerrar caja con monto negativo - PASS");
    }
    
    @Test
    void testCerrarCajaConMontoNulo() {
        // Crear usuario
        Usuario usuario = crearUsuario("Usuario Caja Monto Nulo");
        
        // Limpiar sesión activa si existe
        limpiarSesionActiva();
        
        // Abrir caja
        cajaService.abrirCaja(new BigDecimal("100.00"), usuario.getId());
        
        // Intentar cerrar con monto nulo
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cajaService.cerrarCaja(null, "Motivo", "Obs", usuario.getId());
        });
        
        assertTrue(exception.getMessage().contains("obligatorio"));
        System.out.println("✓ Test cerrar caja con monto nulo - PASS");
    }
    
    // ==================== PRUEBAS DE REGISTRO DE MOVIMIENTOS ====================
    
    @Test
    void testRegistrarMovimientoIngresoValido() {
        // Crear usuario
        Usuario usuario = crearUsuario("Usuario Caja Movimiento Ingreso");
        
        // Limpiar sesión activa si existe
        limpiarSesionActiva();
        
        // Abrir caja
        SesionCaja sesion = cajaService.abrirCaja(new BigDecimal("100.00"), usuario.getId());
        
        // Registrar ingreso
        SesionCaja sesionActualizada = cajaService.registrarMovimiento(
            sesion.getId(),
            "INGRESO",
            new BigDecimal("50.00"),
            "Ingreso de prueba con descripción suficiente",
            "FONDO_ADICIONAL",
            usuario.getId()
        );
        
        assertNotNull(sesionActualizada);
        System.out.println("✓ Test registrar movimiento de ingreso válido - PASS");
    }
    
    @Test
    void testRegistrarMovimientoRetiroValido() {
        // Crear usuario
        Usuario usuario = crearUsuario("Usuario Caja Movimiento Retiro");
        
        // Limpiar sesión activa si existe
        limpiarSesionActiva();
        
        // Abrir caja con monto suficiente
        SesionCaja sesion = cajaService.abrirCaja(new BigDecimal("200.00"), usuario.getId());
        
        // Registrar retiro
        SesionCaja sesionActualizada = cajaService.registrarMovimiento(
            sesion.getId(),
            "RETIRO",
            new BigDecimal("50.00"),
            "Retiro de prueba con descripción suficiente",
            "RETIRO_DUENO",
            usuario.getId()
        );
        
        assertNotNull(sesionActualizada);
        System.out.println("✓ Test registrar movimiento de retiro válido - PASS");
    }
    
    @Test
    void testRegistrarMovimientoConMontoCero() {
        // Crear usuario
        Usuario usuario = crearUsuario("Usuario Caja Movimiento Cero");
        
        // Limpiar sesión activa si existe
        limpiarSesionActiva();
        
        // Abrir caja
        SesionCaja sesion = cajaService.abrirCaja(new BigDecimal("100.00"), usuario.getId());
        
        // Intentar registrar movimiento con monto cero
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cajaService.registrarMovimiento(
                sesion.getId(),
                "INGRESO",
                BigDecimal.ZERO,
                "Descripción de prueba",
                "FONDO_ADICIONAL",
                usuario.getId()
            );
        });
        
        assertTrue(exception.getMessage().contains("mayor a 0"));
        System.out.println("✓ Test registrar movimiento con monto cero - PASS");
    }
    
    @Test
    void testRegistrarMovimientoConMontoNegativo() {
        // Crear usuario
        Usuario usuario = crearUsuario("Usuario Caja Movimiento Negativo");
        
        // Limpiar sesión activa si existe
        limpiarSesionActiva();
        
        // Abrir caja
        SesionCaja sesion = cajaService.abrirCaja(new BigDecimal("100.00"), usuario.getId());
        
        // Intentar registrar movimiento con monto negativo
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cajaService.registrarMovimiento(
                sesion.getId(),
                "INGRESO",
                new BigDecimal("-50.00"),
                "Descripción de prueba",
                "FONDO_ADICIONAL",
                usuario.getId()
            );
        });
        
        assertTrue(exception.getMessage().contains("mayor a 0"));
        System.out.println("✓ Test registrar movimiento con monto negativo - PASS");
    }
    
    @Test
    void testRegistrarMovimientoSinMotivo() {
        // Crear usuario
        Usuario usuario = crearUsuario("Usuario Caja Movimiento Sin Motivo");
        
        // Limpiar sesión activa si existe
        limpiarSesionActiva();
        
        // Abrir caja
        SesionCaja sesion = cajaService.abrirCaja(new BigDecimal("100.00"), usuario.getId());
        
        // Intentar registrar movimiento sin motivo
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cajaService.registrarMovimiento(
                sesion.getId(),
                "INGRESO",
                new BigDecimal("50.00"),
                "",
                "FONDO_ADICIONAL",
                usuario.getId()
            );
        });
        
        assertTrue(exception.getMessage().contains("descripción es obligatoria"));
        System.out.println("✓ Test registrar movimiento sin motivo - PASS");
    }
    
    @Test
    void testRegistrarMovimientoConMotivoCorto() {
        // Crear usuario
        Usuario usuario = crearUsuario("Usuario Caja Movimiento Motivo Corto");
        
        // Limpiar sesión activa si existe
        limpiarSesionActiva();
        
        // Abrir caja
        SesionCaja sesion = cajaService.abrirCaja(new BigDecimal("100.00"), usuario.getId());
        
        // Intentar registrar movimiento con motivo muy corto
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cajaService.registrarMovimiento(
                sesion.getId(),
                "INGRESO",
                new BigDecimal("50.00"),
                "Corto",
                "FONDO_ADICIONAL",
                usuario.getId()
            );
        });
        
        assertTrue(exception.getMessage().contains("al menos 10 caracteres"));
        System.out.println("✓ Test registrar movimiento con motivo corto - PASS");
    }
    
    @Test
    void testRegistrarMovimientoSinCategoria() {
        // Crear usuario
        Usuario usuario = crearUsuario("Usuario Caja Movimiento Sin Categoria");
        
        // Limpiar sesión activa si existe
        limpiarSesionActiva();
        
        // Abrir caja
        SesionCaja sesion = cajaService.abrirCaja(new BigDecimal("100.00"), usuario.getId());
        
        // Intentar registrar movimiento sin categoría
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cajaService.registrarMovimiento(
                sesion.getId(),
                "INGRESO",
                new BigDecimal("50.00"),
                "Descripción de prueba",
                "",
                usuario.getId()
            );
        });
        
        assertTrue(exception.getMessage().contains("categoría es obligatoria"));
        System.out.println("✓ Test registrar movimiento sin categoría - PASS");
    }
    
    @Test
    void testRegistrarRetiroConCategoriaInvalida() {
        // Crear usuario
        Usuario usuario = crearUsuario("Usuario Caja Retiro Categoria Invalida");
        
        // Limpiar sesión activa si existe
        limpiarSesionActiva();
        
        // Abrir caja
        SesionCaja sesion = cajaService.abrirCaja(new BigDecimal("100.00"), usuario.getId());
        
        // Intentar registrar retiro con categoría inválida
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cajaService.registrarMovimiento(
                sesion.getId(),
                "RETIRO",
                new BigDecimal("50.00"),
                "Descripción de prueba",
                "CATEGORIA_INVALIDA",
                usuario.getId()
            );
        });
        
        assertTrue(exception.getMessage().contains("Categoría de retiro no válida"));
        System.out.println("✓ Test registrar retiro con categoría inválida - PASS");
    }
    
    @Test
    void testRegistrarIngresoConCategoriaInvalida() {
        // Crear usuario
        Usuario usuario = crearUsuario("Usuario Caja Ingreso Categoria Invalida");
        
        // Limpiar sesión activa si existe
        limpiarSesionActiva();
        
        // Abrir caja
        SesionCaja sesion = cajaService.abrirCaja(new BigDecimal("100.00"), usuario.getId());
        
        // Intentar registrar ingreso con categoría inválida
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cajaService.registrarMovimiento(
                sesion.getId(),
                "INGRESO",
                new BigDecimal("50.00"),
                "Descripción de prueba",
                "CATEGORIA_INVALIDA",
                usuario.getId()
            );
        });
        
        assertTrue(exception.getMessage().contains("Categoría de ingreso no válida"));
        System.out.println("✓ Test registrar ingreso con categoría inválida - PASS");
    }
    
    @Test
    void testRegistrarRetiroSuperaEfectivoDisponible() {
        // Crear usuario
        Usuario usuario = crearUsuario("Usuario Caja Retiro Supera");
        
        // Limpiar sesión activa si existe
        limpiarSesionActiva();
        
        // Abrir caja con monto limitado
        SesionCaja sesion = cajaService.abrirCaja(new BigDecimal("50.00"), usuario.getId());
        
        // Intentar retirar más de lo disponible
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cajaService.registrarMovimiento(
                sesion.getId(),
                "RETIRO",
                new BigDecimal("100.00"),
                "Retiro que supera el efectivo disponible",
                "RETIRO_DUENO",
                usuario.getId()
            );
        });
        
        assertTrue(exception.getMessage().contains("no puede superar el efectivo disponible"));
        System.out.println("✓ Test registrar retiro que supera efectivo disponible - PASS");
    }
    
    @Test
    void testRegistrarMovimientoEnSesionCerrada() {
        // Crear usuario
        Usuario usuario = crearUsuario("Usuario Caja Movimiento Sesion Cerrada");
        
        // Limpiar sesión activa si existe
        limpiarSesionActiva();
        
        // Abrir y cerrar caja
        SesionCaja sesion = cajaService.abrirCaja(new BigDecimal("100.00"), usuario.getId());
        cajaService.cerrarCaja(new BigDecimal("100.00"), "Motivo", "Obs", usuario.getId());
        
        // Intentar registrar movimiento en sesión cerrada
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cajaService.registrarMovimiento(
                sesion.getId(),
                "INGRESO",
                new BigDecimal("50.00"),
                "Descripción de prueba",
                "FONDO_ADICIONAL",
                usuario.getId()
            );
        });
        
        assertTrue(exception.getMessage().contains("no está abierta"));
        System.out.println("✓ Test registrar movimiento en sesión cerrada - PASS");
    }
    
    // ==================== PRUEBAS DE CONSULTAS ====================
    
    @Test
    void testHaySesionActiva() {
        // Crear usuario
        Usuario usuario = crearUsuario("Usuario Caja Sesion Activa");
        
        // Limpiar sesión activa si existe
        limpiarSesionActiva();
        
        // Inicialmente no debe haber sesión activa
        assertFalse(cajaService.haySesionActiva());
        
        // Abrir caja
        cajaService.abrirCaja(new BigDecimal("100.00"), usuario.getId());
        
        // Ahora debe haber sesión activa
        assertTrue(cajaService.haySesionActiva());
        System.out.println("✓ Test hay sesión activa - PASS");
    }
    
    @Test
    void testObtenerSesionActiva() {
        // Crear usuario
        Usuario usuario = crearUsuario("Usuario Caja Obtener Sesion");
        
        // Limpiar sesión activa si existe
        limpiarSesionActiva();
        
        // Inicialmente no debe haber sesión activa
        assertFalse(cajaService.obtenerSesionActiva().isPresent());
        
        // Abrir caja
        SesionCaja sesion = cajaService.abrirCaja(new BigDecimal("100.00"), usuario.getId());
        
        // Ahora debe haber sesión activa
        Optional<SesionCaja> sesionActiva = cajaService.obtenerSesionActiva();
        assertTrue(sesionActiva.isPresent());
        assertEquals(sesion.getId(), sesionActiva.get().getId());
        System.out.println("✓ Test obtener sesión activa - PASS");
    }
    
    @Test
    void testObtenerResumenSesionActiva() {
        // Crear usuario
        Usuario usuario = crearUsuario("Usuario Caja Resumen");
        
        // Limpiar sesión activa si existe
        limpiarSesionActiva();
        
        // Abrir caja
        cajaService.abrirCaja(new BigDecimal("100.00"), usuario.getId());
        
        // Obtener resumen
        var resumen = cajaService.obtenerResumenSesionActiva();
        
        assertNotNull(resumen);
        assertEquals(0, new BigDecimal("100.00").compareTo(resumen.getMontoInicial()));
        System.out.println("✓ Test obtener resumen sesión activa - PASS");
    }
    
    // ==================== MÉTODOS AUXILIARES ====================
    
    private Usuario crearUsuario(String nombre) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setUsuario(nombre.toLowerCase().replace(" ", "_"));
        usuario.setCorreo(nombre.toLowerCase().replace(" ", "_") + "@test.com");
        usuario.setClave("password123");
        return usuarioService.guardarUsuario(usuario);
    }
}