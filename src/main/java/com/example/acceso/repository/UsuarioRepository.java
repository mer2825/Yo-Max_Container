package com.example.acceso.repository;

import com.example.acceso.model.Perfil;
import com.example.acceso.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Métodos para encontrar usuarios por sus propiedades únicas
    Optional<Usuario> findByUsuario(String usuario);

    Optional<Usuario> findByCorreo(String correo);

    // Métodos para verificar la existencia de usuarios
    boolean existsByUsuario(String usuario);

    boolean existsByCorreo(String correo);

    // Método para verificar si existen usuarios asociados a un perfil
    boolean existsByPerfil(Perfil perfil);

    // Nuevo método para verificar si existen usuarios asociados a un perfil que NO estén en un estado específico
    boolean existsByPerfilAndEstadoNot(Perfil perfil, Integer estado);

    // Métodos para el borrado lógico
    List<Usuario> findAllByEstadoNot(Integer estado);

    long countByEstadoNot(Integer estado);

    // Nuevo método para listar todos los usuarios excepto uno por ID y que no estén en un estado específico
    List<Usuario> findByIdNotAndEstadoNot(Long id, Integer estado);
}
