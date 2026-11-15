package com.metamorfosis.backend.service;

import com.metamorfosis.backend.dto.RestablecerContrasenaRequest;
import com.metamorfosis.backend.dto.SolicitarCodigoRequest;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.AccionLog;
import com.metamorfosis.backend.model.CodigoRecuperacion;
import com.metamorfosis.backend.model.EntidadLog;
import com.metamorfosis.backend.model.ResultadoLog;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.CodigoRecuperacionRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import com.metamorfosis.backend.service.correo.CorreoService;
import com.metamorfosis.backend.service.correo.PlantillaCodigoRecuperacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * HU33: cambiar la contraseña olvidada sin sesión, desde Iniciar sesión.
 * <ol>
 *   <li>La persona escribe su correo y, si hay una cuenta con ese correo, le llega un código de 6
 *       dígitos que vence en 15 minutos. La respuesta es la misma exista o no la cuenta, para no
 *       revelar qué correos están registrados.</li>
 *   <li>Escribe el código y la contraseña nueva dos veces. Si el código es el correcto, la contraseña
 *       cambia, las sesiones abiertas se cierran y llega el correo de aviso (HU32).</li>
 * </ol>
 * Sin sesión no pasa por @Auditar: las acciones se registran a mano en el log de actividad.
 */
@Service
public class RecuperacionContrasenaService {

    static final int DIGITOS = 6;
    static final Duration VIGENCIA = Duration.ofMinutes(15);
    /** Tiempo mínimo entre dos códigos para el mismo correo (evita llenarle la bandeja a alguien). */
    static final Duration ESPERA_REENVIO = Duration.ofSeconds(60);
    static final int MAX_INTENTOS = 5;

    static final String MENSAJE_FALTA_EMAIL = "El email es obligatorio";
    static final String MENSAJE_EMAIL_INVALIDO = "El formato del email no es válido";
    static final String MENSAJE_FALTA_CODIGO = "Escribe el código de 6 dígitos que te llegó al correo";
    static final String MENSAJE_CODIGO_NO_VALIDO = "El código no es correcto o ya venció. Pide uno nuevo.";
    static final String MENSAJE_DEMASIADOS_INTENTOS = "Escribiste mal el código demasiadas veces. Pide uno nuevo.";

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final CodigoRecuperacionRepository codigoRepository;
    private final PasswordEncoder passwordEncoder;
    private final ContrasenaService contrasenaService;
    private final CorreoService correoService;
    private final LogService logService;
    private final Clock reloj;

    @Autowired
    public RecuperacionContrasenaService(UsuarioRepository usuarioRepository, CodigoRecuperacionRepository codigoRepository,
                                         PasswordEncoder passwordEncoder, ContrasenaService contrasenaService,
                                         CorreoService correoService, LogService logService) {
        this(usuarioRepository, codigoRepository, passwordEncoder, contrasenaService, correoService, logService,
                Clock.systemUTC());
    }

    /** Constructor para pruebas: permite fijar la hora. */
    RecuperacionContrasenaService(UsuarioRepository usuarioRepository, CodigoRecuperacionRepository codigoRepository,
                                  PasswordEncoder passwordEncoder, ContrasenaService contrasenaService,
                                  CorreoService correoService, LogService logService, Clock reloj) {
        this.usuarioRepository = usuarioRepository;
        this.codigoRepository = codigoRepository;
        this.passwordEncoder = passwordEncoder;
        this.contrasenaService = contrasenaService;
        this.correoService = correoService;
        this.logService = logService;
        this.reloj = reloj;
    }

    /** Paso 1: manda el código si el correo tiene cuenta. Sin cuenta no hace nada (y no lo dice). */
    public void solicitarCodigo(SolicitarCodigoRequest pedido) {
        List<String> errores = new ArrayList<>();
        String email = validarEmail(pedido == null ? null : pedido.email(), errores);
        if (!errores.isEmpty()) throw new ValidacionException(errores);

        Optional<Usuario> encontrado = usuarioRepository.findByEmailIgnoreCase(email);
        if (encontrado.isEmpty()) {
            registrar(AccionLog.SOLICITAR_CODIGO_CONTRASENA, null, ResultadoLog.ERROR, "USUARIO_INEXISTENTE");
            return;
        }
        Usuario usuario = encontrado.get();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        CodigoRecuperacion registro = codigoRepository.findById(usuario.getCodigoUsuario())
                .orElseGet(() -> new CodigoRecuperacion(usuario.getCodigoUsuario()));
        if (registro.getEnviadoEn() != null && registro.getEnviadoEn().plus(ESPERA_REENVIO).isAfter(ahora)) {
            // Ya se mandó uno hace menos de un minuto: ese sigue sirviendo.
            registrar(AccionLog.SOLICITAR_CODIGO_CONTRASENA, usuario.getCodigoUsuario(), ResultadoLog.ERROR, "REENVIO_MUY_PRONTO");
            return;
        }

        String codigo = nuevoCodigo();
        registro.renovar(passwordEncoder.encode(codigo), ahora, ahora.plus(VIGENCIA));
        codigoRepository.save(registro);

        Locale idioma = LocaleContextHolder.getLocale();
        int minutos = (int) VIGENCIA.toMinutes();
        correoService.enviar(usuario.getEmail(), PlantillaCodigoRecuperacion.asunto(idioma),
                PlantillaCodigoRecuperacion.html(usuario.getNombre(), codigo, minutos, idioma),
                PlantillaCodigoRecuperacion.texto(usuario.getNombre(), codigo, minutos, idioma));
        registrar(AccionLog.SOLICITAR_CODIGO_CONTRASENA, usuario.getCodigoUsuario(), ResultadoLog.EXITO, null);
    }

    /**
     * Paso 2: con el código correcto, pone la contraseña nueva. Sin @Transactional a propósito: un
     * intento fallido que se cuenta no debe deshacerse con el error que se lanza.
     */
    public void restablecer(RestablecerContrasenaRequest pedido) {
        List<String> errores = new ArrayList<>();
        String email = validarEmail(pedido == null ? null : pedido.email(), errores);
        String codigo = soloDigitos(pedido == null ? null : pedido.codigo());
        if (codigo.length() != DIGITOS) errores.add(MENSAJE_FALTA_CODIGO);
        ContrasenaService.validarNueva(pedido == null ? null : pedido.nueva(),
                pedido == null ? null : pedido.confirmacion(), errores);
        if (!errores.isEmpty()) throw new ValidacionException(errores);

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email).orElse(null);
        CodigoRecuperacion registro = usuario == null ? null
                : codigoRepository.findById(usuario.getCodigoUsuario()).orElse(null);
        Integer codigoUsuario = usuario == null ? null : usuario.getCodigoUsuario();
        if (registro == null || !registro.estaVigente(LocalDateTime.now(reloj))) {
            registrar(AccionLog.RESTABLECER_CONTRASENA, codigoUsuario, ResultadoLog.ERROR, "CODIGO_NO_VIGENTE");
            throw new ValidacionException(List.of(MENSAJE_CODIGO_NO_VALIDO));
        }
        if (!passwordEncoder.matches(codigo, registro.getCodigoHash())) {
            int intentos = registro.sumarIntento();
            if (intentos >= MAX_INTENTOS) {
                codigoRepository.delete(registro);
                registrar(AccionLog.RESTABLECER_CONTRASENA, codigoUsuario, ResultadoLog.ERROR, "DEMASIADOS_INTENTOS");
                throw new ValidacionException(List.of(MENSAJE_DEMASIADOS_INTENTOS));
            }
            codigoRepository.save(registro);
            registrar(AccionLog.RESTABLECER_CONTRASENA, codigoUsuario, ResultadoLog.ERROR, "CODIGO_INCORRECTO");
            throw new ValidacionException(List.of(mensajeCodigoIncorrecto(MAX_INTENTOS - intentos)));
        }

        // Primero la contraseña (puede fallar si es igual a la actual) y después se gasta el código.
        contrasenaService.ponerNueva(usuario, pedido.nueva());
        codigoRepository.delete(registro);
        registrar(AccionLog.RESTABLECER_CONTRASENA, codigoUsuario, ResultadoLog.EXITO, null);
    }

    static String mensajeCodigoIncorrecto(int quedan) {
        return "El código no es correcto. Te quedan " + quedan + " intento(s)";
    }

    /** Código de 6 dígitos con ceros a la izquierda (000000 a 999999). */
    static String nuevoCodigo() {
        return String.format("%0" + DIGITOS + "d", ALEATORIO.nextInt((int) Math.pow(10, DIGITOS)));
    }

    /** Acepta «482 913» o «482-913»: se quedan solo los dígitos. */
    static String soloDigitos(String texto) {
        return texto == null ? "" : texto.replaceAll("\\D", "");
    }

    private static String validarEmail(String email, List<String> errores) {
        String limpio = email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
        if (limpio.isEmpty()) errores.add(MENSAJE_FALTA_EMAIL);
        else if (!EMAIL.matcher(limpio).matches()) errores.add(MENSAJE_EMAIL_INVALIDO);
        return limpio;
    }

    private void registrar(AccionLog accion, Integer codigoUsuario, ResultadoLog resultado, String motivo) {
        logService.registrar(codigoUsuario, accion, EntidadLog.USUARIO, codigoUsuario, resultado,
                motivo == null ? null : "{\"motivo\":\"" + motivo + "\"}");
    }
}
