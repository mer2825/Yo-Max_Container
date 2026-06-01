package com.example.acceso.repository;

import com.example.acceso.model.ProductoImagen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoImagenRepository extends JpaRepository<ProductoImagen, Long> {
    void deleteByIdIn(List<Long> ids);
}
