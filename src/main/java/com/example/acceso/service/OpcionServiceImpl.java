package com.example.acceso.service;

import com.example.acceso.model.Opcion;
import com.example.acceso.repository.OpcionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OpcionServiceImpl implements OpcionService {

    private final OpcionRepository opcionRepository;

    @Autowired
    public OpcionServiceImpl(OpcionRepository opcionRepository) {
        this.opcionRepository = opcionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Opcion> listarOpciones() {
        return opcionRepository.findAll();
    }
}
