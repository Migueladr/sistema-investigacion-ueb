package com.example.backend.controller;

import com.example.backend.dto.UsuarioForm;
import com.example.backend.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public String lista(Model model) {
        model.addAttribute("usuarios", service.listar());
        model.addAttribute("roles", service.roles());
        model.addAttribute("form", new UsuarioForm(null, null, null, null, java.util.List.of(), true));
        return "usuarios/lista";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("form") UsuarioForm form, BindingResult errores,
                        Model model, RedirectAttributes flash) {
        if (errores.hasErrors()) {
            model.addAttribute("usuarios", service.listar());
            model.addAttribute("roles", service.roles());
            return "usuarios/lista";
        }
        service.guardar(form);
        flash.addFlashAttribute("exito", "Usuario creado con contraseña cifrada con BCrypt.");
        return "redirect:/usuarios";
    }

    @PostMapping("/{username}/editar")
    public String editar(@PathVariable String username, @Valid @ModelAttribute("form") UsuarioForm form,
                         BindingResult errores, RedirectAttributes flash) {
        UsuarioForm corregido = new UsuarioForm(username, form.password(), form.idProfesor(),
                form.idEstudiante(), form.roles(), form.activo());
        if (errores.hasErrors()) {
            flash.addFlashAttribute("error", "Revise los datos del usuario.");
            return "redirect:/usuarios";
        }
        service.editar(corregido);
        flash.addFlashAttribute("exito", "Usuario actualizado.");
        return "redirect:/usuarios";
    }

    @PostMapping("/cambiar-clave")
    public String clave(@RequestParam String actual, @RequestParam String nueva,
                        @RequestParam String confirmacion, RedirectAttributes flash) {
        if (!nueva.equals(confirmacion)) {
            flash.addFlashAttribute("error", "Las contraseñas nuevas no coinciden.");
            return "redirect:/usuarios";
        }
        service.cambiarContrasena(com.example.backend.security.SecurityUtils.usuarioActual(), actual, nueva);
        flash.addFlashAttribute("exito", "Contraseña actualizada.");
        return "redirect:/usuarios";
    }
}
