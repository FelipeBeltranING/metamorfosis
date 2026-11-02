package com.metamorfosis.backend.service;

import com.metamorfosis.backend.dto.SolicitudRegistroRequest;
import com.metamorfosis.backend.dto.SolicitudRegistroResponse;
import com.metamorfosis.backend.exception.EmailYaRegistradoException;
import com.metamorfosis.backend.exception.SolicitudBloqueadaException;
import com.metamorfosis.backend.exception.SolicitudInvalidaException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.AccionLog;
import com.metamorfosis.backend.model.EntidadLog;
import com.metamorfosis.backend.model.ResultadoLog;
import com.metamorfosis.backend.model.SolicitudRegistro;
import com.metamorfosis.backend.repository.SolicitudRegistroRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import com.metamorfosis.backend.service.correo.CorreoService;
import com.metamorfosis.backend.service.correo.PlantillaAutorizacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * HU8: solo se crea una cuenta si el dueño del correo la autoriza.
 *   1. solicitar: guarda el email con un token y envía el correo con "Autorizar" y "No fui yo".
 *   2. consultar: la página del enlace comprueba el token (no cambia nada, así que los
 *      antivirus o Gmail pueden abrir el enlace sin efectos).
 *   3. AuthService.registrar valida el token con validarParaRegistro y lo consume al crear la cuenta.
 *   4. rechazar: "No fui yo" invalida el enlace y bloquea el email por 24 h.
 */
@Service
public class SolicitudRegistroService {

    static final Duration ESPERA_REENVIO = Duration.ofMinutes(1);
    static final Duration BLOQUEO_RECHAZO = Duration.ofHours(24);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final String MENSAJE_ENLACE_INVALIDO = "Este enlace ya no es válido. Pide uno nuevo para registrarte.";

    private final SolicitudRegistroRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;
    private final CorreoService correoService;
    private final LogService logService;
    private final String urlFrontend;
    private final int minutosVigencia;
    private final Clock reloj;
    private final SecureRandom aleatorio = new SecureRandom();

    @Autowired
    public SolicitudRegistroService(SolicitudRegistroRepository solicitudRepository,
                                    UsuarioRepository usuarioRepository,
                                    CorreoService correoService,
                                    LogService logService,
                                    @Value("${app.frontend.url}") String urlFrontend,
                                    @Value("${app.registro.minutos-vigencia:30}") int minutosVigencia) {
        this(solicitudRepository, usuarioRepository, correoService, logService, urlFrontend,
                minutosVigencia, Clock.systemUTC());
    }

    SolicitudRegistroService(SolicitudRegistroRepository solicitudRepository,
                             UsuarioRepository usuarioRepository,
                             CorreoService correoService,
                             LogService logService,
                             String urlFrontend,
                             int minutosVigencia,
                             Clock reloj) {
        this.solicitudRepository = solicitudRepository;
        this.usuarioRepository = usuarioRepository;
        this.correoService = correoService;
        this.logService = logService;
        this.urlFrontend = urlFrontend.endsWith("/") ? urlFrontend.substring(0, urlFrontend.length() - 1) : urlFrontend;
        this.minutosVigencia = minutosVigencia;
        this.reloj = reloj;
    }

    /** Paso 1: registra la solicitud y envía el correo de autorización. */
    @Transactional
    public SolicitudRegistroResponse solicitar(SolicitudRegistroRequest pedido) {
        String email = normalizarEmail(pedido == null ? null : pedido.email());
        validarEmail(email);

        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailYaRegistradoException("El email ya está registrado");
        }

        LocalDateTime ahora = LocalDateTime.now(reloj);
        SolicitudRegistro solicitud = solicitudRepository.findByEmail(email)
                .orElseGet(() -> new SolicitudRegistro(email));

        if (solicitud.estaBloqueada(ahora)) {
            throw new SolicitudBloqueadaException(
                    "El dueño de este correo rechazó el registro. Intenta de nuevo más tarde.");
        }
        if (solicitud.getFechaUltimoEnvio() != null
                && solicitud.getFechaUltimoEnvio().plus(ESPERA_REENVIO).isAfter(ahora)) {
            throw new SolicitudBloqueadaException("Ya te enviamos un correo. Espera un minuto para pedir otro.");
        }

        String token = generarToken();
        solicitud.renovar(hash(token), ahora, ahora.plusMinutes(minutosVigencia));
        solicitudRepository.save(solicitud);

        String urlAutorizar = urlFrontend + "/registro/completar?token=" + token;
        String urlRechazar = urlFrontend + "/registro/rechazar?token=" + token;
        correoService.enviar(email, PlantillaAutorizacion.ASUNTO,
                PlantillaAutorizacion.html(email, urlAutorizar, urlRechazar, minutosVigencia),
                PlantillaAutorizacion.texto(email, urlAutorizar, urlRechazar, minutosVigencia));

        logService.registrar(null, AccionLog.SOLICITAR_REGISTRO, EntidadLog.USUARIO, null, ResultadoLog.EXITO, null);
        return new SolicitudRegistroResponse(email, solicitud.getFechaExpiracion());
    }

    /** Paso 2: la página del enlace comprueba el token y obtiene el email autorizado. Solo lee. */
    @Transactional(readOnly = true)
    public SolicitudRegistroResponse consultar(String token) {
        SolicitudRegistro solicitud = validarParaRegistro(token);
        return new SolicitudRegistroResponse(solicitud.getEmail(), solicitud.getFechaExpiracion());
    }

    /** La solicitud vigente de ese token, o SolicitudInvalidaException (410). La usa AuthService.registrar. */
    @Transactional(readOnly = true)
    public SolicitudRegistro validarParaRegistro(String token) {
        if (token == null || token.isBlank()) {
            throw new SolicitudInvalidaException(MENSAJE_ENLACE_INVALIDO);
        }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return solicitudRepository.findByTokenHash(hash(token.trim()))
                .filter(s -> s.estaVigente(ahora))
                .orElseThrow(() -> new SolicitudInvalidaException(MENSAJE_ENLACE_INVALIDO));
    }

    /** Paso 3: la cuenta ya se creó, el enlace no se puede volver a usar. */
    @Transactional
    public void consumir(SolicitudRegistro solicitud) {
        solicitudRepository.delete(solicitud);
    }

    /** "No fui yo": el enlace deja de servir y el email no recibe más correos por 24 h. */
    @Transactional
    public void rechazar(String token) {
        SolicitudRegistro solicitud = validarParaRegistro(token);
        solicitud.bloquear(LocalDateTime.now(reloj).plus(BLOQUEO_RECHAZO));
        solicitudRepository.save(solicitud);
        logService.registrar(null, AccionLog.RECHAZAR_REGISTRO, EntidadLog.USUARIO, null, ResultadoLog.EXITO, null);
    }

    /** Borra solicitudes vencidas y bloqueos terminados. La llama LimpiezaSolicitudesService. */
    @Transactional
    public int limpiarVencidas() {
        return solicitudRepository.borrarVencidas(LocalDateTime.now(reloj));
    }

    /** 32 bytes aleatorios en Base64 URL (43 caracteres): imposible de adivinar. */
    private String generarToken() {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** En la base solo se guarda el SHA-256 del token: quien lea la tabla no puede usar los enlaces. */
    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private void validarEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ValidacionException(List.of("El email es obligatorio"));
        }
        if (email.length() > 255 || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidacionException(List.of("El formato del email no es válido"));
        }
    }
}
