package com.example.acceso.repository;

import com.example.acceso.model.Egreso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface EgresoRepository extends JpaRepository<Egreso, Long> {

    List<Egreso> findByFechaBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin);

    List<Egreso> findByTipoEgresoAndFechaBetween(String tipoEgreso, LocalDateTime fechaInicio, LocalDateTime fechaFin);

    List<Egreso> findByEstado(Integer estado);

    @Query("SELECT SUM(e.monto) FROM Egreso e WHERE e.fecha >= :inicio AND e.fecha <= :fin")
    BigDecimal sumMontoByFechaBetween(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query("SELECT e.tipoEgreso, SUM(e.monto) FROM Egreso e WHERE e.fecha >= :inicio AND e.fecha <= :fin GROUP BY e.tipoEgreso")
    List<Object[]> sumMontoByTipoEgresoAndFechaBetween(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query("SELECT DISTINCT e.tipoEgreso FROM Egreso e ORDER BY e.tipoEgreso")
    List<String> findAllTiposEgreso();
}

