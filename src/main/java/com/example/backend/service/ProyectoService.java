package com.example.backend.service;

import com.example.backend.dto.FiltroProyecto;
import com.example.backend.dto.ProyectoForm;
import com.example.backend.entity.Proyecto;
import com.example.backend.entity.ProyectoDetalle;
import com.example.backend.exception.BusinessException;
import com.example.backend.pattern.observer.EventoDominio;
import com.example.backend.repository.ProyectoRepository;
import com.example.backend.security.SecurityUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** Orquesta proyectos: delega las operaciones atómicas a los procedimientos almacenados. */
@Service
public class ProyectoService {

    private final ProyectoRepository repository;
    private final ApplicationEventPublisher publisher;

    public ProyectoService(ProyectoRepository repository, ApplicationEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    public List<Proyecto> listar(FiltroProyecto filtro) {
        return repository.listar(filtro);
    }

    public long contar(FiltroProyecto filtro) {
        return repository.contar(filtro);
    }

    public Proyecto buscar(String codigo) {
        Proyecto proyecto = repository.buscar(codigo);
        if (proyecto == null) {
            throw new BusinessException("No existe el proyecto " + codigo);
        }
        verificarAcceso(codigo);
        return proyecto;
    }

    /** sp_tx_inicio_proyecto: alta de proyecto, subrogante y línea en una sola transacción. */
    public void crear(ProyectoForm form) {
        repository.crear(form);
        publisher.publishEvent(EventoDominio.de(EventoDominio.TipoEvento.PROYECTO_CREADO,
                form.codigo(), "Proyecto " + form.acronimo() + " registrado"));
    }

    public void editar(ProyectoForm form) {
        verificarAcceso(form.codigo());
        if (repository.actualizar(form) == 0) {
            throw new BusinessException("No existe el proyecto que desea editar");
        }
    }

    /** sp_tx_cierre_proyecto: cierra proyecto y participaciones con auditoría. */
    public void cerrar(String codigo) {
        verificarAcceso(codigo);
        repository.cerrar(codigo);
        publisher.publishEvent(EventoDominio.de(EventoDominio.TipoEvento.PROYECTO_CERRADO,
                codigo, "Proyecto " + codigo + " cerrado"));
    }

    public void retirarDirector(String codigo) {
        verificarAcceso(codigo);
        repository.retirarDirector(codigo);
    }

    public void cambiarInvestigador(String codigo, int sale, int entra, String rol) {
        verificarAcceso(codigo);
        repository.cambioInvestigador(codigo, sale, entra, rol);
    }

    public void asignarLinea(String codigo, String codLinea) {
        verificarAcceso(codigo);
        repository.agregarLinea(codigo, codLinea);
    }

    public void asignarInvestigador(String codigo, Integer id, String rol) {
        verificarAcceso(codigo);
        repository.agregarInvestigador(codigo, id, rol == null ? "INVESTIGADOR" : rol);
    }

    public void finalizarInvestigador(String codigo, Integer id) {
        verificarAcceso(codigo);
        repository.finalizarInvestigador(codigo, id);
    }

    public void asignarEstudiante(String codigo, Integer id, String fechaInicio) {
        verificarAcceso(codigo);
        repository.agregarEstudiante(codigo, id, fechaInicio);
    }

    public void finalizarEstudiante(String codigo, Integer id) {
        verificarAcceso(codigo);
        repository.finalizarEstudiante(codigo, id);
    }

    public ProyectoDetalle detalle(String codigo) {
        Proyecto proyecto = buscar(codigo);
        return new ProyectoDetalle(proyecto, repository.lineas(codigo), repository.integrantes(codigo),
                repository.estudiantes(codigo), repository.publicaciones(codigo), repository.historial(codigo));
    }

    public List<Map<String, Object>> historial(String codigo) {
        verificarAcceso(codigo);
        return repository.historial(codigo);
    }

    public List<Map<String, Object>> directores() {
        return repository.directores();
    }

    private void verificarAcceso(String codigo) {
        if ((SecurityUtils.esProfesor() || SecurityUtils.esEstudiante())
                && !repository.puedeVer(codigo, SecurityUtils.usuarioActual())) {
            throw new BusinessException("No tiene permiso para acceder a este proyecto");
        }
    }
}
