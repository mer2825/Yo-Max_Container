package com.example.acceso.repository;

import com.example.acceso.model.Categoria;
import com.example.acceso.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // --- Métodos existentes requeridos por ProductoService/CategoriaServiceImpl ---

    List<Producto> findAllByEstadoNot(Integer estado);

    long countByEstado(Integer estado);

    Optional<Producto> findByNombreIgnoreCaseAndEstadoNot(String nombre, Integer estado);

    boolean existsByCategoriaAndEstadoNot(Categoria categoria, int estado);

    // --- Métodos usados por otros flujos ---

    List<Producto> findByEstado(Integer estado);

    List<Producto> findByEstadoNot(Integer estado);
}

