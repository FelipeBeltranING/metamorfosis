package com.metamorfosis.backend.service;

import com.metamorfosis.backend.dto.RegistroRequest;
import com.metamorfosis.backend.dto.UsuarioResponse;
import com.metamorfosis.backend.exception.EmailYaRegistradoException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final int LONGITUD_MINIMA_PASSWORD = 8;
    private static final Pattern FORMATO_EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioResponse registrar(RegistroRequest solicitud) {
        validar(solicitud);

        String email = solicitud.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(email)) {
            throw new EmailYaRegistradoException("El email ya está registrado");
        }

        String hash = passwordEncoder.encode(solicitud.password());
        Usuario usuario = new Usuario(
                solicitud.nombre().trim(),
                solicitud.apellido().trim(),
                email,
                hash);


        try {
            Usuario guardado = usuarioRepository.save(usuario);
            return UsuarioResponse.desde(guardado);
        } catch (DataIntegrityViolationException e) {
            throw new EmailYaRegistradoException("El email ya está registrado");
        }
    }

    private void validar(RegistroRequest s) {
        List<String> errores = new ArrayList<>();
        if (s == null) {
            errores.add("La solicitud de registro no puede ser nula");
        } else {
            if (estaVacio(s.nombre())) errores.add("El nombre no puede estar vacío");
            if (estaVacio(s.apellido())) errores.add("El apellido no puede estar vacío");
            if (estaVacio(s.email()) || !FORMATO_EMAIL.matcher(s.email().trim()).matches()) {
                errores.add("El email no es válido, escriba un formato válido");
            }
            if (s.password() == null || s.password().length() < LONGITUD_MINIMA_PASSWORD) {
                errores.add("La contraseña debe tener al menos " + LONGITUD_MINIMA_PASSWORD + " caracteres");
            }
        }
        if (!errores.isEmpty()) throw new ValidacionException(errores);
    }

    private boolean estaVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
