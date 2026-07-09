package com.example.acceso.repository;

import com.example.acceso.model.DetalleVenta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {

    // Necesario por ProductoService (línea de ventas por producto)
    List<DetalleVenta> findByProductoId(Long productoId);

    // Compatibilidad: otros servicios podrían requerir este método
    List<DetalleVenta> findByVentaId(Long ventaId);
}

