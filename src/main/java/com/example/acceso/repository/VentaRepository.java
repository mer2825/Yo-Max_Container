package com.example.acceso.repository;

import com.example.acceso.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    @Query("SELECT COUNT(v) FROM Venta v WHERE v.fechaVenta >= :startOfDay AND v.fechaVenta <= :endOfDay")
    long countByFechaVenta(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT SUM(v.total) FROM Venta v WHERE v.fechaVenta >= :startOfDay AND v.fechaVenta <= :endOfDay")
    BigDecimal sumTotalByFechaVenta(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT COUNT(v) FROM Venta v WHERE v.fechaVenta >= :startOfMonth AND v.fechaVenta <= :endOfMonth")
    long countByFechaVentaMonth(@Param("startOfMonth") LocalDateTime startOfMonth, @Param("endOfMonth") LocalDateTime endOfMonth);

    @Query("SELECT SUM(v.total) FROM Venta v WHERE v.fechaVenta >= :startOfMonth AND v.fechaVenta <= :endOfMonth")
    BigDecimal sumTotalByFechaVentaMonth(@Param("startOfMonth") LocalDateTime startOfMonth, @Param("endOfMonth") LocalDateTime endOfMonth);

    @Query(value = "SELECT p.nombre, SUM(dv.cantidad) AS unidades_vendidas, SUM(dv.precio_unitario * dv.cantidad) AS total_dinero " +
            "FROM detalles_venta dv " +
            "JOIN productos p ON dv.producto_id = p.id " +
            "JOIN ventas v ON dv.venta_id = v.id " +
            "WHERE v.fecha_venta BETWEEN :inicioSemana AND :finSemana " +
            "GROUP BY p.nombre " +
            "ORDER BY unidades_vendidas DESC " +
            "LIMIT 5", nativeQuery = true)
    List<Object[]> findTop5ProductosMasVendidosDeLaSemana(@Param("inicioSemana") LocalDateTime inicioSemana, @Param("finSemana") LocalDateTime finSemana);

    @Query(value = "SELECT p.nombre, SUM(dv.cantidad) AS unidades_vendidas, SUM(dv.precio_unitario * dv.cantidad) AS total_dinero " +
            "FROM detalles_venta dv " +
            "JOIN productos p ON dv.producto_id = p.id " +
            "JOIN ventas v ON dv.venta_id = v.id " +
            "WHERE v.fecha_venta BETWEEN :inicioHoy AND :finHoy " +
            "GROUP BY p.nombre " +
            "ORDER BY unidades_vendidas DESC " +
            "LIMIT 5", nativeQuery = true)
    List<Object[]> findTop5ProductosMasVendidosDeHoy(@Param("inicioHoy") LocalDateTime inicioHoy, @Param("finHoy") LocalDateTime finHoy);

    List<Venta> findAllByEstadoNot(Integer estado);
    List<Venta> findByEstado(Integer estado);
    List<Venta> findByFechaVentaBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin);

    Optional<Venta> findTopByNumeroVentaStartingWithOrderByNumeroVentaDesc(String prefijo);
}
