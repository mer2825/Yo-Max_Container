package com.example.acceso.repository;

import com.example.acceso.model.PedidoWeb;
import com.example.acceso.model.EstadoPedidoWeb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // Importar JpaSpecificationExecutor
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoWebRepository extends JpaRepository<PedidoWeb, Long>, JpaSpecificationExecutor<PedidoWeb> { // Extender JpaSpecificationExecutor
    
    Optional<PedidoWeb> findByNumeroPedido(String numeroPedido);
    
    List<PedidoWeb> findByClienteId(Long clienteId);
    
    List<PedidoWeb> findByEstado(EstadoPedidoWeb estado);
    
    List<PedidoWeb> findByEstadoOrderByFechaPedidoDesc(EstadoPedidoWeb estado);
}