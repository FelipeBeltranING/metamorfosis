package com.metamorfosis.backend.service;

import com.metamorfosis.backend.auditoria.Auditar;
import com.metamorfosis.backend.dto.CambiarContrasenaRequest;
import com.metamorfosis.backend.dto.ContrasenaCambiadaResponse;
import com.metamorfosis.backend.exception.CuentaBloqueadaException;
import com.metamorfosis.backend.exception.UsuarioNoEncontradoException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.AccionLog;
import com.metamorfosis.backend.model.EntidadLog;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * HU32: cambiar la contraseña desde la cuenta. Pide la contraseña actual; si está mal, cuenta como
 * un intento fallido del mismo bloqueo del inicio de sesión (5 fallos → 5 minutos), para que una
 * sesión robada no sirva para adivinarla. Al cambiarla, las demás sesiones se cierran y esta recibe
 * un token nuevo.
 */
@Service
public class ContrasenaService {

    static final String MENSAJE_FALTA_ACTUAL = "Escribe tu contraseña actual";
    static final String MENSAJE_FALTA_NUEVA = "Escribe la contraseña nueva";
    static final String MENSAJE_NO_COINCIDEN = "Las contraseñas nuevas no coinciden";
    static final String MENSAJE_ACTUAL_INCORRECTA = "La contraseña actual no es correcta";
    static final String MENSAJE_IGUAL_A_LA_ACTUAL = "La contraseña nueva debe ser distinta de la actual";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final BloqueoLoginService bloqueoLoginService;
    private final JwtService jwtService;
    private final Clock reloj;

    @Autowired
    public ContrasenaService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                             BloqueoLoginService bloqueoLoginService, JwtService jwtService) {
        this(usuarioRepository, passwordEncoder, bloqueoLoginService, jwtService, Clock.systemUTC());
    }

    /** Constructor para pruebas: permite fijar la hora. */
    ContrasenaService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                      BloqueoLoginService bloqueoLoginService, JwtService jwtService, Clock reloj) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.bloqueoLoginService = bloqueoLoginService;
        this.jwtService = jwtService;
        this.reloj = reloj;
    }

    /**
     * Sin @Transactional a propósito: si la contraseña actual está mal se lanza un error, y el
     * intento fallido que se acaba de contar no debe deshacerse con él.
     */
    @Auditar(accion = AccionLog.CAMBIAR_CONTRASENA, entidad = EntidadLog.USUARIO)
    public ContrasenaCambiadaResponse cambiar(Integer codigoUsuario, CambiarContrasenaRequest pedido) {
        Usuario usuario = usuarioRepository.findById(codigoUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException(codigoUsuario));
        String actual = pedido == null ? null : pedido.actual();
        String nueva = pedido == null ? null : pedido.nueva();
        String confirmacion = pedido == null ? null : pedido.confirmacion();
        validar(actual, nueva, confirmacion);

        // La misma llave que usa el inicio de sesión para contar los fallos de ese correo.
        String email = usuario.getEmail().strip().toLowerCase(Locale.ROOT);
        long segundosBloqueado = bloqueoLoginService.segundosBloqueado(email);
        if (segundosBloqueado > 0) {
            throw new CuentaBloqueadaException(segundosBloqueado);
        }
        if (!passwordEncoder.matches(actual, usuario.getPasswordHash())) {
            bloqueoLoginService.registrarFallo(email);
            throw new ValidacionException(List.of(MENSAJE_ACTUAL_INCORRECTA));
        }
        if (passwordEncoder.matches(nueva, usuario.getPasswordHash())) {
            throw new ValidacionException(List.of(MENSAJE_IGUAL_A_LA_ACTUAL));
        }
        bloqueoLoginService.limpiar(email);

        // En segundos enteros, como la fecha de emisión del JWT: el token nuevo no queda "antes" del cambio.
        LocalDateTime ahora = LocalDateTime.now(reloj).truncatedTo(ChronoUnit.SECONDS);
        usuario.cambiarContrasena(passwordEncoder.encode(nueva), ahora);
        Usuario guardado = usuarioRepository.save(usuario);
        return new ContrasenaCambiadaResponse(jwtService.generarToken(guardado));
    }

    private static void validar(String actual, String nueva, String confirmacion) {
        List<String> errores = new ArrayList<>();
        if (actual == null || actual.isEmpty()) {
            errores.add(MENSAJE_FALTA_ACTUAL);
        }
        if (nueva == null || nueva.isEmpty()) {
            errores.add(MENSAJE_FALTA_NUEVA);
        } else {
            if (!ReglaContrasena.cumple(nueva)) errores.add(ReglaContrasena.MENSAJE);
            if (!nueva.equals(confirmacion)) errores.add(MENSAJE_NO_COINCIDEN);
        }
        if (!errores.isEmpty()) throw new ValidacionException(errores);
    }
}
