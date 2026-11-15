package com.metamorfosis.backend.service;

import com.metamorfosis.backend.auditoria.Auditar;
import com.metamorfosis.backend.dto.CambiarContrasenaRequest;
import com.metamorfosis.backend.dto.ContrasenaCambiadaResponse;
import com.metamorfosis.backend.service.correo.CorreoService;
import com.metamorfosis.backend.service.correo.PlantillaContrasenaCambiada;
import com.metamorfosis.backend.exception.UsuarioNoEncontradoException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.AccionLog;
import com.metamorfosis.backend.model.EntidadLog;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * HU32: cambiar la contraseña desde la cuenta. Como ya hay una sesión iniciada, solo se pide la
 * nueva y su confirmación. Al cambiarla, las demás sesiones se cierran, esta recibe un token nuevo y
 * llega un correo de aviso (por si no fue la persona dueña de la cuenta).
 */
@Service
public class ContrasenaService {

    static final String MENSAJE_FALTA_NUEVA = "Escribe la contraseña nueva";
    static final String MENSAJE_NO_COINCIDEN = "Las contraseñas nuevas no coinciden";
    static final String MENSAJE_IGUAL_A_LA_ACTUAL = "La contraseña nueva debe ser distinta de la actual";

    private static final Logger log = LoggerFactory.getLogger(ContrasenaService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final BloqueoLoginService bloqueoLoginService;
    private final JwtService jwtService;
    private final CorreoService correoService;
    private final String urlFrontend;
    private final Clock reloj;

    @Autowired
    public ContrasenaService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                             BloqueoLoginService bloqueoLoginService, JwtService jwtService,
                             CorreoService correoService, @Value("${app.frontend.url}") String urlFrontend) {
        this(usuarioRepository, passwordEncoder, bloqueoLoginService, jwtService, correoService, urlFrontend,
                Clock.systemUTC());
    }

    /** Constructor para pruebas: permite fijar la hora. */
    ContrasenaService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                      BloqueoLoginService bloqueoLoginService, JwtService jwtService,
                      CorreoService correoService, String urlFrontend, Clock reloj) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.bloqueoLoginService = bloqueoLoginService;
        this.jwtService = jwtService;
        this.correoService = correoService;
        this.urlFrontend = urlFrontend;
        this.reloj = reloj;
    }

    @Auditar(accion = AccionLog.CAMBIAR_CONTRASENA, entidad = EntidadLog.USUARIO)
    @Transactional
    public ContrasenaCambiadaResponse cambiar(Integer codigoUsuario, CambiarContrasenaRequest pedido) {
        Usuario usuario = usuarioRepository.findById(codigoUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException(codigoUsuario));
        List<String> errores = new ArrayList<>();
        validarNueva(pedido == null ? null : pedido.nueva(), pedido == null ? null : pedido.confirmacion(), errores);
        if (!errores.isEmpty()) throw new ValidacionException(errores);

        Usuario guardado = ponerNueva(usuario, pedido.nueva());
        return new ContrasenaCambiadaResponse(jwtService.generarToken(guardado));
    }

    /** Revisa la contraseña nueva y su confirmación; anota los errores en la lista. */
    static void validarNueva(String nueva, String confirmacion, List<String> errores) {
        if (nueva == null || nueva.isEmpty()) {
            errores.add(MENSAJE_FALTA_NUEVA);
            return;
        }
        if (!ReglaContrasena.cumple(nueva)) errores.add(ReglaContrasena.MENSAJE);
        if (!nueva.equals(confirmacion)) errores.add(MENSAJE_NO_COINCIDEN);
    }

    /**
     * Pone la contraseña nueva (ya validada): debe ser distinta de la actual. Anota la hora del cambio
     * (las sesiones anteriores dejan de servir), borra los intentos fallidos del inicio de sesión y
     * manda el correo de aviso. Público porque lo llama otro servicio a través del proxy de Spring
     * (este servicio tiene @Auditar): un método de paquete se ejecutaría sobre el proxy, sin campos.
     */
    public Usuario ponerNueva(Usuario usuario, String nueva) {
        if (passwordEncoder.matches(nueva, usuario.getPasswordHash())) {
            throw new ValidacionException(List.of(MENSAJE_IGUAL_A_LA_ACTUAL));
        }
        // En segundos enteros, como la fecha de emisión del JWT: el token nuevo no queda "antes" del cambio.
        LocalDateTime ahora = LocalDateTime.now(reloj).truncatedTo(ChronoUnit.SECONDS);
        usuario.cambiarContrasena(passwordEncoder.encode(nueva), ahora);
        Usuario guardado = usuarioRepository.save(usuario);
        bloqueoLoginService.limpiar(usuario.getEmail().strip().toLowerCase(Locale.ROOT));
        avisarPorCorreo(guardado);
        return guardado;
    }

    /** El aviso es un extra: si el correo falla, la contraseña ya quedó cambiada y no se deshace. */
    private void avisarPorCorreo(Usuario usuario) {
        Locale idioma = LocaleContextHolder.getLocale();
        String urlEntrar = urlFrontend + "/login";
        try {
            correoService.enviar(usuario.getEmail(), PlantillaContrasenaCambiada.asunto(idioma),
                    PlantillaContrasenaCambiada.html(usuario.getNombre(), usuario.getEmail(), urlEntrar, idioma),
                    PlantillaContrasenaCambiada.texto(usuario.getNombre(), usuario.getEmail(), urlEntrar, idioma));
        } catch (RuntimeException e) {
            log.warn("No se pudo enviar el aviso de cambio de contraseña al usuario {}", usuario.getCodigoUsuario(), e);
        }
    }
}
