package com.metamorfosis.backend.service;

import com.metamorfosis.backend.auditoria.Auditar;
import com.metamorfosis.backend.auditoria.ContextoAuditoria;
import com.metamorfosis.backend.dto.InvitacionCorreoResponse;
import com.metamorfosis.backend.dto.InvitacionEnviadaResponse;
import com.metamorfosis.backend.dto.InvitacionRespondidaResponse;
import com.metamorfosis.backend.dto.InvitacionResponse;
import com.metamorfosis.backend.dto.InvitarRequest;
import com.metamorfosis.backend.dto.ParticipanteResponse;
import com.metamorfosis.backend.dto.ResponderInvitacionRequest;
import com.metamorfosis.backend.exception.ConflictoException;
import com.metamorfosis.backend.exception.CorreoNoEnviadoException;
import com.metamorfosis.backend.exception.InvitacionNoEncontradaException;
import com.metamorfosis.backend.exception.InvitacionNoValidaException;
import com.metamorfosis.backend.exception.MetaNoEncontradaException;
import com.metamorfosis.backend.exception.PermisoDenegadoException;
import com.metamorfosis.backend.exception.UsuarioNoEncontradoException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.AccionLog;
import com.metamorfosis.backend.model.EntidadLog;
import com.metamorfosis.backend.model.EstadoInvitacion;
import com.metamorfosis.backend.model.EstadoMeta;
import com.metamorfosis.backend.model.Meta;
import com.metamorfosis.backend.model.Participacion;
import com.metamorfosis.backend.model.ParticipacionId;
import com.metamorfosis.backend.model.ResultadoLog;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.MetaRepository;
import com.metamorfosis.backend.repository.ParticipacionRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import com.metamorfosis.backend.service.correo.CorreoService;
import com.metamorfosis.backend.service.correo.PlantillaInvitacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * HU13: invitar personas a una meta. La invitada queda PENDIENTE hasta que responda (HU14),
 * desde la app o desde los botones del correo. Al aceptar, la meta aparece en sus metas: comparte
 * las sub-metas de la meta y registra sus propios días.
 */
@Service
public class InvitacionService {

    private static final Logger log = LoggerFactory.getLogger(InvitacionService.class);

    /** Personas por meta, contando al creador y a las invitaciones pendientes. */
    static final int MAXIMO_PARTICIPANTES = 10;
    /** Días que sirve el enlace del correo. */
    static final int DIAS_VIGENCIA = 7;

    private final MetaRepository metaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ParticipacionRepository participacionRepository;
    private final CorreoService correoService;
    private final LogService logService;
    private final String urlFrontend;
    private final Clock reloj;

    @Autowired
    public InvitacionService(MetaRepository metaRepository, UsuarioRepository usuarioRepository,
                             ParticipacionRepository participacionRepository, CorreoService correoService,
                             LogService logService, @Value("${app.frontend.url}") String urlFrontend) {
        this(metaRepository, usuarioRepository, participacionRepository, correoService, logService, urlFrontend,
                Clock.systemUTC());
    }

    InvitacionService(MetaRepository metaRepository, UsuarioRepository usuarioRepository,
                      ParticipacionRepository participacionRepository, CorreoService correoService,
                      LogService logService, String urlFrontend, Clock reloj) {
        this.metaRepository = metaRepository;
        this.usuarioRepository = usuarioRepository;
        this.participacionRepository = participacionRepository;
        this.correoService = correoService;
        this.logService = logService;
        this.urlFrontend = urlFrontend;
        this.reloj = reloj;
    }

    /**
     * HU13: el creador invita a otra persona a una meta activa. No se invita a uno mismo, a quien
     * ya participa ni a quien ya tiene una invitación pendiente; a quien rechazó se le puede volver
     * a invitar. Se envía el correo con Aceptar y Rechazar; si el correo falla, la invitación queda
     * igual (la persona la ve en sus invitaciones) y la respuesta lo indica.
     */
    @Auditar(accion = AccionLog.INVITAR_META, entidad = EntidadLog.META)
    @Transactional
    public InvitacionEnviadaResponse invitar(Integer codigoMeta, InvitarRequest pedido, Integer codigoUsuario) {
        if (pedido == null || pedido.codigoUsuario() == null) {
            throw new ValidacionException(List.of("Elige a quién quieres invitar"));
        }
        Meta meta = metaRepository.findById(codigoMeta)
                .orElseThrow(() -> new MetaNoEncontradaException(codigoMeta));
        Usuario creador = meta.getCreador();
        if (!creador.getCodigoUsuario().equals(codigoUsuario)) {
            throw new PermisoDenegadoException("Solo el creador puede invitar personas a la meta");
        }
        if (meta.getEstado() != EstadoMeta.ACTIVA) {
            throw new ValidacionException(List.of("Solo se puede invitar a metas activas"));
        }
        if (pedido.codigoUsuario().equals(codigoUsuario)) {
            throw new ValidacionException(List.of("No puedes invitarte a ti mismo"));
        }
        Usuario invitado = usuarioRepository.findById(pedido.codigoUsuario())
                .orElseThrow(() -> new UsuarioNoEncontradoException(pedido.codigoUsuario()));

        String nombreInvitado = invitado.getNombre() + " " + invitado.getApellido();
        Optional<Participacion> previa = meta.participacionDeUsuario(invitado.getCodigoUsuario());
        if (previa.isPresent() && previa.get().getEstadoInvitacion() == EstadoInvitacion.ACEPTADA) {
            throw new ConflictoException(nombreInvitado + " ya participa en esta meta");
        }
        if (previa.isPresent() && previa.get().getEstadoInvitacion() == EstadoInvitacion.PENDIENTE) {
            throw new ConflictoException(nombreInvitado + " ya tiene una invitación pendiente a esta meta");
        }
        if (meta.participantesVisibles().size() >= MAXIMO_PARTICIPANTES) {
            throw new ValidacionException(List.of("La meta ya tiene el máximo de " + MAXIMO_PARTICIPANTES + " personas"));
        }

        String token = TokensSeguros.generar();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Participacion participacion = meta.invitar(invitado, codigoUsuario, TokensSeguros.hash(token),
                ahora, ahora.plusDays(DIAS_VIGENCIA));
        metaRepository.save(meta);
        ContextoAuditoria.agregar("invitado", invitado.getCodigoUsuario());

        boolean correoEnviado = enviarCorreo(invitado, creador, meta, token);
        if (!correoEnviado) ContextoAuditoria.agregar("correoEnviado", false);
        return new InvitacionEnviadaResponse(ParticipanteResponse.desde(participacion, creador), correoEnviado);
    }

    /** HU14: invitaciones pendientes del usuario (solo de metas activas). */
    @Auditar(accion = AccionLog.VER_INVITACIONES, entidad = EntidadLog.META)
    @Transactional(readOnly = true)
    public List<InvitacionResponse> pendientes(Integer codigoUsuario) {
        return participacionRepository.pendientesDe(codigoUsuario).stream().map(InvitacionResponse::desde).toList();
    }

    /** HU14: acepta o rechaza, desde la app, la invitación del usuario a esa meta. */
    @Auditar(accion = AccionLog.RESPONDER_INVITACION, entidad = EntidadLog.META)
    @Transactional
    public InvitacionRespondidaResponse responder(Integer codigoMeta, ResponderInvitacionRequest pedido,
                                                  Integer codigoUsuario) {
        if (pedido == null || pedido.aceptar() == null) {
            throw new ValidacionException(List.of("Indica si aceptas o rechazas la invitación"));
        }
        Participacion p = participacionRepository.findById(new ParticipacionId(codigoUsuario, codigoMeta))
                .filter(x -> x.getEstadoInvitacion() == EstadoInvitacion.PENDIENTE)
                .orElseThrow(InvitacionNoEncontradaException::new);
        return conContexto(aplicarRespuesta(p, pedido.aceptar()), "app");
    }

    /** HU14: datos de la invitación del enlace del correo, para la página que pregunta. Solo lee. */
    @Transactional(readOnly = true)
    public InvitacionCorreoResponse consultarPorToken(String token) {
        Participacion p = porToken(token);
        Meta meta = p.getMeta();
        Usuario creador = meta.getCreador();
        return new InvitacionCorreoResponse(meta.getCodigoMeta(), meta.getNombre(),
                creador.getNombre() + " " + creador.getApellido(), enmascarar(p.getUsuario().getEmail()));
    }

    /**
     * HU14: aceptar desde el correo. Hay que haber iniciado sesión con la cuenta invitada: si el
     * correo se reenvió, otra persona no puede entrar a la meta con ese enlace.
     */
    @Auditar(accion = AccionLog.RESPONDER_INVITACION, entidad = EntidadLog.META)
    @Transactional
    public InvitacionRespondidaResponse aceptarPorToken(String token, Integer codigoUsuario) {
        Participacion p = porToken(token);
        if (!p.getUsuario().getCodigoUsuario().equals(codigoUsuario)) {
            throw new PermisoDenegadoException("Esta invitación es para otra cuenta (" + enmascarar(p.getUsuario().getEmail())
                    + "). Inicia sesión con esa cuenta para aceptarla.");
        }
        return conContexto(aplicarRespuesta(p, true), "correo");
    }

    /** HU14: rechazar desde el correo, sin iniciar sesión (como el "No fui yo" de HU8). */
    @Transactional
    public void rechazarPorToken(String token) {
        Participacion p = porToken(token);
        aplicarRespuesta(p, false); // sin sesión no pasa por @Auditar: se registra a mano
        logService.registrar(p.getUsuario().getCodigoUsuario(), AccionLog.RESPONDER_INVITACION, EntidadLog.META,
                p.getMeta().getCodigoMeta(), ResultadoLog.EXITO, "{\"aceptada\":false,\"desde\":\"correo\"}");
    }

    private InvitacionRespondidaResponse aplicarRespuesta(Participacion p, boolean aceptar) {
        Meta meta = p.getMeta();
        if (aceptar && meta.getEstado() != EstadoMeta.ACTIVA) {
            throw new ValidacionException(List.of("Esta meta ya se cumplió: ya no se pueden unir más personas"));
        }
        p.responder(aceptar);
        participacionRepository.save(p);
        return new InvitacionRespondidaResponse(meta.getCodigoMeta(), meta.getNombre(), aceptar);
    }

    /** Para el log (@Auditar): qué meta, si aceptó y desde dónde respondió. */
    private static InvitacionRespondidaResponse conContexto(InvitacionRespondidaResponse r, String desde) {
        ContextoAuditoria.agregar("codigoMeta", r.codigoMeta());
        ContextoAuditoria.agregar("aceptada", r.aceptada());
        ContextoAuditoria.agregar("desde", desde);
        return r;
    }

    /** Invitación pendiente y vigente de ese token, o 410. */
    private Participacion porToken(String token) {
        if (token == null || token.isBlank()) throw new InvitacionNoValidaException();
        return participacionRepository.findByTokenHash(TokensSeguros.hash(token.trim()))
                .filter(p -> p.getEstadoInvitacion() == EstadoInvitacion.PENDIENTE)
                .filter(p -> p.getTokenExpira() != null && p.getTokenExpira().isAfter(LocalDateTime.now(reloj)))
                .orElseThrow(InvitacionNoValidaException::new);
    }

    /** "laura.gomez@gmail.com" → "l***@gmail.com". */
    static String enmascarar(String email) {
        int arroba = email.indexOf('@');
        if (arroba <= 0) return "***";
        return email.charAt(0) + "***" + email.substring(arroba);
    }

    private boolean enviarCorreo(Usuario invitado, Usuario creador, Meta meta, String token) {
        String nombreInvitador = creador.getNombre() + " " + creador.getApellido();
        String base = urlFrontend + "/invitaciones/responder?token=" + token;
        String aceptar = base + "&accion=aceptar";
        String rechazar = base + "&accion=rechazar";
        // i18n: el correo sale en el idioma de quien invita (cabecera Accept-Language).
        var idioma = LocaleContextHolder.getLocale();
        try {
            correoService.enviar(invitado.getEmail(), PlantillaInvitacion.asunto(nombreInvitador, idioma),
                    PlantillaInvitacion.html(nombreInvitador, meta.getNombre(), aceptar, rechazar, DIAS_VIGENCIA, idioma),
                    PlantillaInvitacion.texto(nombreInvitador, meta.getNombre(), aceptar, rechazar, DIAS_VIGENCIA, idioma));
            return true;
        } catch (CorreoNoEnviadoException e) {
            log.warn("No se pudo enviar el correo de invitación a la meta {}", meta.getCodigoMeta(), e);
            return false;
        }
    }
}
