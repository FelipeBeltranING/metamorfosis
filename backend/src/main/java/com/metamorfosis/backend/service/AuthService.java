package com.metamorfosis.backend.service;

import com.metamorfosis.backend.dto.RegistroRequest;
import com.metamorfosis.backend.dto.UsuarioResponse;
import com.metamorfosis.backend.exception.EmailYaRegistradoException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.AccionLog;
import com.metamorfosis.backend.model.EntidadLog;
import com.metamorfosis.backend.model.ResultadoLog;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final LogService logService;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$");

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, LogService logService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.logService = logService;
    }

    @Transactional
    public UsuarioResponse registrar(RegistroRequest solicitud) {
        // 1. Validaciones de formato y campos obligatorios
        validar(solicitud);

        if (usuarioRepository.existsByEmail(solicitud.email())) {
            throw new EmailYaRegistradoException("El email ya está registrado");
        }

        String passwordHash = passwordEncoder.encode(solicitud.password());
        Usuario nuevoUsuario = new Usuario(solicitud.nombre(), solicitud.apellido(), solicitud.email(), passwordHash);
        
        Usuario guardado = usuarioRepository.save(nuevoUsuario);
        UsuarioResponse respuesta = UsuarioResponse.desde(guardado);

        logService.registrar(
            guardado.getCodigoUsuario(),
            AccionLog.REGISTRAR_USUARIO,
            EntidadLog.USUARIO,
            guardado.getCodigoUsuario(),
            ResultadoLog.EXITO,
            null
        );

        return respuesta;
    }

    private void validar(RegistroRequest solicitud) {
        List<String> errores = new ArrayList<>();

        if (solicitud.nombre() == null || solicitud.nombre().isBlank()) {
            errores.add("El nombre es obligatorio");
        }
        
        if (solicitud.apellido() == null || solicitud.apellido().isBlank()) {
            errores.add("El apellido es obligatorio");
        }
        
        if (solicitud.email() == null || solicitud.email().isBlank()) {
            errores.add("El email es obligatorio");
        } else if (!EMAIL_PATTERN.matcher(solicitud.email()).matches()) {
            errores.add("El formato del email no es válido");
        }
        
        if (solicitud.password() == null || solicitud.password().isBlank()) {
            errores.add("La contraseña es obligatoria");
        } else if (!PASSWORD_PATTERN.matcher(solicitud.password()).matches()) {
            errores.add("La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula y un número");
        }

        if (!errores.isEmpty()) {
            throw new ValidacionException(errores);
        }
    }
}