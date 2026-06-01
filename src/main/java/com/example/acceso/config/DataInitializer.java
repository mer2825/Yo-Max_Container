package com.example.acceso.config;

import com.example.acceso.model.Cliente;
import com.example.acceso.model.Opcion;
import com.example.acceso.model.Perfil;
import com.example.acceso.model.Usuario;
import com.example.acceso.repository.ClienteRepository;
import com.example.acceso.repository.OpcionRepository;
import com.example.acceso.repository.PerfilRepository;
import com.example.acceso.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    private final OpcionRepository opcionRepository;
    private final PerfilRepository perfilRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public DataInitializer(OpcionRepository opcionRepository, PerfilRepository perfilRepository, ClienteRepository clienteRepository, UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.opcionRepository = opcionRepository;
        this.perfilRepository = perfilRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        crearOpcionesDeMenu();
        crearPerfilAdminConTodasLasOpciones();
        crearClienteSiNoExiste("Consumidor Final", "-", "-", "-");
        garantizarUsuarioAdmin();
    }

    private void crearOpcionesDeMenu() {
        crearOpcionSiNoExiste("Gestión de Usuarios", "/usuarios/listar", "bi-people");
        crearOpcionSiNoExiste("Gestión de Perfiles", "/perfiles/listar", "bi-person-check");
        crearOpcionSiNoExiste("Gestión de Categorías", "/categorias/listar", "bi-tags");
        crearOpcionSiNoExiste("Gestión de Productos", "/productos/listar", "bi-cake2");
        crearOpcionSiNoExiste("Gestión de Clientes", "/clientes/listar", "bi-person-vcard");
        crearOpcionSiNoExiste("Gestión de Empresa", "/empresa/listar", "bi-shop-window");
        crearOpcionSiNoExiste("Listado de Ventas", "/ventas/listar", "bi-receipt");
        crearOpcionSiNoExiste("Nueva Venta", "/ventas/nueva", "bi-cart-plus");
        crearOpcionSiNoExiste("Ventas Web", "/ventas_web", "bi-globe");
        crearOpcionSiNoExiste("Gestión Inventario", "/inventario/listar", "bi-boxes");
        crearOpcionSiNoExiste("Ir al Catálogo", "/catalogo", "bi-shop"); // Nueva opción añadida
    }

    private void crearPerfilAdminConTodasLasOpciones() {
        Perfil adminPerfil = perfilRepository.findByNombre("Administrador")
                .orElseGet(() -> {
                    Perfil nuevoPerfil = new Perfil();
                    nuevoPerfil.setNombre("Administrador");
                    nuevoPerfil.setDescripcion("Acceso total al sistema");
                    nuevoPerfil.setEstado(1);
                    return nuevoPerfil;
                });

        List<Opcion> todasLasOpciones = opcionRepository.findAll();
        adminPerfil.setOpciones(new HashSet<>(todasLasOpciones));
        perfilRepository.save(adminPerfil);
        System.out.println("Perfil 'Administrador' configurado con todos los permisos.");
    }

    private void garantizarUsuarioAdmin() {
        if (existeUsuarioAdministrador()) {
            System.out.println("Ya existe un administrador en la base de datos. No se creará el usuario por defecto 'usuario_original'.");
            return;
        }

        Optional<Usuario> usuarioByIdOpt = usuarioRepository.findById(1L);
        Optional<Usuario> usuarioByNombreOpt = usuarioRepository.findByUsuario("usuario_original");

        if (usuarioByIdOpt.isPresent()) {
            Usuario usuarioExistente = usuarioByIdOpt.get();
            if (!"usuario_original".equals(usuarioExistente.getUsuario())) {
                throw new RuntimeException("Error de inicialización: El ID 1 está ocupado por el usuario '" + usuarioExistente.getUsuario() + "'. No se puede crear 'usuario_original'. Por favor, corrija la base de datos.");
            }
            System.out.println("El usuario 'usuario_original' con ID 1 ya existe. No se realizarán cambios.");
        } else if (usuarioByNombreOpt.isPresent()) {
            throw new RuntimeException("Error de inicialización: El usuario 'usuario_original' ya existe con un ID diferente (" + usuarioByNombreOpt.get().getId() + "). No se puede crear con ID 1. Por favor, corrija la base de datos.");
        } else {
            System.out.println("Creando usuario administrador por defecto 'usuario_original' con ID 1.");
            
            Perfil adminPerfil = perfilRepository.findByNombre("Administrador")
                    .orElseThrow(() -> new RuntimeException("El perfil de Administrador no fue encontrado."));

            String nombre = "Administrador del Sistema";
            String usuario = "usuario_original";
            String claveCifrada = passwordEncoder.encode("usuario28@#");
            String correo = "admin@sistema.com";
            int estado = 1;
            Long perfilId = adminPerfil.getId();

            entityManager.createNativeQuery("INSERT INTO usuarios (id, nombre, usuario, clave, correo, estado, id_perfil) VALUES (?, ?, ?, ?, ?, ?, ?)")
                .setParameter(1, 1L)
                .setParameter(2, nombre)
                .setParameter(3, usuario)
                .setParameter(4, claveCifrada)
                .setParameter(5, correo)
                .setParameter(6, estado)
                .setParameter(7, perfilId)
                .executeUpdate();
            
            System.out.println("Usuario administrador por defecto 'usuario_original' creado con éxito.");
        }
    }

    private boolean existeUsuarioAdministrador() {
        List<?> admins = entityManager.createQuery("SELECT u.id FROM Usuario u WHERE lower(u.perfil.nombre) = :adminName")
                .setParameter("adminName", "administrador")
                .setMaxResults(1)
                .getResultList();
        return !admins.isEmpty();
    }

    private void crearClienteSiNoExiste(String nombre, String tipoDoc, String numDoc, String direccion) {
        if (!clienteRepository.findByNombre(nombre).isPresent()) {
            Cliente clienteGenerico = new Cliente();
            clienteGenerico.setNombre(nombre);
            clienteGenerico.setTipoDocumento(tipoDoc);
            clienteGenerico.setNumeroDocumento(numDoc);
            clienteGenerico.setDireccion(direccion);
            clienteGenerico.setEstado(1);
            clienteRepository.save(clienteGenerico);
            System.out.println("Cliente por defecto creado: " + nombre);
        }
    }

    private void crearOpcionSiNoExiste(String nombre, String ruta, String icono) {
        Opcion opcion = opcionRepository.findByRuta(ruta).orElse(null);
        if (opcion == null) {
            opcion = new Opcion();
        }
        opcion.setNombre(nombre);
        opcion.setRuta(ruta);
        opcion.setIcono(icono);
        opcion.setEstado(true);
        opcionRepository.save(opcion);
    }
}
