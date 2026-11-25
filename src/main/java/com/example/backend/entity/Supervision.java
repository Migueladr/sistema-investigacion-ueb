package com.example.backend.entity;

import java.time.LocalDate;

public record Supervision(Integer idDoctor, String doctor, Integer idNoDoctor, String noDoctor,
                          LocalDate fechaInicio, LocalDate fechaFin, boolean vigente) {
}
