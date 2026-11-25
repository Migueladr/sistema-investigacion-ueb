package com.example.backend.service;

import com.example.backend.dto.SupervisionForm;
import com.example.backend.entity.Supervision;
import com.example.backend.exception.BusinessException;
import com.example.backend.repository.SupervisionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SupervisionService {

    private final SupervisionRepository repository;

    public SupervisionService(SupervisionRepository repository) {
        this.repository = repository;
    }

    public List<Supervision> listar(boolean vigentes, String q) {
        return repository.listar(vigentes, q);
    }

    public void registrar(SupervisionForm form) {
        repository.registrar(form);
    }

    public void finalizar(int doctor, int noDoctor, String fechaInicio, String fechaFin) {
        if (repository.finalizar(doctor, noDoctor, fechaInicio, fechaFin) == 0) {
            throw new BusinessException("No se encontró la supervisión indicada");
        }
    }

    public Map<String, Object> catalogos() {
        return Map.of("doctores", repository.doctores(), "noDoctores", repository.noDoctores());
    }

    public List<Map<String, Object>> historial() {
        return repository.historial();
    }
}
