package com.example.acceso.repository;

import com.example.acceso.model.StockMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovimientoRepository extends JpaRepository<StockMovimiento, Long> {
    List<StockMovimiento> findByProductoIdOrderByFechaDesc(Long productoId);
}
