package com.example.acceso.service;

import com.example.acceso.dto.AuditDetailsDto;
import com.example.acceso.model.Perfil;

import java.util.List;
import java.util.Optional;

public interface PerfilService {
    List<Perfil> listarPerfiles();
    Optional<Perfil> obtenerPerfilPorId(Long id);
    Perfil guardarPerfil(Perfil perfil);
    void eliminarPerfil(Long id);
    Optional<Perfil> cambiarEstadoPerfil(Long id);
    List<Perfil> listarPerfilesActivos(); // Método para obtener solo los perfiles activos
    AuditDetailsDto getAuditDetails(Long id);
}