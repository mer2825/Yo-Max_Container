package com.example.acceso.repository;

import com.example.acceso.model.VentaWeb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VentaWebRepository extends JpaRepository<VentaWeb, Long> {
}
