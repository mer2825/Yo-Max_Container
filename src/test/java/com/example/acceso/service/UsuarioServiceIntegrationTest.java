package com.example.acceso.service;

import com.example.acceso.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UsuarioServiceIntegrationTest {
    
    @Autowired
    private UsuarioService usuarioService;
    
    // ==================== PRUEBAS DE CREACIÓN ====================
    
    @Test
    void testCrearUsuarioConDatosValidos() {
        Usuario usuario = new Usuario();
        usuario.setNombre("Usuario Test Válido");
        usuario.setUsuario("usuariotest");
        usuario.setCorreo("test@example.com");
        usuario.setClave("password123");
        
        Usuario resultado = usuarioService.guardarUsuario(usuario);
        
        assertNotNull(resultado);
        assertNotNull(resultado.getId());
        assertEquals("Usuario Test Válido", resultado.getNombre());
        assertEquals("usuariotest", resultado.getUsuario());
        assertEquals("test@example.com", resultado.getCorreo());
        assertEquals(1, resultado.getEstado());
        System.out.println("✓ Test crear usuario válido - PASS");
    }
    
    @Test
    void testCrearUsuarioConNombreVacio() {
        Usuario usuario = new Usuario();
        usuario.setNombre("");
        usuario.setUsuario("usuariotest2");
        usuario.setCorreo("test2@example.com");
        usuario.setClave("password123");
        
        // Debería fallar por nombre vacío (trim vacío)
        Exception exception = assertThrows(Exception.class, () -> {
            usuarioService.guardarUsuario(usuario);
        });
        
        // Puede ser NullPointerException o similar por el trim en línea 35
        System.out.println("✓ Test crear usuario con nombre vacío - PASS (Error detectado: " + exception.getClass().getSimpleName() + ")");
    }
    
    @Test
    void testCrearUsuarioConCorreoDuplicado() {
        // Crear primer usuario
        Usuario usuario1 = new Usuario();
        usuario1.setNombre("Usuario Uno");
        usuario1.setUsuario("usuario1");
        usuario1.setCorreo("duplicado@example.com");
        usuario1.setClave("password123");
        usuarioService.guardarUsuario(usuario1);
        
        // Intentar crear segundo usuario con mismo correo
        Usuario usuario2 = new Usuario();
        usuario2.setNombre("Usuario Dos");
        usuario2.setUsuario("usuario2");
        usuario2.setCorreo("duplicado@example.com");
        usuario2.setClave("password456");
        
        // El servicio no valida duplicados explícitamente, pero la base de datos debería fallar
        Exception exception = assertThrows(Exception.class, () -> {
            usuarioService.guardarUsuario(usuario2);
        });
        
        System.out.println("✓ Test crear usuario con correo duplicado - PASS (Error detectado: " + exception.getClass().getSimpleName() + ")");
    }
    
    @Test
    void testCrearUsuarioConUsuarioDuplicado() {
        // Crear primer usuario
        Usuario usuario1 = new Usuario();
        usuario1.setNombre("Usuario Uno");
        usuario1.setUsuario("usuarioduplicado");
        usuario1.setCorreo("uno@example.com");
        usuario1.setClave("password123");
        usuarioService.guardarUsuario(usuario1);
        
        // Intentar crear segundo usuario con mismo username
        Usuario usuario2 = new Usuario();
        usuario2.setNombre("Usuario Dos");
        usuario2.setUsuario("usuarioduplicado");
        usuario2.setCorreo("dos@example.com");
        usuario2.setClave("password456");
        
        // El servicio no valida duplicados explícitamente, pero la base de datos debería fallar
        Exception exception = assertThrows(Exception.class, () -> {
            usuarioService.guardarUsuario(usuario2);
        });
        
        System.out.println("✓ Test crear usuario con username duplicado - PASS (Error detectado: " + exception.getClass().getSimpleName() + ")");
    }
    
    // ==================== PRUEBAS DE EDICIÓN ====================
    
    @Test
    void testActualizarUsuarioConDatosValidos() {
        // Crear usuario
        Usuario usuario = new Usuario();
        usuario.setNombre("Usuario Original");
        usuario.setUsuario("usuarioeditar");
        usuario.setCorreo("editar@example.com");
        usuario.setClave("password123");
        Usuario guardado = usuarioService.guardarUsuario(usuario);
        
        // Actualizar el usuario
        guardado.setNombre("Usuario Actualizado");
        guardado.setCorreo("actualizado@example.com");
        
        Usuario actualizado = usuarioService.guardarUsuario(guardado);
        
        assertNotNull(actualizado);
        assertEquals("Usuario Actualizado", actualizado.getNombre());
        assertEquals("actualizado@example.com", actualizado.getCorreo());
        System.out.println("✓ Test actualizar usuario - PASS");
    }
    
    @Test
    void testActualizarUsuarioSinCambiarClave() {
        // Crear usuario
        Usuario usuario = new Usuario();
        usuario.setNombre("Usuario Clave");
        usuario.setUsuario("usuarioclave");
        usuario.setCorreo("clave@example.com");
        usuario.setClave("password123");
        Usuario guardado = usuarioService.guardarUsuario(usuario);
        
        // Actualizar solo el nombre (manteniendo la misma clave)
        guardado.setNombre("Usuario Clave Actualizado");
        // No enviamos la clave nuevamente, usamos la misma
        guardado.setClave("password123");
        
        Usuario actualizado = usuarioService.guardarUsuario(guardado);
        
        // Verificar que el nombre se actualizó
        assertEquals("Usuario Clave Actualizado", actualizado.getNombre());
        // La clave se actualiza pero con el mismo valor
        assertNotNull(actualizado.getClave());
        System.out.println("✓ Test actualizar usuario sin cambiar clave - PASS");
    }
    
    @Test
    void testActualizarUsuarioConNuevaClave() {
        // Crear usuario
        Usuario usuario = new Usuario();
        usuario.setNombre("Usuario Nueva Clave");
        usuario.setUsuario("usuarionuevaclave");
        usuario.setCorreo("nuevaclave@example.com");
        usuario.setClave("password123");
        Usuario guardado = usuarioService.guardarUsuario(usuario);
        
        String claveOriginal = guardado.getClave();
        
        // Actualizar con nueva clave
        guardado.setNombre("Usuario Nueva Clave Actualizado");
        guardado.setClave("newpassword456");
        
        Usuario actualizado = usuarioService.guardarUsuario(guardado);
        
        // La clave debe cambiar
        assertNotEquals(claveOriginal, actualizado.getClave());
        System.out.println("✓ Test actualizar usuario con nueva clave - PASS");
    }
    
    // ==================== PRUEBAS DE ELIMINACIÓN ====================
    
    @Test
    void testEliminarUsuario() {
        // Crear usuario
        Usuario usuario = new Usuario();
        usuario.setNombre("Usuario a Eliminar");
        usuario.setUsuario("usuarioeliminar");
        usuario.setCorreo("eliminar@example.com");
        usuario.setClave("password123");
        Usuario guardado = usuarioService.guardarUsuario(usuario);
        
        Long usuarioId = guardado.getId();
        
        // Eliminar el usuario
        usuarioService.eliminarUsuario(usuarioId);
        
        // Verificar que el usuario existe pero con estado 2 (eliminado)
        Optional<Usuario> eliminado = usuarioService.obtenerUsuarioPorId(usuarioId);
        assertTrue(eliminado.isPresent());
        assertEquals(2, eliminado.get().getEstado());
        System.out.println("✓ Test eliminar usuario - PASS");
    }
    
    @Test
    void testEliminarUsuarioInexistente() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            usuarioService.eliminarUsuario(99999L);
        });
        
        assertTrue(exception.getMessage().contains("no encontrado"));
        System.out.println("✓ Test eliminar usuario inexistente - PASS");
    }
    
    // ==================== PRUEBAS DE CAMBIO DE ESTADO ====================
    
    @Test
    void testCambiarEstadoUsuarioActivarDesactivar() {
        // Crear usuario
        Usuario usuario = new Usuario();
        usuario.setNombre("Usuario Estado");
        usuario.setUsuario("usuarioestado");
        usuario.setCorreo("estado@example.com");
        usuario.setClave("password123");
        Usuario guardado = usuarioService.guardarUsuario(usuario);
        
        // Estado inicial debe ser 1 (activo)
        assertEquals(1, guardado.getEstado());
        
        // Cambiar a inactivo (0)
        Optional<Usuario> inactivo = usuarioService.cambiarEstadoUsuario(guardado.getId());
        assertTrue(inactivo.isPresent());
        assertEquals(0, inactivo.get().getEstado());
        
        // Cambiar a activo (1) nuevamente
        Optional<Usuario> activo = usuarioService.cambiarEstadoUsuario(guardado.getId());
        assertTrue(activo.isPresent());
        assertEquals(1, activo.get().getEstado());
        System.out.println("✓ Test cambiar estado usuario (activar/desactivar) - PASS");
    }
    
    // ==================== PRUEBAS DE BÚSQUEDA ====================
    
    @Test
    void testBuscarUsuarioPorNombreUsuario() {
        // Crear usuario
        Usuario usuario = new Usuario();
        usuario.setNombre("Usuario Búsqueda");
        usuario.setUsuario("usuariobusqueda");
        usuario.setCorreo("busqueda@example.com");
        usuario.setClave("password123");
        usuarioService.guardarUsuario(usuario);
        
        // Buscar por username
        Optional<Usuario> encontrado = usuarioService.findByUsuario("usuariobusqueda");
        
        assertTrue(encontrado.isPresent());
        assertEquals("usuariobusqueda", encontrado.get().getUsuario());
        System.out.println("✓ Test buscar usuario por username - PASS");
    }
    
    @Test
    void testBuscarUsuarioPorCorreo() {
        // Crear usuario
        Usuario usuario = new Usuario();
        usuario.setNombre("Usuario Búsqueda Correo");
        usuario.setUsuario("usuariocorreo");
        usuario.setCorreo("correo@example.com");
        usuario.setClave("password123");
        usuarioService.guardarUsuario(usuario);
        
        // Buscar por correo
        Optional<Usuario> encontrado = usuarioService.findByCorreo("correo@example.com");
        
        assertTrue(encontrado.isPresent());
        assertEquals("correo@example.com", encontrado.get().getCorreo());
        System.out.println("✓ Test buscar usuario por correo - PASS");
    }
    
    // ==================== PRUEBAS DE VERIFICACIÓN DE EXISTENCIA ====================
    
    @Test
    void testVerificarExistenciaUsuario() {
        // Crear usuario
        Usuario usuario = new Usuario();
        usuario.setNombre("Usuario Existencia");
        usuario.setUsuario("usuarioexistencia");
        usuario.setCorreo("existencia@example.com");
        usuario.setClave("password123");
        usuarioService.guardarUsuario(usuario);
        
        // Verificar que existe
        assertTrue(usuarioService.existeUsuario("usuarioexistencia"));
        assertFalse(usuarioService.existeUsuario("usuarioinexistente"));
        System.out.println("✓ Test verificar existencia de usuario - PASS");
    }
    
    @Test
    void testVerificarExistenciaCorreo() {
        // Crear usuario
        Usuario usuario = new Usuario();
        usuario.setNombre("Usuario Correo Existencia");
        usuario.setUsuario("usuariocorreoexistencia");
        usuario.setCorreo("correoexistencia@example.com");
        usuario.setClave("password123");
        usuarioService.guardarUsuario(usuario);
        
        // Verificar que existe
        assertTrue(usuarioService.existeCorreo("correoexistencia@example.com"));
        assertFalse(usuarioService.existeCorreo("correoinexistente@example.com"));
        System.out.println("✓ Test verificar existencia de correo - PASS");
    }
    
    // ==================== PRUEBAS DE LISTADO ====================
    
    @Test
    void testListarUsuarios() {
        // Crear algunos usuarios
        Usuario usuario1 = new Usuario();
        usuario1.setNombre("Usuario Listado 1");
        usuario1.setUsuario("usuariolistado1");
        usuario1.setCorreo("listado1@example.com");
        usuario1.setClave("password123");
        usuarioService.guardarUsuario(usuario1);
        
        Usuario usuario2 = new Usuario();
        usuario2.setNombre("Usuario Listado 2");
        usuario2.setUsuario("usuariolistado2");
        usuario2.setCorreo("listado2@example.com");
        usuario2.setClave("password456");
        usuarioService.guardarUsuario(usuario2);
        
        // Listar usuarios
        List<Usuario> usuarios = usuarioService.listarUsuarios();
        
        assertNotNull(usuarios);
        assertTrue(usuarios.size() >= 2);
        System.out.println("✓ Test listar usuarios - PASS (Total usuarios: " + usuarios.size() + ")");
    }
    
    // ==================== PRUEBAS DE CONTEO ====================
    
    @Test
    void testContarUsuarios() {
        // Crear un usuario
        Usuario usuario = new Usuario();
        usuario.setNombre("Usuario Conteo");
        usuario.setUsuario("usuarioconteto");
        usuario.setCorreo("conteo@example.com");
        usuario.setClave("password123");
        usuarioService.guardarUsuario(usuario);
        
        // Contar usuarios
        long total = usuarioService.contarUsuarios();
        
        assertTrue(total > 0);
        System.out.println("✓ Test contar usuarios - PASS (Total: " + total + ")");
    }
    
    // ==================== PRUEBAS DE VALIDACIÓN DE CONTRASEÑA ====================
    
    @Test
    void testVerificarContrasenaCorrecta() {
        // Crear usuario
        Usuario usuario = new Usuario();
        usuario.setNombre("Usuario Password");
        usuario.setUsuario("usuariopassword");
        usuario.setCorreo("password@example.com");
        usuario.setClave("password123");
        Usuario guardado = usuarioService.guardarUsuario(usuario);
        
        // Verificar contraseña correcta
        boolean resultado = usuarioService.verificarContrasena("password123", guardado.getClave());
        assertTrue(resultado);
        System.out.println("✓ Test verificar contraseña correcta - PASS");
    }
    
    @Test
    void testVerificarContrasenaIncorrecta() {
        // Crear usuario
        Usuario usuario = new Usuario();
        usuario.setNombre("Usuario Password Incorrecta");
        usuario.setUsuario("usuariopasswordincorrecta");
        usuario.setCorreo("passwordincorrecta@example.com");
        usuario.setClave("password123");
        Usuario guardado = usuarioService.guardarUsuario(usuario);
        
        // Verificar contraseña incorrecta
        boolean resultado = usuarioService.verificarContrasena("passwordincorrecto", guardado.getClave());
        assertFalse(resultado);
        System.out.println("✓ Test verificar contraseña incorrecta - PASS");
    }
}