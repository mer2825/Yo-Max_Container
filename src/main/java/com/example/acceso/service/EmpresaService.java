package com.example.acceso.service;

import com.example.acceso.model.Empresa;
import com.example.acceso.repository.EmpresaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepository;

    @Autowired
    public EmpresaService(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    // Corregido: Se elimina 'readOnly = true' para permitir la creación por defecto.
    @Transactional
    public Empresa getEmpresaInfo() {
        List<Empresa> empresas = empresaRepository.findAll();
        if (empresas.isEmpty()) {
            // Si no hay ninguna empresa, crea una por defecto.
            return createDefaultEmpresa();
        } else {
            return empresas.get(0); // Retorna la primera empresa encontrada
        }
    }

    @Transactional
    public Empresa saveEmpresa(Empresa empresa) {
        return empresaRepository.saveAndFlush(empresa);
    }

    // Este método es privado y será llamado dentro de la transacción de getEmpresaInfo
    private Empresa createDefaultEmpresa() {
        Empresa defaultEmpresa = new Empresa();
        defaultEmpresa.setNombre("Nombre de tu Empresa");
        defaultEmpresa.setDireccion("Añade una dirección");
        defaultEmpresa.setTelefono("999-999-999");
        defaultEmpresa.setEmail("correo@empresa.com");
        defaultEmpresa.setNosotros("Describe tu increíble empresa aquí.");
        return empresaRepository.save(defaultEmpresa);
    }
}
