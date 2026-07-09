package com.example.acceso.repository;

import com.example.acceso.model.CambioProducto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CambioProductoRepository extends JpaRepository<CambioProducto, Long> {

    List<CambioProducto> findByVentaOriginalId(Long ventaId);

    boolean existsByDetalleVentaOriginalIdAndEstado(Long detalleVentaId, String estado);
}

