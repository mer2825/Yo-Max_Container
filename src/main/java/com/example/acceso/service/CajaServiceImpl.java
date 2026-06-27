package com.example.acceso.service;

import com.example.acceso.dto.CierreResumenDTO;
import com.example.acceso.dto.EventoAuditoriaDTO;
import com.example.acceso.dto.MovimientoLogDTO;
import com.example.acceso.dto.ReportePeriodoDTO;
import com.example.acceso.dto.ResumenDiaDTO;
import com.example.acceso.dto.ResumenSesionActivaDTO;
import com.example.acceso.model.MovimientoCaja;
import com.example.acceso.model.SesionCaja;
import com.example.acceso.model.Usuario;
import com.example.acceso.model.Venta;
import com.example.acceso.repository.MovimientoCajaRepository;
import com.example.acceso.repository.SesionCajaRepository;
import com.example.acceso.repository.UsuarioRepository;
import com.example.acceso.repository.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CajaServiceImpl implements CajaService {

    private final SesionCajaRepository sesionCajaRepository;
    private final VentaRepository ventaRepository;
    private final MovimientoCajaRepository movimientoCajaRepository;
    private final UsuarioRepository usuarioRepository;

    public CajaServiceImpl(SesionCajaRepository sesionCajaRepository,
                           VentaRepository ventaRepository,
                           MovimientoCajaRepository movimientoCajaRepository,
                           UsuarioRepository usuarioRepository) {
        this.sesionCajaRepository = sesionCajaRepository;
        this.ventaRepository = ventaRepository;
        this.movimientoCajaRepository = movimientoCajaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional
    public SesionCaja abrirCaja(BigDecimal montoInicial, Long usuarioId) {
        // Verificar que no haya una sesión abierta
        Optional<SesionCaja> sesionAbierta = sesionCajaRepository.findByEstado("ABIERTA");
        if (sesionAbierta.isPresent()) {
            throw new RuntimeException("Ya existe una sesión de caja abierta.");
        }

        // Obtener el usuario
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Crear nueva sesión de caja
        SesionCaja sesionCaja = new SesionCaja();
        sesionCaja.setMontoInicial(montoInicial);
        sesionCaja.setFechaApertura(LocalDateTime.now());
        sesionCaja.setUsuarioApertura(usuario);
        sesionCaja.setEstado("ABIERTA");

        SesionCaja sesionGuardada = sesionCajaRepository.save(sesionCaja);

        // Si se proporcionó un monto inicial, registrarlo como traspaso desde la sesión anterior
        if (montoInicial != null && montoInicial.compareTo(BigDecimal.ZERO) > 0) {
            Optional<SesionCaja> ultimaSesionCerrada = sesionCajaRepository.findFirstByEstadoOrderByFechaCierreDesc("CERRADA");
            if (ultimaSesionCerrada.isPresent()) {
                SesionCaja sesionAnterior = ultimaSesionCerrada.get();
                sesionAnterior.setSaldoTraspasado(montoInicial);
                sesionCajaRepository.save(sesionAnterior);
            }
        }

        return sesionGuardada;
    }

    @Override
    @Transactional
    public SesionCaja cerrarCaja(BigDecimal montoDeclarado,
                                 String motivoDiferencia,
                                 String observaciones,
                                 Long usuarioId) {

        SesionCaja sesion = sesionCajaRepository.findByEstado("ABIERTA")
                .orElseThrow(() -> new RuntimeException("No hay sesión abierta"));

        if ("CERRADA".equals(sesion.getEstado())) {
            throw new RuntimeException("Esta sesión ya fue cerrada y no puede modificarse.");
        }

        if (montoDeclarado == null) {
            throw new RuntimeException("montoDeclarado es obligatorio");
        }
        if (montoDeclarado.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("montoDeclarado no puede ser negativo");
        }

        // Calcular efectivo esperado (mismo cálculo que obtenerResumenParaCierre)
        CierreResumenDTO resumen = obtenerResumenParaCierre();
        BigDecimal efectivoEsperado = resumen.getEfectivoEsperado();
        BigDecimal diferencia = montoDeclarado.subtract(efectivoEsperado);

        sesion.setFechaCierre(LocalDateTime.now());

        // Nombres de campos según tu entidad SesionCaja
        sesion.setMontoCierreDeclarado(montoDeclarado);
        sesion.setMontoCierreEsperado(efectivoEsperado);
        sesion.setDiferencia(diferencia);

        sesion.setMotivoDiferencia(motivoDiferencia);
        sesion.setObservaciones(observaciones);

        if (usuarioId != null) {
            usuarioRepository.findById(usuarioId)
                    .ifPresent(sesion::setUsuarioCierre);
        }

        sesion.setEstado("CERRADA");

        SesionCaja sesionCerrada = sesionCajaRepository.save(sesion);

        // Calcular y guardar el saldo traspasado para la próxima sesión
        BigDecimal saldoTraspasado = calcularMontoEsperado(sesion.getId());
        sesionCerrada.setSaldoTraspasado(saldoTraspasado);
        sesionCajaRepository.save(sesionCerrada);

        return sesionCerrada;
    }

    @Override
    @Transactional
    public SesionCaja registrarMovimiento(Long sesionId, String tipo, BigDecimal monto, String motivo, String categoria, Long usuarioId) {
        // Validar que la sesión exista y esté abierta
        SesionCaja sesion = sesionCajaRepository.findById(sesionId)
                .orElseThrow(() -> new RuntimeException("Sesión de caja no encontrada"));

        if (!"ABIERTA".equals(sesion.getEstado())) {
            throw new RuntimeException("La sesión de caja no está abierta");
        }

        // Validar que el monto sea mayor a 0
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El monto debe ser mayor a 0");
        }

        // Validar que el motivo no esté vacío y tenga longitud mínima
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new RuntimeException("La descripción es obligatoria");
        }
        if (motivo.trim().length() < 10) {
            throw new RuntimeException("La descripción debe tener al menos 10 caracteres");
        }

        // Validar que la categoría no esté vacía
        if (categoria == null || categoria.trim().isEmpty()) {
            throw new RuntimeException("La categoría es obligatoria");
        }

        // Validar categoría según el tipo de movimiento
        if ("RETIRO".equalsIgnoreCase(tipo)) {
            if (!esCategoriaRetiroValida(categoria)) {
                throw new RuntimeException("Categoría de retiro no válida. Valores permitidos: PAGO_PROVEEDOR, GASTO_OPERACION, RETIRO_DUENO, PAGO_SERVICIOS, CAMBIO_TURNO, OTRO");
            }
        } else if ("INGRESO".equalsIgnoreCase(tipo)) {
            if (!esCategoriaIngresoValida(categoria)) {
                throw new RuntimeException("Categoría de ingreso no válida. Valores permitidos: FONDO_ADICIONAL, DEVOLUCION_CLIENTE, INGRESO_OTRO");
            }
        }

        // Si es RETIRO, validar que no supere el efectivo disponible
        if ("RETIRO".equalsIgnoreCase(tipo)) {
            BigDecimal efectivoDisponible = calcularEfectivoDisponible(sesionId);
            if (monto.compareTo(efectivoDisponible) > 0) {
                throw new RuntimeException("El monto del retiro no puede superar el efectivo disponible en caja");
            }
        }

        // Obtener el usuario
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Crear y guardar el movimiento
        MovimientoCaja movimiento = new MovimientoCaja();
        movimiento.setTipo(tipo.toUpperCase());
        movimiento.setMonto(monto);
        movimiento.setMotivo(motivo);
        movimiento.setCategoria(categoria.toUpperCase());
        movimiento.setUsuario(usuario);
        movimiento.setSesion(sesion);
        movimiento.setFecha(LocalDateTime.now());

        movimientoCajaRepository.save(movimiento);

        return sesion;
    }

    // Método auxiliar para validar categorías de retiro
    private boolean esCategoriaRetiroValida(String categoria) {
        return categoria.equalsIgnoreCase("PAGO_PROVEEDOR") ||
               categoria.equalsIgnoreCase("GASTO_OPERACION") ||
               categoria.equalsIgnoreCase("RETIRO_DUENO") ||
               categoria.equalsIgnoreCase("PAGO_SERVICIOS") ||
               categoria.equalsIgnoreCase("CAMBIO_TURNO") ||
               categoria.equalsIgnoreCase("OTRO");
    }

    // Método auxiliar para validar categorías de ingreso
    private boolean esCategoriaIngresoValida(String categoria) {
        return categoria.equalsIgnoreCase("FONDO_ADICIONAL") ||
               categoria.equalsIgnoreCase("DEVOLUCION_CLIENTE") ||
               categoria.equalsIgnoreCase("INGRESO_OTRO");
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenSesionActivaDTO obtenerResumenSesionActiva() {
        // Buscar la sesión abierta
        SesionCaja sesion = sesionCajaRepository.findByEstado("ABIERTA")
                .orElse(null);

        if (sesion == null) {
            return null;
        }

        // Calcular total de ventas del día agrupadas por método de pago
        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        LocalDateTime finDia = LocalDate.now().atTime(23, 59, 59);

        List<Object[]> ventasPorMetodoPago = ventaRepository.obtenerVentasPorMetodoPagoEnSesion(
                sesion.getId(), inicioDia, finDia);

        Map<String, BigDecimal> ventasPorMetodoPagoMap = new HashMap<>();
        BigDecimal totalVentas = BigDecimal.ZERO;

        for (Object[] row : ventasPorMetodoPago) {
            String metodoPago = (String) row[0];
            BigDecimal total = (BigDecimal) row[1];
            ventasPorMetodoPagoMap.put(metodoPago, total);
            totalVentas = totalVentas.add(total);
        }

        // Número de transacciones
        Long numeroTransacciones = ventaRepository.countBySesionCajaIdAndFechaVentaBetween(
                sesion.getId(), inicioDia, finDia);

        // Ticket promedio
        BigDecimal ticketPromedio = BigDecimal.ZERO;
        if (numeroTransacciones > 0) {
            ticketPromedio = totalVentas.divide(new BigDecimal(numeroTransacciones), 2, RoundingMode.HALF_UP);
        }

        // Calcular métricas separadas
        BigDecimal soloEfectivo = BigDecimal.ZERO;
        BigDecimal otrosMedios = BigDecimal.ZERO;
        
        for (Map.Entry<String, BigDecimal> entry : ventasPorMetodoPagoMap.entrySet()) {
            if ("EFECTIVO".equalsIgnoreCase(entry.getKey())) {
                soloEfectivo = entry.getValue();
            } else {
                otrosMedios = otrosMedios.add(entry.getValue());
            }
        }
        
        // Efectivo real en caja: fondo inicial + ventas efectivo + ingresos manuales - retiros
        BigDecimal efectivoEnCaja = sesion.getMontoInicial()
            .add(soloEfectivo)
            .add(movimientoCajaRepository.sumIngresosBySesionId(sesion.getId()) != null ? movimientoCajaRepository.sumIngresosBySesionId(sesion.getId()) : BigDecimal.ZERO)
            .subtract(movimientoCajaRepository.sumRetirosBySesionId(sesion.getId()) != null ? movimientoCajaRepository.sumRetirosBySesionId(sesion.getId()) : BigDecimal.ZERO);

        // Crear y retornar el DTO
        ResumenSesionActivaDTO dto = new ResumenSesionActivaDTO();
        dto.setSesionId(sesion.getId());
        dto.setFechaApertura(sesion.getFechaApertura());
        dto.setMontoInicial(sesion.getMontoInicial());
        dto.setTotalVentas(totalVentas);
        dto.setSoloEfectivo(soloEfectivo);
        dto.setOtrosMedios(otrosMedios);
        dto.setNumeroTransacciones(numeroTransacciones);
        dto.setTicketPromedio(ticketPromedio);
        dto.setEfectivoEnCaja(efectivoEnCaja);
        dto.setVentasPorMetodoPago(ventasPorMetodoPagoMap);

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<SesionCaja> obtenerSesionActiva() {
        return sesionCajaRepository.findByEstado("ABIERTA");
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<SesionCaja> obtenerSesionesCerradasPorPeriodo(LocalDateTime inicio, LocalDateTime fin) {
        return sesionCajaRepository.findSesionesCerradasPorPeriodo(inicio, fin);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<SesionCaja> obtenerTodasLasSesionesCerradas() {
        return sesionCajaRepository.findByEstadoOrderByFechaAperturaDesc("CERRADA");
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<SesionCaja> obtenerSesionPorId(Long id) {
        return sesionCajaRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> obtenerDetalleSesion(Long sesionId) {
        java.util.Map<String, Object> detalle = new java.util.HashMap<>();
        
        SesionCaja sesion = sesionCajaRepository.findById(sesionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));
        
        detalle.put("sesion", sesion);
        
        // Ventas del período
        List<Venta> ventas = ventaRepository.findBySesionCajaId(sesionId);
        detalle.put("ventas", ventas);
        
        // Movimientos manuales
        List<MovimientoCaja> movimientos = movimientoCajaRepository.findBySesionIdOrderByFechaDesc(sesionId);
        detalle.put("movimientos", movimientos);
        
        // Resumen por método de pago
        java.util.Map<String, BigDecimal> ventasPorMetodoPago = new java.util.HashMap<>();
        BigDecimal totalVentas = BigDecimal.ZERO;
        for (Venta venta : ventas) {
            String metodoPago = venta.getMetodoPago();
            ventasPorMetodoPago.put(metodoPago,
                    ventasPorMetodoPago.getOrDefault(metodoPago, BigDecimal.ZERO).add(venta.getTotal()));
            totalVentas = totalVentas.add(venta.getTotal());
        }
        detalle.put("ventasPorMetodoPago", ventasPorMetodoPago);
        detalle.put("totalVentas", totalVentas);
        
        // Diferencia de arqueo
        detalle.put("montoEsperado", sesion.getMontoCierreEsperado());
        detalle.put("montoDeclarado", sesion.getMontoCierreDeclarado());
        detalle.put("diferencia", sesion.getDiferencia());
        
        return detalle;
    }

    @Override
    @Transactional(readOnly = true)
    public ReportePeriodoDTO obtenerReportePeriodo(LocalDate desde, LocalDate hasta) {
        // Convertir a LocalDateTime para las consultas
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.atTime(23, 59, 59);

        // Consultar todas las sesiones cerradas en ese rango
        List<SesionCaja> sesiones = sesionCajaRepository.findSesionesCerradasPorPeriodo(inicio, fin);

        // Totales por día
        Map<LocalDate, ResumenDiaDTO> resumenPorDiaMap = new HashMap<>();
        BigDecimal totalPeriodo = BigDecimal.ZERO;
        Long totalTransacciones = 0L;
        LocalDate mejorDia = null;
        BigDecimal mejorDiaTotal = BigDecimal.ZERO;

        // Ventas por método de pago
        Map<String, BigDecimal> ventasPorMetodoPago = new HashMap<>();

        // Procesar cada sesión
        for (SesionCaja sesion : sesiones) {
            List<Venta> ventas = ventaRepository.findBySesionCajaId(sesion.getId());

            for (Venta venta : ventas) {
                LocalDate fechaVenta = venta.getFechaVenta().toLocalDate();

                // Actualizar resumen por día
                ResumenDiaDTO resumenDia = resumenPorDiaMap.get(fechaVenta);
                if (resumenDia == null) {
                    resumenDia = new ResumenDiaDTO();
                    resumenDia.setFecha(fechaVenta);
                    resumenDia.setTotalVentas(BigDecimal.ZERO);
                    resumenDia.setNumeroTransacciones(0L);
                    resumenDia.setTicketPromedio(BigDecimal.ZERO);
                    resumenPorDiaMap.put(fechaVenta, resumenDia);
                }

                resumenDia.setTotalVentas(resumenDia.getTotalVentas().add(venta.getTotal()));
                resumenDia.setNumeroTransacciones(resumenDia.getNumeroTransacciones() + 1);

                // Actualizar totales del período
                totalPeriodo = totalPeriodo.add(venta.getTotal());
                totalTransacciones++;

                // Actualizar ventas por método de pago
                String metodoPago = venta.getMetodoPago();
                ventasPorMetodoPago.put(metodoPago,
                        ventasPorMetodoPago.getOrDefault(metodoPago, BigDecimal.ZERO).add(venta.getTotal()));

                // Actualizar mejor día
                BigDecimal totalDia = resumenDia.getTotalVentas();
                if (mejorDia == null || totalDia.compareTo(mejorDiaTotal) > 0) {
                    mejorDia = fechaVenta;
                    mejorDiaTotal = totalDia;
                }
            }
        }

        // Calcular ticket promedio por día
        for (ResumenDiaDTO resumenDia : resumenPorDiaMap.values()) {
            if (resumenDia.getNumeroTransacciones() > 0) {
                BigDecimal ticketPromedio = resumenDia.getTotalVentas()
                        .divide(new BigDecimal(resumenDia.getNumeroTransacciones()), 2, RoundingMode.HALF_UP);
                resumenDia.setTicketPromedio(ticketPromedio);
            }
        }

        // Ticket promedio general
        BigDecimal ticketPromedioGeneral = BigDecimal.ZERO;
        if (totalTransacciones > 0) {
            ticketPromedioGeneral = totalPeriodo.divide(new BigDecimal(totalTransacciones), 2, RoundingMode.HALF_UP);
        }

        // Top 5 productos más vendidos del período
        List<Object[]> top5ProductosRaw = ventaRepository.findTop5ProductosMasVendidosPorPeriodo(inicio, fin);
        List<com.example.acceso.dto.ProductoMasVendidoDTO> top5Productos = new ArrayList<>();
        for (Object[] row : top5ProductosRaw) {
            String nombre = (String) row[0];
            Long unidades = ((Number) row[1]).longValue();
            BigDecimal total = (BigDecimal) row[2];
            top5Productos.add(new com.example.acceso.dto.ProductoMasVendidoDTO(nombre, unidades, total));
        }

        // Convertir el mapa a lista para el DTO
        List<ResumenDiaDTO> resumenPorDiaList = new ArrayList<>(resumenPorDiaMap.values());

        // Crear y retornar el DTO
        ReportePeriodoDTO dto = new ReportePeriodoDTO();
        dto.setDesde(desde);
        dto.setHasta(hasta);
        dto.setTotalPeriodo(totalPeriodo);
        dto.setTicketPromedio(ticketPromedioGeneral);
        dto.setMejorDia(mejorDia);
        dto.setMejorDiaTotal(mejorDiaTotal);
        dto.setTotalSesiones((long) sesiones.size());
        dto.setVentasPorMetodoPago(ventasPorMetodoPago);
        dto.setResumenPorDia(resumenPorDiaList);
        dto.setTop5Productos(top5Productos);

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<EventoAuditoriaDTO> obtenerLogAuditoriaSesionActiva() {
        java.util.List<EventoAuditoriaDTO> eventos = new java.util.ArrayList<>();
        
        // Buscar la sesión abierta
        SesionCaja sesion = sesionCajaRepository.findByEstado("ABIERTA")
                .orElse(null);
        
        if (sesion == null) {
            return eventos;
        }
        
        // Agregar evento de apertura de caja
        EventoAuditoriaDTO apertura = new EventoAuditoriaDTO(
            EventoAuditoriaDTO.TipoEvento.APERTURA_CAJA,
            "Apertura de Caja",
            "Fondo inicial registrado",
            sesion.getUsuarioApertura() != null ? sesion.getUsuarioApertura().getNombre() : "Sistema",
            sesion.getFechaApertura(),
            sesion.getMontoInicial(),
            "bi-lock-open",
            "text-secondary",
            null
        );
        eventos.add(apertura);
        
        // Obtener ventas de la sesión
        List<Venta> ventas = ventaRepository.findBySesionCajaId(sesion.getId());
        
        for (Venta venta : ventas) {
            String metodoPago = venta.getMetodoPago();
            String usuario = "Cliente";
            if (venta.getCliente() != null && venta.getCliente().getNombre() != null) {
                usuario = venta.getCliente().getNombre();
            }
            
            if ("EFECTIVO".equalsIgnoreCase(metodoPago)) {
                // Venta en efectivo
                EventoAuditoriaDTO evento = new EventoAuditoriaDTO(
                    EventoAuditoriaDTO.TipoEvento.VENTA_EFECTIVO,
                    "Venta en Efectivo",
                    "Comprobante: " + (venta.getSerieCorrelativo() != null ? venta.getSerieCorrelativo() : "N° " + venta.getId()),
                    usuario,
                    venta.getFechaVenta(),
                    venta.getTotal(),
                    "bi-cart",
                    "text-success",
                    null
                );
                eventos.add(evento);
            } else {
                // Venta por otro medio (Yape, transferencia, tarjeta)
                EventoAuditoriaDTO evento = new EventoAuditoriaDTO(
                    EventoAuditoriaDTO.TipoEvento.VENTA_OTRO_MEDIO,
                    "Venta - " + metodoPago,
                    "Comprobante: " + (venta.getSerieCorrelativo() != null ? venta.getSerieCorrelativo() : "N° " + venta.getId()),
                    usuario,
                    venta.getFechaVenta(),
                    venta.getTotal(),
                    "bi-cart",
                    "text-warning",
                    "No impacta efectivo"
                );
                eventos.add(evento);
            }
        }
        
        // Obtener movimientos manuales
        List<MovimientoCaja> movimientos = movimientoCajaRepository.findBySesionIdOrderByFechaDesc(sesion.getId());
        
        for (MovimientoCaja movimiento : movimientos) {
            String usuario = movimiento.getUsuario() != null ? movimiento.getUsuario().getNombre() : "Sistema";
            String categoria = movimiento.getCategoria() != null ? movimiento.getCategoria() : "Sin categoría";
            
            if ("RETIRO".equalsIgnoreCase(movimiento.getTipo())) {
                EventoAuditoriaDTO evento = new EventoAuditoriaDTO(
                    EventoAuditoriaDTO.TipoEvento.RETIRO_MANUAL,
                    "Retiro Manual",
                    categoria + " - " + movimiento.getMotivo(),
                    usuario,
                    movimiento.getFecha(),
                    movimiento.getMonto(),
                    "bi-arrow-down-circle",
                    "text-danger",
                    null
                );
                eventos.add(evento);
            } else if ("INGRESO".equalsIgnoreCase(movimiento.getTipo())) {
                EventoAuditoriaDTO evento = new EventoAuditoriaDTO(
                    EventoAuditoriaDTO.TipoEvento.INGRESO_MANUAL,
                    "Ingreso Manual",
                    categoria + " - " + movimiento.getMotivo(),
                    usuario,
                    movimiento.getFecha(),
                    movimiento.getMonto(),
                    "bi-arrow-up-circle",
                    "text-warning",
                    null
                );
                eventos.add(evento);
            }
        }
        
        // Ordenar por fecha descendente (más reciente primero)
        eventos.sort((a, b) -> b.getFecha().compareTo(a.getFecha()));
        
        return eventos;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<EventoAuditoriaDTO> obtenerLogAuditoriaSesion(Long sesionId) {
        java.util.List<EventoAuditoriaDTO> eventos = new java.util.ArrayList<>();
        
        // Buscar la sesión
        SesionCaja sesion = sesionCajaRepository.findById(sesionId)
                .orElse(null);
        
        if (sesion == null) {
            return eventos;
        }
        
        // Agregar evento de apertura de caja
        EventoAuditoriaDTO apertura = new EventoAuditoriaDTO(
            EventoAuditoriaDTO.TipoEvento.APERTURA_CAJA,
            "Apertura de Caja",
            "Fondo inicial registrado",
            sesion.getUsuarioApertura() != null ? sesion.getUsuarioApertura().getNombre() : "Sistema",
            sesion.getFechaApertura(),
            sesion.getMontoInicial(),
            "bi-lock-open",
            "text-secondary",
            null
        );
        eventos.add(apertura);
        
        // Obtener ventas de la sesión
        List<Venta> ventas = ventaRepository.findBySesionCajaId(sesion.getId());
        
        for (Venta venta : ventas) {
            String metodoPago = venta.getMetodoPago();
            String usuario = "Cliente";
            if (venta.getCliente() != null && venta.getCliente().getNombre() != null) {
                usuario = venta.getCliente().getNombre();
            }
            
            if ("EFECTIVO".equalsIgnoreCase(metodoPago)) {
                // Venta en efectivo
                EventoAuditoriaDTO evento = new EventoAuditoriaDTO(
                    EventoAuditoriaDTO.TipoEvento.VENTA_EFECTIVO,
                    "Venta en Efectivo",
                    "Comprobante: " + (venta.getSerieCorrelativo() != null ? venta.getSerieCorrelativo() : "N° " + venta.getId()),
                    usuario,
                    venta.getFechaVenta(),
                    venta.getTotal(),
                    "bi-cart",
                    "text-success",
                    null
                );
                eventos.add(evento);
            } else {
                // Venta por otro medio (Yape, transferencia, tarjeta)
                EventoAuditoriaDTO evento = new EventoAuditoriaDTO(
                    EventoAuditoriaDTO.TipoEvento.VENTA_OTRO_MEDIO,
                    "Venta - " + metodoPago,
                    "Comprobante: " + (venta.getSerieCorrelativo() != null ? venta.getSerieCorrelativo() : "N° " + venta.getId()),
                    usuario,
                    venta.getFechaVenta(),
                    venta.getTotal(),
                    "bi-cart",
                    "text-warning",
                    "No impacta efectivo"
                );
                eventos.add(evento);
            }
        }
        
        // Obtener movimientos manuales
        List<MovimientoCaja> movimientos = movimientoCajaRepository.findBySesionIdOrderByFechaDesc(sesion.getId());
        
        for (MovimientoCaja movimiento : movimientos) {
            String usuario = movimiento.getUsuario() != null ? movimiento.getUsuario().getNombre() : "Sistema";
            String categoria = movimiento.getCategoria() != null ? movimiento.getCategoria() : "Sin categoría";
            
            if ("RETIRO".equalsIgnoreCase(movimiento.getTipo())) {
                EventoAuditoriaDTO evento = new EventoAuditoriaDTO(
                    EventoAuditoriaDTO.TipoEvento.RETIRO_MANUAL,
                    "Retiro Manual",
                    categoria + " - " + movimiento.getMotivo(),
                    usuario,
                    movimiento.getFecha(),
                    movimiento.getMonto(),
                    "bi-arrow-down-circle",
                    "text-danger",
                    null
                );
                eventos.add(evento);
            } else if ("INGRESO".equalsIgnoreCase(movimiento.getTipo())) {
                EventoAuditoriaDTO evento = new EventoAuditoriaDTO(
                    EventoAuditoriaDTO.TipoEvento.INGRESO_MANUAL,
                    "Ingreso Manual",
                    categoria + " - " + movimiento.getMotivo(),
                    usuario,
                    movimiento.getFecha(),
                    movimiento.getMonto(),
                    "bi-arrow-up-circle",
                    "text-warning",
                    null
                );
                eventos.add(evento);
            }
        }
        
        // Ordenar por fecha descendente (más reciente primero)
        eventos.sort((a, b) -> b.getFecha().compareTo(a.getFecha()));
        
        return eventos;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<MovimientoLogDTO> construirLogSesion(Long sesionId) {
        java.util.List<MovimientoLogDTO> log = new java.util.ArrayList<>();
        
        // Buscar la sesión
        SesionCaja sesion = sesionCajaRepository.findById(sesionId)
                .orElse(null);
        
        if (sesion == null) {
            return log;
        }
        
        // Evento de apertura
        String usuarioApertura = sesion.getUsuarioApertura() != null ? sesion.getUsuarioApertura().getNombre() : "Sistema";
        log.add(new MovimientoLogDTO("APERTURA", "Fondo inicial registrado", sesion.getMontoInicial(),
                "+", true, null, null, usuarioApertura, sesion.getFechaApertura()));
        
        // Obtener ventas de la sesión
        List<Venta> ventas = ventaRepository.findBySesionCajaId(sesion.getId());
        
        for (Venta venta : ventas) {
            String metodoPago = venta.getMetodoPago();
            String usuario = "Cliente";
            if (venta.getCliente() != null && venta.getCliente().getNombre() != null) {
                usuario = venta.getCliente().getNombre();
            }
            String comprobante = venta.getSerieCorrelativo() != null ? venta.getSerieCorrelativo() : "N° " + venta.getId();
            
            if ("EFECTIVO".equalsIgnoreCase(metodoPago)) {
                log.add(new MovimientoLogDTO("VENTA_EFECTIVO", "Comprobante: " + comprobante,
                        venta.getTotal(), "+", true, metodoPago, null, usuario, venta.getFechaVenta()));
            } else {
                log.add(new MovimientoLogDTO("VENTA_OTRO", "Comprobante: " + comprobante + " (" + metodoPago + ")",
                        venta.getTotal(), "+", false, metodoPago, null, usuario, venta.getFechaVenta()));
            }
        }
        
        // Obtener movimientos manuales
        List<MovimientoCaja> movimientos = movimientoCajaRepository.findBySesionIdOrderByFechaDesc(sesion.getId());
        
        for (MovimientoCaja movimiento : movimientos) {
            String usuario = movimiento.getUsuario() != null ? movimiento.getUsuario().getNombre() : "Sistema";
            String categoria = movimiento.getCategoria() != null ? movimiento.getCategoria() : "Sin categoría";
            
            if ("RETIRO".equalsIgnoreCase(movimiento.getTipo())) {
                log.add(new MovimientoLogDTO("RETIRO", categoria + " - " + movimiento.getMotivo(),
                        movimiento.getMonto(), "−", true, null, categoria, usuario, movimiento.getFecha()));
            } else if ("INGRESO".equalsIgnoreCase(movimiento.getTipo())) {
                log.add(new MovimientoLogDTO("INGRESO", categoria + " - " + movimiento.getMotivo(),
                        movimiento.getMonto(), "+", true, null, categoria, usuario, movimiento.getFecha()));
            }
        }
        
        // Evento de cierre si existe
        if (sesion.getFechaCierre() != null) {
            String usuarioCierre = sesion.getUsuarioCierre() != null ? sesion.getUsuarioCierre().getNombre() : "Sistema";
            log.add(new MovimientoLogDTO("CIERRE", "Cierre de caja - Efectivo declarado: S/ " + 
                    (sesion.getMontoCierreDeclarado() != null ? sesion.getMontoCierreDeclarado() : "0"),
                    sesion.getMontoCierreDeclarado(), "=", true, null, null, usuarioCierre, sesion.getFechaCierre()));
        }
        
        // Ordenar por fecha descendente (más reciente primero)
        log.sort((a, b) -> b.getFecha().compareTo(a.getFecha()));
        
        return log;
    }

    @Override
    @Transactional(readOnly = true)
    public java.math.BigDecimal obtenerSaldoParaApertura() {
        // Buscar la última sesión cerrada ordenada por fecha de cierre descendente
        return sesionCajaRepository
            .findTopByEstadoOrderByFechaCierreDesc("CERRADA")
            .map(sesion -> {
                // Si la sesión tiene saldo traspasado definido, usar ese
                if (sesion.getSaldoTraspasado() != null) {
                    return sesion.getSaldoTraspasado();
                }
                // Sino, usar el monto declarado por el cajero
                if (sesion.getMontoCierreDeclarado() != null) {
                    return sesion.getMontoCierreDeclarado();
                }
                return BigDecimal.ZERO;
            })
            .orElse(BigDecimal.ZERO); // primera vez que se abre la caja
    }

    @Override
    @Transactional(readOnly = true)
    public boolean debeAlertarCierre(SesionCaja sesion) {
        if (sesion == null || "CERRADA".equals(sesion.getEstado())) {
            return false;
        }
        int horaActual = LocalDateTime.now().getHour();
        return horaActual >= 20; // 8 PM o más
    }

    @Override
    @Transactional(readOnly = true)
    public boolean haySesionDelDiaAnteriorSinCerrar() {
        return sesionCajaRepository.findByEstado("ABIERTA")
            .map(sesion -> {
                LocalDate fechaApertura = sesion.getFechaApertura().toLocalDate();
                LocalDate hoy = LocalDate.now();
                // Si la sesión se abrió antes de hoy, está sin cerrar
                return fechaApertura.isBefore(hoy);
            })
            .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<SesionCaja> obtenerUltimaSesionCerrada() {
        return sesionCajaRepository.findTopByEstadoOrderByFechaCierreDesc("CERRADA");
    }

    // Método auxiliar para calcular el monto esperado en caja
    private BigDecimal calcularMontoEsperado(Long sesionId) {
        BigDecimal montoEsperado = BigDecimal.ZERO;

        // Obtener la sesión para el monto inicial
        SesionCaja sesion = sesionCajaRepository.findById(sesionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        montoEsperado = montoEsperado.add(sesion.getMontoInicial());

        // Sumar ventas en efectivo
        List<Venta> ventasEfectivo = ventaRepository.findBySesionCajaIdAndMetodoPago(sesionId, "Efectivo");
        for (Venta venta : ventasEfectivo) {
            montoEsperado = montoEsperado.add(venta.getTotal());
        }

        // Sumar ingresos
        BigDecimal totalIngresos = movimientoCajaRepository.sumIngresosBySesionId(sesionId);
        if (totalIngresos != null) {
            montoEsperado = montoEsperado.add(totalIngresos);
        }

        // Restar retiros
        BigDecimal totalRetiros = movimientoCajaRepository.sumRetirosBySesionId(sesionId);
        if (totalRetiros != null) {
            montoEsperado = montoEsperado.subtract(totalRetiros);
        }

        return montoEsperado;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<SesionCaja> obtenerHistorialSesiones(LocalDate desde, LocalDate hasta, String filtroArqueo) {
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.atTime(23, 59, 59);
        
        List<SesionCaja> sesiones = sesionCajaRepository.findSesionesCerradasPorPeriodo(inicio, fin);
        
        // Aplicar filtro de arqueo
        if ("CON_DIFERENCIA".equalsIgnoreCase(filtroArqueo)) {
            sesiones = sesiones.stream()
                .filter(s -> s.getDiferencia() != null && s.getDiferencia().compareTo(BigDecimal.ZERO) != 0)
                .collect(java.util.stream.Collectors.toList());
        } else if ("SIN_DIFERENCIA".equalsIgnoreCase(filtroArqueo)) {
            sesiones = sesiones.stream()
                .filter(s -> s.getDiferencia() == null || s.getDiferencia().compareTo(BigDecimal.ZERO) == 0)
                .collect(java.util.stream.Collectors.toList());
        }
        
        return sesiones;
    }

    @Override
    @Transactional(readOnly = true)
    public CierreResumenDTO obtenerResumenParaCierre() {
        SesionCaja sesion = sesionCajaRepository.findByEstado("ABIERTA")
                .orElseThrow(() -> new RuntimeException("No hay sesión de caja abierta"));

        // Ventas vinculadas a esta sesión
        List<Venta> ventas = ventaRepository.findBySesionCajaId(sesion.getId());

        // Totales por método de pago (garantizar no-null)
        BigDecimal totalEfectivo = ventas.stream()
                .filter(v -> v != null && "Efectivo".equalsIgnoreCase(v.getMetodoPago()))
                .map(Venta::getTotal)
                .filter(t -> t != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalYape = ventas.stream()
                .filter(v -> "Yape".equalsIgnoreCase(v.getMetodoPago())
                        || "Transferencia".equalsIgnoreCase(v.getMetodoPago()))
                .map(Venta::getTotal)
                .filter(t -> t != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalVentas = ventas.stream()
                .map(Venta::getTotal)
                .filter(t -> t != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Movimientos manuales
        List<MovimientoCaja> movimientos = movimientoCajaRepository.findBySesionId(sesion.getId());

        BigDecimal totalIngresos = movimientos.stream()
                .filter(m -> "INGRESO".equalsIgnoreCase(m.getTipo()))
                .map(MovimientoCaja::getMonto)
                .filter(t -> t != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRetiros = movimientos.stream()
                .filter(m -> "RETIRO".equalsIgnoreCase(m.getTipo()))
                .map(MovimientoCaja::getMonto)
                .filter(t -> t != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal montoInicial = sesion.getMontoInicial() != null ? sesion.getMontoInicial() : BigDecimal.ZERO;

        BigDecimal efectivoEsperado = montoInicial
                .add(totalEfectivo)
                .add(totalIngresos)
                .subtract(totalRetiros);

        // Duración de la sesión (si falta fecha apertura, evitar excepción)
        LocalDateTime fechaApertura = sesion.getFechaApertura() != null ? sesion.getFechaApertura() : LocalDateTime.now();
        long minutosAbierta = java.time.Duration.between(fechaApertura, LocalDateTime.now()).toMinutes();
        String duracion = String.format("%dh %02dmin", minutosAbierta / 60, minutosAbierta % 60);

        BigDecimal ticketPromedio = ventas.isEmpty() ? BigDecimal.ZERO
                : totalVentas.divide(BigDecimal.valueOf(ventas.size()), 2, RoundingMode.HALF_UP);

        return new CierreResumenDTO(
                sesion.getId(),
                fechaApertura,
                montoInicial,
                totalEfectivo,
                totalYape,
                totalVentas,
                totalIngresos,
                totalRetiros,
                efectivoEsperado,
                ventas.size(),
                ticketPromedio,
                duracion,
                movimientos.size()
        );
    }

    // Método auxiliar para calcular el efectivo disponible
    private BigDecimal calcularEfectivoDisponible(Long sesionId) {
        BigDecimal efectivoDisponible = BigDecimal.ZERO;

        // Obtener la sesión para el monto inicial
        SesionCaja sesion = sesionCajaRepository.findById(sesionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        efectivoDisponible = efectivoDisponible.add(sesion.getMontoInicial());

        // Sumar ventas en efectivo
        List<Venta> ventasEfectivo = ventaRepository.findBySesionCajaIdAndMetodoPago(sesionId, "Efectivo");
        for (Venta venta : ventasEfectivo) {
            efectivoDisponible = efectivoDisponible.add(venta.getTotal());
        }

        // Sumar ingresos
        BigDecimal totalIngresos = movimientoCajaRepository.sumIngresosBySesionId(sesionId);
        if (totalIngresos != null) {
            efectivoDisponible = efectivoDisponible.add(totalIngresos);
        }

        // Restar retiros
        BigDecimal totalRetiros = movimientoCajaRepository.sumRetirosBySesionId(sesionId);
        if (totalRetiros != null) {
            efectivoDisponible = efectivoDisponible.subtract(totalRetiros);
        }

        return efectivoDisponible;
    }
}