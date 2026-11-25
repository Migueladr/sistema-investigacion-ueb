package com.example.backend.controller;

import com.example.backend.dto.InvestigadorForm;
import com.example.backend.service.InvestigadorService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/investigadores")
public class InvestigadorController {

    private final InvestigadorService service;

    public InvestigadorController(InvestigadorService service) {
        this.service = service;
    }

    @GetMapping
    public String lista(@RequestParam(required = false) String q,
                        @RequestParam(required = false) String tipo,
                        @RequestParam(defaultValue = "1") int page, Model model) {
        model.addAttribute("investigadores", service.listar(q, tipo, page, 10));
        model.addAttribute("total", service.contar(q, tipo));
        model.addAttribute("pagina", page);
        model.addAttribute("q", q);
        model.addAttribute("tipo", tipo);
        return "investigadores/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("form", new InvestigadorForm(null, null, null, null, null, null));
        model.addAttribute("edicion", false);
        return "investigadores/form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("form") InvestigadorForm form, BindingResult errores,
                        Model model, RedirectAttributes flash) {
        if (errores.hasErrors()) {
            model.addAttribute("edicion", false);
            return "investigadores/form";
        }
        service.guardar(form);
        flash.addFlashAttribute("exito", "Investigador registrado; el tipo se refleja en doctor o no_doctor.");
        return "redirect:/investigadores";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable int id, Model model) {
        model.addAttribute("datos", service.detalle(id));
        return "investigadores/detalle";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable int id, Model model) {
        var i = service.buscar(id);
        model.addAttribute("form", new InvestigadorForm(i.id(), i.nombres(), i.apellidos(),
                i.despacho(), i.telefono(), i.tipo()));
        model.addAttribute("edicion", true);
        return "investigadores/form";
    }

    @PostMapping("/{id}/editar")
    public String guardar(@PathVariable int id, @Valid @ModelAttribute("form") InvestigadorForm form,
                          BindingResult errores, Model model, RedirectAttributes flash) {
        InvestigadorForm corregido = new InvestigadorForm(id, form.nombres(), form.apellidos(),
                form.despacho(), form.telefono(), form.tipo());
        if (errores.hasErrors()) {
            model.addAttribute("form", corregido);
            model.addAttribute("edicion", true);
            return "investigadores/form";
        }
        service.editar(corregido);
        flash.addFlashAttribute("exito", "Datos del investigador actualizados.");
        return "redirect:/investigadores/" + id;
    }
}
