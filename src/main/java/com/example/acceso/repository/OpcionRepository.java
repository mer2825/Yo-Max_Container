package com.example.acceso.repository;

import com.example.acceso.model.Opcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OpcionRepository extends JpaRepository<Opcion, Long> {
    // Se restaura el método necesario para el DataInitializer
    Optional<Opcion> findByRuta(String ruta);
}
