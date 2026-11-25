package com.example.backend.service;

import com.example.backend.dto.EstudianteForm;
import com.example.backend.entity.Estudiante;
import com.example.backend.exception.BusinessException;
import com.example.backend.repository.EstudianteRepository;
import com.example.backend.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class EstudianteService {

    private final EstudianteRepository repository;

    public EstudianteService(EstudianteRepository repository) {
        this.repository = repository;
    }

    public List<Estudiante> listar(String q, int page, int size) {
        return repository.listar(q, page, size);
    }

    public long contar(String q) {
        return repository.contar(q);
    }

    public Estudiante buscar(int id) {
        Estudiante e = repository.buscar(id);
        if (e == null) {
            throw new BusinessException("No existe el estudiante indicado");
        }
        return e;
    }

    public void guardar(EstudianteForm form) {
        repository.guardar(form);
    }

    public void editar(EstudianteForm form) {
        if (repository.actualizar(form) == 0) {
            throw new BusinessException("No existe el estudiante indicado");
        }
    }

    public Map<String, Object> detalle(int id) {
        return Map.of("estudiante", buscar(id), "proyectos", repository.proyectos(id));
    }

    public List<Map<String, Object>> misProyectos() {
        if (!SecurityUtils.esEstudiante()) {
            return List.of();
        }
        return repository.proyectosDelAutenticado();
    }
}
