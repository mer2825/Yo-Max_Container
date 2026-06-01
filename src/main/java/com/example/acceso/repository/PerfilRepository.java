package com.example.acceso.repository;

import com.example.acceso.model.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PerfilRepository extends JpaRepository<Perfil, Long> {
    Optional<Perfil> findByNombre(String nombre);

    // Métodos para el borrado lógico y filtrado por estado
    List<Perfil> findAllByEstadoNot(Integer estado);
    long countByEstadoNot(Integer estado);
    List<Perfil> findByEstado(Integer estado);
}
