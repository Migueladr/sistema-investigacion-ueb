package com.example.backend.controller;

import com.example.backend.dto.SupervisionForm;
import com.example.backend.service.SupervisionService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/supervisiones")
public class SupervisionController {

    private final SupervisionService service;

    public SupervisionController(SupervisionService service) {
        this.service = service;
    }

    @GetMapping
    public String lista(@RequestParam(required = false) String q,
                        @RequestParam(defaultValue = "true") boolean vigentes, Model model) {
        model.addAttribute("supervisiones", service.listar(vigentes, q));
        model.addAttribute("catalogos", service.catalogos());
        model.addAttribute("historial", service.historial());
        model.addAttribute("vigentes", vigentes);
        model.addAttribute("q", q);
        return "supervisiones/lista";
    }

    @PostMapping
    public String registrar(@Valid @ModelAttribute("form") SupervisionForm form, BindingResult errores,
                            RedirectAttributes flash) {
        if (errores.hasErrors()) {
            flash.addFlashAttribute("error", "Revise los campos de la supervisión.");
            return "redirect:/supervisiones";
        }
        service.registrar(form);
        flash.addFlashAttribute("exito", "Supervisión registrada y validada por sp_validar_supervision.");
        return "redirect:/supervisiones";
    }

    @PostMapping("/finalizar")
    public String finalizar(@RequestParam int doctor, @RequestParam int noDoctor,
                            @RequestParam String fechaInicio, @RequestParam String fechaFin,
                            RedirectAttributes flash) {
        service.finalizar(doctor, noDoctor, fechaInicio, fechaFin);
        flash.addFlashAttribute("exito", "Supervisión finalizada.");
        return "redirect:/supervisiones";
    }
}
