package com.example.backend.service;

import com.example.backend.dto.UsuarioForm;
import com.example.backend.exception.BusinessException;
import com.example.backend.repository.UsuarioRepository;
import com.example.backend.security.SecurityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
    }

    public List<Map<String, Object>> listar() {
        return repository.listar();
    }

    public List<Map<String, Object>> roles() {
        return repository.rolesDisponibles();
    }

    @Transactional
    public void guardar(UsuarioForm form) {
        if (repository.existe(form.username())) {
            throw new BusinessException("El usuario ya existe");
        }
        validar(form);
        repository.guardar(form, encoder.encode(form.password()));
    }

    @Transactional
    public void editar(UsuarioForm form) {
        if (!repository.existe(form.username())) {
            throw new BusinessException("El usuario no existe");
        }
        validar(form);
        String hash = form.password() == null || form.password().isBlank()
                ? null : encoder.encode(form.password());
        repository.actualizar(form, hash);
    }

    private void validar(UsuarioForm form) {
        boolean profesor = form.idProfesor() != null;
        boolean estudiante = form.idEstudiante() != null;
        if (profesor && estudiante) {
            throw new BusinessException("Un usuario solo puede asociar un perfil: profesor o estudiante");
        }
        if (form.roles() == null || form.roles().isEmpty()) {
            throw new BusinessException("Asigne al menos un rol");
        }
        boolean requiereProfesor = form.roles().contains("PROFESOR");
        boolean requiereEstudiante = form.roles().contains("ESTUDIANTE");
        if (requiereProfesor != profesor || requiereEstudiante != estudiante) {
            throw new BusinessException("Los roles PROFESOR/ESTUDIANTE requieren su perfil académico asociado; "
                    + "ADMIN y SECRETARIA no lo requieren");
        }
        if ((form.password() == null || form.password().isBlank()) && !repository.existe(form.username())) {
            throw new BusinessException("La contraseña es obligatoria para usuarios nuevos");
        }
    }

    public void cambiarContrasena(String username, String actual, String nueva) {
        Map<String, Object> usuario = repository.buscar(username);
        if (usuario == null) {
            throw new BusinessException("El usuario no existe");
        }
        if (!encoder.matches(actual, String.valueOf(usuario.get("password_hash")))) {
            throw new BusinessException("La contraseña actual no es correcta");
        }
        UsuarioForm formulario = new UsuarioForm(username, null,
                (Integer) usuario.get("id_profesor"), (Integer) usuario.get("id_estudiante"),
                repository.rolesDe(username), (Boolean) usuario.get("activo"));
        repository.actualizar(formulario, encoder.encode(nueva));
    }

    public boolean esAdministradorActual() {
        return SecurityUtils.esAdmin();
    }
}
