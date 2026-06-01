package com.example.acceso.repository;

import com.example.acceso.model.DetallePedidoWeb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetallePedidoWebRepository extends JpaRepository<DetallePedidoWeb, Long> {
}
