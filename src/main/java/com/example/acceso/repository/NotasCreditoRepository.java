package com.example.acceso.repository;

import com.example.acceso.model.NotasCredito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotasCreditoRepository extends JpaRepository<NotasCredito, Long> {

    List<NotasCredito> findByVentaId(Long ventaId);

    Optional<NotasCredito> findTopByVentaIdOrderByFechaEmisionDesc(Long ventaId);

    List<NotasCredito> findByVenta_SesionCaja_Id(Long sesionCajaId);

    List<NotasCredito> findByFechaEmisionBetween(LocalDateTime desde, LocalDateTime hasta);

    List<NotasCredito> findAllByOrderByFechaEmisionDesc();

    List<NotasCredito> findByFechaEmisionBetweenOrderByFechaEmisionDesc(LocalDateTime desde, LocalDateTime hasta);
}
