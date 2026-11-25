package com.example.backend.service;

import com.example.backend.dto.DashboardData;
import com.example.backend.repository.DashboardRepository;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final DashboardRepository repository;

    public DashboardService(DashboardRepository repository) {
        this.repository = repository;
    }

    public DashboardData obtener() {
        return repository.cargar();
    }
}
