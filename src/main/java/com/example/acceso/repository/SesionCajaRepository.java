package com.example.acceso.repository;

import com.example.acceso.model.SesionCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SesionCajaRepository extends JpaRepository<SesionCaja, Long> {

    Optional<SesionCaja> findByEstado(String estado);

    List<SesionCaja> findByUsuarioAperturaIdOrderByFechaAperturaDesc(Long usuarioId);

    @Query("SELECT sc FROM SesionCaja sc WHERE sc.estado = 'ABIERTA' ORDER BY sc.fechaApertura DESC")
    Optional<SesionCaja> findSesionAbierta();

    @Query("SELECT sc FROM SesionCaja sc WHERE sc.fechaApertura >= :inicio AND sc.fechaApertura <= :fin ORDER BY sc.fechaApertura DESC")
    List<SesionCaja> findByFechaAperturaBetween(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    List<SesionCaja> findByEstadoOrderByFechaAperturaDesc(String estado);

    Optional<SesionCaja> findFirstByEstadoOrderByFechaCierreDesc(String estado);

    Optional<SesionCaja> findTopByEstadoOrderByFechaCierreDesc(String estado);

    @Query("SELECT sc FROM SesionCaja sc WHERE sc.estado = 'CERRADA' AND sc.fechaApertura >= :inicio AND sc.fechaApertura <= :fin ORDER BY sc.fechaApertura DESC")
    List<SesionCaja> findSesionesCerradasPorPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    List<SesionCaja> findByFechaAperturaBetweenOrderByFechaAperturaDesc(LocalDateTime desde, LocalDateTime hasta);
}
