package com.example.backend.pattern.observer;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Observer + Singleton de Spring: un único bean mantiene el búfer de notificaciones internas.
 * Los eventos de proyecto y publicación se muestran en el panel sin consultar datos ficticios.
 */
@Service
public class NotificacionService {

    private final ConcurrentLinkedQueue<EventoDominio> eventos = new ConcurrentLinkedQueue<>();

    @EventListener
    public void registrar(EventoDominio evento) {
        eventos.offer(evento);
        while (eventos.size() > 30) {
            eventos.poll();
        }
    }

    public List<EventoDominio> ultimas(int cantidad) {
        return eventos.stream().skip(Math.max(0, eventos.size() - cantidad))
                .sorted((a, b) -> b.fecha().compareTo(a.fecha())).toList();
    }
}
