package com.example.acceso.service;

import com.example.acceso.model.PedidoWeb;
import com.example.acceso.model.EstadoPedidoWeb;
import org.springframework.data.domain.Page; // Importar Page
import java.time.LocalDate; // Importar LocalDate

import java.util.List;
import java.util.Optional;

public interface PedidoWebService {
    PedidoWeb crearPedido(PedidoWeb pedido);
    List<PedidoWeb> listarTodosLosPedidos();
    List<PedidoWeb> listarPedidosPorCliente(Long clienteId);
    List<PedidoWeb> listarPedidosPorEstado(EstadoPedidoWeb estado);
    Optional<PedidoWeb> obtenerPedidoPorId(Long id);
    Optional<PedidoWeb> obtenerPedidoPorNumero(String numeroPedido);
    PedidoWeb aprobarPedido(Long id, Long verificadoPorId);
    PedidoWeb rechazarPedido(Long id, Long verificadoPorId, String motivoRechazo);
    PedidoWeb marcarComoProcesado(Long id);
    void eliminarPedido(Long id, String motivo);

    // Nuevo método para listar pedidos con filtros y paginación
    Page<PedidoWeb> listarPedidosConFiltros(int page, int size, String sortBy, String sortDir, String estado, LocalDate fechaDesde, LocalDate fechaHasta, String busqueda);
}