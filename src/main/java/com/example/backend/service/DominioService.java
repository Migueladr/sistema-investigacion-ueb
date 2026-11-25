package com.example.backend.service;

import com.example.backend.entity.Dominio;
import com.example.backend.entity.Linea;
import com.example.backend.repository.DominioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DominioService {

    private final DominioRepository repository;

    public DominioService(DominioRepository repository) {
        this.repository = repository;
    }

    public List<Dominio> dominios() {
        return repository.dominios();
    }

    public List<Linea> lineas(String codDominio) {
        return repository.lineas(codDominio);
    }

    public Map<String, Object> vistaDominios(String codDominio) {
        return Map.of(
                "dominios", dominios(),
                "lineas", lineas(codDominio),
                "proyectos", codDominio == null || codDominio.isBlank()
                        ? List.of() : repository.proyectosPorDominio(codDominio),
                "puntajes", repository.parametrosPuntaje());
    }

    public Map<String, Object> detalleLinea(String codLinea) {
        return Map.of("investigadores", repository.investigadoresPorLinea(codLinea));
    }
}
