package com.example.acceso.repository;

import com.example.acceso.model.MovimientoCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface MovimientoCajaRepository extends JpaRepository<MovimientoCaja, Long> {

    List<MovimientoCaja> findBySesionId(Long sesionId);

    List<MovimientoCaja> findBySesionIdOrderByFechaDesc(Long sesionId);

    @Query("SELECT SUM(mc.monto) FROM MovimientoCaja mc WHERE mc.sesion.id = :sesionId AND mc.tipo = 'INGRESO'")
    BigDecimal sumIngresosBySesionId(@Param("sesionId") Long sesionId);

    @Query("SELECT SUM(mc.monto) FROM MovimientoCaja mc WHERE mc.sesion.id = :sesionId AND mc.tipo = 'RETIRO'")
    BigDecimal sumRetirosBySesionId(@Param("sesionId") Long sesionId);

    @Query("SELECT mc FROM MovimientoCaja mc WHERE mc.sesion.id = :sesionId AND mc.fecha >= :inicio AND mc.fecha <= :fin ORDER BY mc.fecha DESC")
    List<MovimientoCaja> findBySesionIdAndFechaBetween(@Param("sesionId") Long sesionId, @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}
