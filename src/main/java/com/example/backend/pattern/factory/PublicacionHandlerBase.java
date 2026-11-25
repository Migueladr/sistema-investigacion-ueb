package com.example.backend.pattern.factory;

import com.example.backend.dto.DetalleCampo;
import com.example.backend.dto.PublicacionForm;
import com.example.backend.entity.PublicacionItem;
import com.example.backend.repository.PublicacionRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Implementación común: inserta publicación + autor en una transacción y deja el detalle a cada tipo. */
public abstract class PublicacionHandlerBase implements PublicacionHandler {

    protected final PublicacionRepository repository;

    protected PublicacionHandlerBase(PublicacionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void registrar(PublicacionForm formulario) {
        validar(formulario);
        int codigo = repository.insertarBase(formulario);
        repository.insertarDetalle(tipo(), codigo, formulario);
        repository.insertarAutor(codigo, formulario);
    }

    /** Cada tipo puede añadir reglas de formulario sin tocar el orquestador. */
    protected void validar(PublicacionForm formulario) {
        // Las validaciones de integridad (tipo, puntos, periodos) son de la base de datos.
    }

    @Override
    public List<DetalleCampo> detalle(PublicacionItem publicacion) {
        Map<String, Object> datos = repository.detalleEspecifico(publicacion.codigo(), tipo());
        List<DetalleCampo> campos = new ArrayList<>();
        etiquetas().forEach((clave, etiqueta) -> campos.add(
                new DetalleCampo(etiqueta, String.valueOf(datos.getOrDefault(clave, "—")))));
        return campos;
    }

    protected abstract Map<String, String> etiquetas();

    protected static Map<String, String> campos(String... pares) {
        Map<String, String> mapa = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) {
            mapa.put(pares[i], pares[i + 1]);
        }
        return mapa;
    }
}
