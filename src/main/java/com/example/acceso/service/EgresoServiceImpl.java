package com.example.acceso.service;

import com.example.acceso.model.Egreso;
import com.example.acceso.model.MovimientoCaja;
import com.example.acceso.repository.EgresoRepository;
import com.example.acceso.repository.MovimientoCajaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EgresoServiceImpl implements EgresoService {

    private final MovimientoCajaRepository movimientoCajaRepository;
    private final EgresoRepository egresoRepository;

    @Autowired
    public EgresoServiceImpl(MovimientoCajaRepository movimientoCajaRepository, EgresoRepository egresoRepository) {
        this.movimientoCajaRepository = movimientoCajaRepository;
        this.egresoRepository = egresoRepository;
    }

    @Override
    public Egreso crearEgreso(Egreso egreso) {
        if (egreso.getEstado() == null) {
            egreso.setEstado(1);
        }
        return egresoRepository.save(egreso);
    }

    @Override
    public Optional<Egreso> obtenerEgresoPorId(Long id) {
        return egresoRepository.findById(id);
    }

    @Override
    public List<Egreso> listarTodosLosEgresos() {
        return egresoRepository.findAll().stream()
            .filter(e -> e.getEstado() != 2)
            .collect(Collectors.toList());
    }

    @Override
    public List<Egreso> listarEgresosActivos() {
        return egresoRepository.findByEstado(1);
    }

    @Override
    public void actualizarEgreso(Long id, Egreso egresoActualizado) {
        Optional<Egreso> egresoOpt = egresoRepository.findById(id);
        if (egresoOpt.isPresent()) {
            Egreso egreso = egresoOpt.get();
            if (egresoActualizado.getFecha() != null) {
                egreso.setFecha(egresoActualizado.getFecha());
            }
            if (egresoActualizado.getTipoEgreso() != null) {
                egreso.setTipoEgreso(egresoActualizado.getTipoEgreso());
            }
            if (egresoActualizado.getComentario() != null) {
                egreso.setComentario(egresoActualizado.getComentario());
            }
            if (egresoActualizado.getMonto() != null) {
                egreso.setMonto(egresoActualizado.getMonto());
            }
            if (egresoActualizado.getNumeroOperacion() != null) {
                egreso.setNumeroOperacion(egresoActualizado.getNumeroOperacion());
            }
            egresoRepository.save(egreso);
        }
    }

    @Override
    public void eliminarEgreso(Long id) {
        Optional<Egreso> egresoOpt = egresoRepository.findById(id);
        if (egresoOpt.isPresent()) {
            Egreso egreso = egresoOpt.get();
            egreso.setEstado(2); // Soft delete
            egresoRepository.save(egreso);
        }
    }

    @Override
    public List<String> obtenerTiposEgreso() {
        return movimientoCajaRepository.findDistinctCategoriasByTipoRetiro();
    }

    /**
     * Obtener egresos desde movimientos_caja (RETIRO)
     */
    public List<MovimientoCaja> listarEgresosDesdeMovimientosCaja(LocalDate desde, LocalDate hasta) {
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.atTime(23, 59, 59);
        return movimientoCajaRepository.findRetirosByFechaBetween(inicio, fin);
    }
}


