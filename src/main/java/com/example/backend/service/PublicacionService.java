package com.example.backend.service;

import com.example.backend.dto.DetalleCampo;
import com.example.backend.dto.PublicacionForm;
import com.example.backend.entity.PublicacionItem;
import com.example.backend.exception.BusinessException;
import com.example.backend.pattern.factory.PublicacionHandler;
import com.example.backend.pattern.factory.PublicacionHandlerFactory;
import com.example.backend.pattern.observer.EventoDominio;
import com.example.backend.repository.PublicacionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** Orquesta publicaciones mediante Factory Method/Strategy según el tipo. */
@Service
public class PublicacionService {

    private final PublicacionRepository repository;
    private final PublicacionHandlerFactory factory;
    private final ApplicationEventPublisher publisher;

    public PublicacionService(PublicacionRepository repository, PublicacionHandlerFactory factory,
                              ApplicationEventPublisher publisher) {
        this.repository = repository;
        this.factory = factory;
        this.publisher = publisher;
    }

    public List<PublicacionItem> listar(String q, String proyecto, Integer autor, String tipo, int page, int size) {
        return repository.listar(q, proyecto, autor, tipo, page, size);
    }

    public long contar(String q, String proyecto, Integer autor, String tipo) {
        return repository.contar(q, proyecto, autor, tipo);
    }

    /** La revista usa sp_tx_registrar_revista; los demás tipos usan transacción local y triggers. */
    public void registrar(PublicacionForm form) {
        PublicacionHandler handler = factory.obtener(form.tipo());
        handler.registrar(form);
        publisher.publishEvent(EventoDominio.de(EventoDominio.TipoEvento.PUBLICACION_REGISTRADA,
                form.tipo(), "Publicación registrada: " + form.titulo()));
    }

    public Map<String, Object> detalle(int codigo) {
        PublicacionItem item = repository.buscar(codigo);
        if (item == null) {
            throw new BusinessException("No existe la publicación indicada");
        }
        PublicacionHandler handler = factory.obtener(item.tipo());
        List<DetalleCampo> campos = handler.detalle(item);
        return Map.of("publicacion", item, "campos", campos,
                "autores", repository.autores(codigo));
    }

    /** Nunca cambia el tipo ni los puntos: los trigger lo impiden y son responsables de ambos. */
    public void editar(int codigo, String titulo, String nombre) {
        if (repository.actualizarDatosBasicos(codigo, titulo, nombre) == 0) {
            throw new BusinessException("No existe la publicación indicada");
        }
    }
}
