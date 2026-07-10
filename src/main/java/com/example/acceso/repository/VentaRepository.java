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

    @Query(value = "SELECT COALESCE(MAX(CAST(REGEXP_REPLACE(numero_venta, '[^0-9]', '', 'g') AS INTEGER)), 0) FROM ventas WHERE numero_venta LIKE CONCAT(:prefijo, '%')", nativeQuery = true)
    Integer findMaxCorrelativoByNumeroVentaPrefijo(@Param("prefijo") String prefijo);

    List<Venta> findBySesionCajaIdAndMetodoPago(Long sesionCajaId, String metodoPago);

    List<Venta> findBySesionCajaId(Long sesionCajaId);

    @Query("SELECT DISTINCT v FROM Venta v LEFT JOIN FETCH v.notasCredito WHERE v.estado != 2 ORDER BY v.id DESC")
    List<Venta> findAllWithNotasCredito();

    @Query("SELECT v.metodoPago, SUM(v.total) FROM Venta v WHERE v.sesionCaja.id = :sesionId AND v.fechaVenta >= :inicio AND v.fechaVenta <= :fin GROUP BY v.metodoPago")
    List<Object[]> obtenerVentasPorMetodoPagoEnSesion(@Param("sesionId") Long sesionId, @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query("SELECT COUNT(v) FROM Venta v WHERE v.sesionCaja.id = :sesionId AND v.fechaVenta >= :inicio AND v.fechaVenta <= :fin")
    long countBySesionCajaIdAndFechaVentaBetween(@Param("sesionId") Long sesionId, @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query(value = "SELECT p.nombre, SUM(dv.cantidad) AS unidades_vendidas, SUM(dv.precio_unitario * dv.cantidad) AS total_dinero " +
            "FROM detalles_venta dv " +
            "JOIN productos p ON dv.producto_id = p.id " +
            "JOIN ventas v ON dv.venta_id = v.id " +
            "WHERE v.sesion_caja_id = :sesionId " +
            "GROUP BY p.nombre " +
            "ORDER BY unidades_vendidas DESC " +
            "LIMIT 5", nativeQuery = true)
    List<Object[]> findTop5ProductosMasVendidosPorSesion(@Param("sesionId") Long sesionId);

    @Query(value = "SELECT p.nombre, SUM(dv.cantidad) AS unidades_vendidas, SUM(dv.precio_unitario * dv.cantidad) AS total_dinero " +
            "FROM detalles_venta dv " +
            "JOIN productos p ON dv.producto_id = p.id " +
            "JOIN ventas v ON dv.venta_id = v.id " +
            "WHERE v.fecha_venta BETWEEN :inicio AND :fin " +
            "GROUP BY p.nombre " +
            "ORDER BY unidades_vendidas DESC " +
            "LIMIT 5", nativeQuery = true)
    List<Object[]> findTop5ProductosMasVendidosPorPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query("SELECT DISTINCT d.producto.id FROM DetalleVenta d " +
           "WHERE d.venta.fechaVenta >= :fecha")
    List<Long> findProductoIdsVendidosDespues(
        @Param("fecha") LocalDateTime fecha);

    @Query("SELECT v FROM Venta v JOIN FETCH v.cliente JOIN FETCH v.detalles d JOIN FETCH d.producto WHERE v.id = :id")
    Optional<Venta> findByIdWithDetallesAndProductos(@Param("id") Long id);

    // Consulta optimizada para obtener ventas de una sesión con datos mínimos necesarios
    @Query("SELECT v.id, v.serieCorrelativo, v.total, v.metodoPago, v.fechaVenta, v.cliente.nombre " +
           "FROM Venta v " +
           "WHERE v.sesionCaja.id = :sesionId " +
           "ORDER BY v.fechaVenta DESC")
    List<Object[]> findVentasLigerasBySesionId(@Param("sesionId") Long sesionId);

    // Consulta optimizada para obtener ventas por período con datos mínimos
    @Query("SELECT v.id, v.serieCorrelativo, v.total, v.metodoPago, v.fechaVenta, v.cliente.nombre, v.sesionCaja.id " +
           "FROM Venta v " +
           "WHERE v.fechaVenta >= :inicio AND v.fechaVenta <= :fin " +
           "ORDER BY v.fechaVenta DESC")
    List<Object[]> findVentasLigerasByPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}
