package com.example.backend.service;

import com.example.backend.repository.AuditoriaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AuditoriaService {

    private final AuditoriaRepository repository;

    public AuditoriaService(AuditoriaRepository repository) {
        this.repository = repository;
    }

    public List<Map<String, Object>> cambios(String tipo, String tabla, String usuario, String desde, String hasta) {
        return repository.cambios(tipo, tabla, usuario, desde, hasta, 300);
    }

    public List<Map<String, Object>> operaciones(String tipo, String procedimiento, String usuario,
                                                 String desde, String hasta) {
        return repository.operaciones(tipo, procedimiento, usuario, desde, hasta, 300);
    }
}
