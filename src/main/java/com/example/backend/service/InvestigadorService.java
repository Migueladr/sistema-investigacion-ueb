package com.example.backend.service;

import com.example.backend.dto.InvestigadorForm;
import com.example.backend.entity.Investigador;
import com.example.backend.exception.BusinessException;
import com.example.backend.repository.InvestigadorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class InvestigadorService {

    private final InvestigadorRepository repository;

    public InvestigadorService(InvestigadorRepository repository) {
        this.repository = repository;
    }

    public List<Investigador> listar(String q, String tipo, int page, int size) {
        return repository.listar(q, tipo, page, size);
    }

    public long contar(String q, String tipo) {
        return repository.contar(q, tipo);
    }

    public Investigador buscar(int id) {
        Investigador i = repository.buscar(id);
        if (i == null) {
            throw new BusinessException("No existe el investigador indicado");
        }
        return i;
    }

    public void guardar(InvestigadorForm form) {
        repository.guardar(form);
    }

    public void editar(InvestigadorForm form) {
        if (repository.actualizar(form) == 0) {
            throw new BusinessException("No existe el investigador indicado");
        }
    }

    public Map<String, Object> detalle(int id) {
        Investigador i = buscar(id);
        return Map.of(
                "investigador", i,
                "proyectos", repository.proyectosVigentes(id),
                "publicaciones", repository.publicaciones(id),
                "lineas", repository.lineas(id),
                "supervisores", repository.supervisores(id),
                "supervisados", repository.supervisados(id),
                "historial", repository.historialParticipacion(id),
                "sinLinea", repository.sinLinea());
    }
}
