package com.example.acceso.repository;

import com.example.acceso.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    // Métodos para el borrado lógico y filtrado por estado
    List<Categoria> findAllByEstadoNot(Integer estado);
    long countByEstadoNot(Integer estado);
    List<Categoria> findByEstado(Integer estado);

    // Nuevo método para buscar una categoría por nombre ignorando mayúsculas/minúsculas y que no esté en un estado específico
    Optional<Categoria> findByNombreIgnoreCaseAndEstadoNot(String nombre, Integer estado);
}
