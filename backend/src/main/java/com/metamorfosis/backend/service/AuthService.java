package com.metamorfosis.backend.service;

import com.metamorfosis.backend.dto.LoginRequest;
import com.metamorfosis.backend.dto.LoginResponse;
import com.metamorfosis.backend.dto.RegistroRequest;
import com.metamorfosis.backend.dto.UsuarioResponse;
import com.metamorfosis.backend.exception.CredencialesInvalidasException;
import com.metamorfosis.backend.exception.CuentaBloqueadaException;
import com.metamorfosis.backend.exception.EmailYaRegistradoException;
import com.metamorfosis.backend.exception.TokenInvalidoException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.AccionLog;
import com.metamorfosis.backend.model.EntidadLog;
import com.metamorfosis.backend.model.ResultadoLog;
import com.metamorfosis.backend.model.TokenRevocado;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.TokenRevocadoRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenRevocadoRepository tokenRevocadoRepository;
    private final LogService logService;
    private final BloqueoLoginService bloqueoLoginService;
    // Hash real calculado al arrancar; solo sirve para igualar tiempos de respuesta en el login.
    private final String hashRelleno;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final String MENSAJE_CREDENCIALES = "Email o contraseña incorrectos";
    private static final String MENSAJE_SESION_CERRADA = "La sesión ya fue cerrada";
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$");

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, TokenRevocadoRepository tokenRevocadoRepository,
                       LogService logService, BloqueoLoginService bloqueoLoginService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenRevocadoRepository = tokenRevocadoRepository;
        this.logService = logService;
        this.bloqueoLoginService = bloqueoLoginService;
        this.hashRelleno = passwordEncoder.encode("relleno-para-igualar-tiempos");
    }

    @Transactional
    public UsuarioResponse registrar(RegistroRequest solicitud) {
        // El email se guarda sin espacios y en minúsculas para que "Ana@x.com" y "ana@x.com" sean el mismo
        solicitud = new RegistroRequest(solicitud.nombre(), solicitud.apellido(),
                normalizarEmail(solicitud.email()), solicitud.password());

        // 1. Validaciones de formato y campos obligatorios
        validar(solicitud);

        if (usuarioRepository.existsByEmailIgnoreCase(solicitud.email())) {
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

    
    public LoginResponse iniciarSesion(LoginRequest solicitud) {
        solicitud = new LoginRequest(normalizarEmail(solicitud.email()), solicitud.password());
        validar(solicitud);

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(solicitud.email()).orElse(null);

        // Si el email está bloqueado se rechaza sin verificar la contraseña:
        // durante el bloqueo ni la contraseña correcta permite entrar.
        long segundosBloqueado = bloqueoLoginService.segundosBloqueado(solicitud.email());
        if (segundosBloqueado > 0) {
            registrarFallo(AccionLog.INICIAR_SESION,
                    usuario == null ? null : usuario.getCodigoUsuario(), "CUENTA_BLOQUEADA");
            throw new CuentaBloqueadaException(segundosBloqueado);
        }

        if (usuario == null) {
            // Se compara contra un hash de relleno para que la respuesta tarde lo mismo
            // exista o no el email (evita enumerar usuarios midiendo tiempos).
            passwordEncoder.matches(solicitud.password(), hashRelleno);
            bloqueoLoginService.registrarFallo(solicitud.email());
            registrarFallo(AccionLog.INICIAR_SESION, null, "USUARIO_INEXISTENTE");
            throw new CredencialesInvalidasException(MENSAJE_CREDENCIALES);
        }

        if (!passwordEncoder.matches(solicitud.password(), usuario.getPasswordHash())) {
            bloqueoLoginService.registrarFallo(solicitud.email());
            registrarFallo(AccionLog.INICIAR_SESION, usuario.getCodigoUsuario(), "PASSWORD_INCORRECTO");
            throw new CredencialesInvalidasException(MENSAJE_CREDENCIALES);
        }

        // Login correcto: se reinicia el contador de fallos de ese email.
        bloqueoLoginService.limpiar(solicitud.email());

        String token = jwtService.generarToken(usuario);
        LoginResponse respuesta = new LoginResponse(
            token,
            usuario.getCodigoUsuario(),
            usuario.getNombre(),
            usuario.getApellido(),
            usuario.getEmail()
        );

        logService.registrar(
            usuario.getCodigoUsuario(),
            AccionLog.INICIAR_SESION,
            EntidadLog.USUARIO,
            usuario.getCodigoUsuario(),
            ResultadoLog.EXITO,
            null
        );

        return respuesta;
    }

    @Transactional
    public void cerrarSesion(String token) {
        String jti;
        LocalDateTime fechaExpiracion;
        Integer codigoUsuario;
        try {
            jti = jwtService.extraerId(token);
            fechaExpiracion = jwtService.extraerExpiracion(token);
            codigoUsuario = jwtService.extraerCodigoUsuario(token);
        } catch (TokenInvalidoException e) {
            registrarFallo(AccionLog.CERRAR_SESION, null, "TOKEN_INVALIDO");
            throw e;
        }

        if (tokenRevocadoRepository.existsByJti(jti)) {
            registrarFallo(AccionLog.CERRAR_SESION, codigoUsuario, "TOKEN_YA_REVOCADO");
            throw new TokenInvalidoException(MENSAJE_SESION_CERRADA);
        }

        try {
            tokenRevocadoRepository.save(new TokenRevocado(jti, fechaExpiracion));
        } catch (DataIntegrityViolationException e) {
            // Dos cierres simultáneos del mismo token: gana el primero.
            registrarFallo(AccionLog.CERRAR_SESION, codigoUsuario, "TOKEN_YA_REVOCADO");
            throw new TokenInvalidoException(MENSAJE_SESION_CERRADA);
        }

        logService.registrar(
            codigoUsuario,
            AccionLog.CERRAR_SESION,
            EntidadLog.USUARIO,
            codigoUsuario,
            ResultadoLog.EXITO,
            null
        );
    }

    /** Quita espacios y pasa a minúsculas; deja pasar null para que la validación lo reporte. */
    private String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private void registrarFallo(AccionLog accion, Integer codigoUsuario, String motivo) {
        logService.registrar(
            codigoUsuario,
            accion,
            EntidadLog.USUARIO,
            codigoUsuario,
            ResultadoLog.ERROR,
            "{\"motivo\":\"" + motivo + "\"}"
        );
    }

    private void validar(LoginRequest solicitud) {
        List<String> errores = new ArrayList<>();

        if (solicitud.email() == null || solicitud.email().isBlank()) {
            errores.add("El email es obligatorio");
        } else if (!EMAIL_PATTERN.matcher(solicitud.email()).matches()) {
            errores.add("El formato del email no es válido");
        }

        if (solicitud.password() == null || solicitud.password().isBlank()) {
            errores.add("La contraseña es obligatoria");
        }

        if (!errores.isEmpty()) {
            throw new ValidacionException(errores);
        }
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