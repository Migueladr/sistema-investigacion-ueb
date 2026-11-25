package com.example.backend.pattern.factory;

import com.example.backend.dto.DetalleCampo;
import com.example.backend.dto.PublicacionForm;
import com.example.backend.entity.PublicacionItem;

import java.util.List;

/** Strategy por tipo de publicación: cada handler decide cómo registrar y qué detalle mostrar. */
public interface PublicacionHandler {

    String tipo();

    void registrar(PublicacionForm formulario);

    List<DetalleCampo> detalle(PublicacionItem publicacion);
}
