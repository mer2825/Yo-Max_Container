package com.example.acceso.service;

import com.example.acceso.dto.AuditDetailsDto;
import com.example.acceso.model.Categoria;

import java.util.List;
import java.util.Optional;

public interface CategoriaService {
    // Se añade el método que faltaba
    List<Categoria> listarCategorias();
    List<Categoria> listarCategoriasActivas();
    Optional<Categoria> obtenerCategoriaPorId(Long id);
    Categoria guardarCategoria(Categoria categoria);
    void eliminarCategoria(Long id);
    Optional<Categoria> cambiarEstadoCategoria(Long id);
    AuditDetailsDto getAuditDetails(Long id);
}