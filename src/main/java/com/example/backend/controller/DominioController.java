package com.example.backend.controller;

import com.example.backend.service.DominioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/dominios")
public class DominioController {

    private final DominioService service;

    public DominioController(DominioService service) {
        this.service = service;
    }

    @GetMapping
    public String lista(@RequestParam(required = false) String codigo, Model model) {
        model.addAttribute("vista", service.vistaDominios(codigo));
        model.addAttribute("seleccionado", codigo);
        return "dominios/lista";
    }

    @GetMapping("/{codigo}")
    public String detalle(@PathVariable String codigo, Model model) {
        model.addAttribute("vista", service.vistaDominios(codigo));
        model.addAttribute("seleccionado", codigo);
        return "dominios/lista";
    }
}
