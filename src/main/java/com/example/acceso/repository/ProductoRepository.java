package com.example.acceso.repository;

import com.example.acceso.model.Categoria;
import com.example.acceso.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    // Método para contar productos por ID de categoría
    long countByCategoriaId(Long categoriaId);

    // Nuevo método para contar productos por ID de categoría que NO estén en un estado específico
    long countByCategoriaIdAndEstadoNot(Long categoriaId, Integer estado);

    // Método para listar productos por estado
    List<Producto> findByEstado(Integer estado);

    // Nuevo método para listar productos que NO estén en un estado específico
    List<Producto> findAllByEstadoNot(Integer estado);

    // Nuevo método para contar productos por estado
    long countByEstado(Integer estado);

    // Método para verificar si existen productos asociados a una categoría que NO estén en un estado específico
    boolean existsByCategoriaAndEstadoNot(Categoria categoria, Integer estado);

    // Nuevo método para buscar un producto por nombre ignorando mayúsculas/minúsculas y que no esté en un estado específico
    Optional<Producto> findByNombreIgnoreCaseAndEstadoNot(String nombre, Integer estado);
}
