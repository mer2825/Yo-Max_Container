package com.example.acceso.service;

import com.example.acceso.model.Egreso;
import java.util.List;
import java.util.Optional;

public interface EgresoService {
    
    Egreso crearEgreso(Egreso egreso);
    
    Optional<Egreso> obtenerEgresoPorId(Long id);
    
    List<Egreso> listarTodosLosEgresos();
    
    List<Egreso> listarEgresosActivos();
    
    void actualizarEgreso(Long id, Egreso egresoActualizado);
    
    void eliminarEgreso(Long id);
    
    List<String> obtenerTiposEgreso();
}

