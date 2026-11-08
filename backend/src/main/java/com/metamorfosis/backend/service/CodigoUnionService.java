package com.metamorfosis.backend.service;

import com.metamorfosis.backend.auditoria.Auditar;
import com.metamorfosis.backend.dto.CodigoUnionResponse;
import com.metamorfosis.backend.dto.MetaResponse;
import com.metamorfosis.backend.dto.UnirseRequest;
import com.metamorfosis.backend.exception.ConflictoException;
import com.metamorfosis.backend.exception.MetaNoEncontradaException;
import com.metamorfosis.backend.exception.PermisoDenegadoException;
import com.metamorfosis.backend.exception.SolicitudBloqueadaException;
import com.metamorfosis.backend.exception.UsuarioNoEncontradoException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.AccionLog;
import com.metamorfosis.backend.model.EntidadLog;
import com.metamorfosis.backend.model.EstadoInvitacion;
import com.metamorfosis.backend.model.EstadoMeta;
import com.metamorfosis.backend.model.Meta;
import com.metamorfosis.backend.model.Participacion;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.MetaRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unirse a una meta con un código. El creador genera un código de 8 caracteres (sin letras
 * que se confundan, como O/0 o I/1) que vence en 7 días; puede cambiarlo o desactivarlo. Quien
 * lo escribe entra directo como participante. Para que nadie pruebe códigos al azar, cada
 * usuario tiene máximo 10 intentos fallidos cada 15 minutos.
 */
@Service
public class CodigoUnionService {

    static final String ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    static final int LARGO = 8;
    static final int DIAS_VIGENCIA = 7;
    static final int MAXIMO_FALLOS = 10;
    static final Duration VENTANA_FALLOS = Duration.ofMinutes(15);

    private final MetaRepository metaRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock reloj;
    private final SecureRandom aleatorio = new SecureRandom();
    private final Map<Integer, Deque<Instant>> fallos = new ConcurrentHashMap<>();

    @Autowired
    public CodigoUnionService(MetaRepository metaRepository, UsuarioRepository usuarioRepository) {
        this(metaRepository, usuarioRepository, Clock.systemUTC());
    }

    CodigoUnionService(MetaRepository metaRepository, UsuarioRepository usuarioRepository, Clock reloj) {
        this.metaRepository = metaRepository;
        this.usuarioRepository = usuarioRepository;
        this.reloj = reloj;
    }

    /** El código activo de la meta, o NINGUNO (solo el creador lo ve). */
    @Transactional(readOnly = true)
    public CodigoUnionResponse verCodigo(Integer codigoMeta, Integer codigoUsuario) {
        Meta meta = metaDelCreador(codigoMeta, codigoUsuario);
        return meta.codigoUnionVigente(LocalDateTime.now(reloj))
                ? new CodigoUnionResponse(meta.getCodigoUnion(), meta.getCodigoUnionExpira())
                : CodigoUnionResponse.NINGUNO;
    }

    /** Genera un código nuevo (el anterior deja de servir). Solo el creador y solo con la meta activa. */
    @Auditar(accion = AccionLog.GENERAR_CODIGO_META, entidad = EntidadLog.META)
    @Transactional
    public CodigoUnionResponse generar(Integer codigoMeta, Integer codigoUsuario) {
        Meta meta = metaDelCreador(codigoMeta, codigoUsuario);
        if (meta.getEstado() != EstadoMeta.ACTIVA) {
            throw new ValidacionException(List.of("Solo se puede invitar a metas activas"));
        }
        String codigo;
        do {
            codigo = nuevoCodigo();
        } while (metaRepository.findByCodigoUnion(codigo).isPresent());
        meta.activarCodigoUnion(codigo, LocalDateTime.now(reloj).plusDays(DIAS_VIGENCIA));
        metaRepository.save(meta);
        return new CodigoUnionResponse(meta.getCodigoUnion(), meta.getCodigoUnionExpira());
    }

    /** Desactiva el código: quien lo tenga ya no puede unirse. */
    @Auditar(accion = AccionLog.DESACTIVAR_CODIGO_META, entidad = EntidadLog.META)
    @Transactional
    public void desactivar(Integer codigoMeta, Integer codigoUsuario) {
        Meta meta = metaDelCreador(codigoMeta, codigoUsuario);
        meta.desactivarCodigoUnion();
        metaRepository.save(meta);
    }

    /** Se une a la meta del código. Devuelve la meta, como la verá en sus metas. */
    @Auditar(accion = AccionLog.UNIRSE_CON_CODIGO, entidad = EntidadLog.META)
    @Transactional
    public MetaResponse unirse(UnirseRequest pedido, Integer codigoUsuario) {
        String codigo = normalizar(pedido == null ? null : pedido.codigo());
        if (codigo.isEmpty()) {
            throw new ValidacionException(List.of("Escribe el código que te compartieron"));
        }
        revisarIntentos(codigoUsuario);

        LocalDateTime ahora = LocalDateTime.now(reloj);
        Optional<Meta> encontrada = codigo.length() == LARGO
                ? metaRepository.findByCodigoUnion(codigo).filter(m -> m.codigoUnionVigente(ahora))
                : Optional.empty();
        if (encontrada.isEmpty()) {
            registrarFallo(codigoUsuario);
            throw new ValidacionException(List.of("Ese código no existe o ya venció. Revisa que esté bien escrito."));
        }
        Meta meta = encontrada.get();
        if (meta.getEstado() != EstadoMeta.ACTIVA) {
            throw new ValidacionException(List.of("Esta meta ya se cumplió: ya no se pueden unir más personas"));
        }
        Optional<Participacion> previa = meta.participacionDeUsuario(codigoUsuario);
        if (previa.isPresent() && previa.get().getEstadoInvitacion() == EstadoInvitacion.ACEPTADA) {
            throw new ConflictoException("Ya participas en esta meta");
        }
        boolean yaContaba = previa.isPresent() && previa.get().getEstadoInvitacion() == EstadoInvitacion.PENDIENTE;
        if (!yaContaba && meta.participantesVisibles().size() >= InvitacionService.MAXIMO_PARTICIPANTES) {
            throw new ValidacionException(List.of("La meta ya tiene el máximo de "
                    + InvitacionService.MAXIMO_PARTICIPANTES + " personas"));
        }
        Usuario usuario = usuarioRepository.findById(codigoUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException(codigoUsuario));
        meta.unirse(usuario);
        return MetaResponse.desde(metaRepository.save(meta), codigoUsuario);
    }

    private Meta metaDelCreador(Integer codigoMeta, Integer codigoUsuario) {
        Meta meta = metaRepository.findById(codigoMeta)
                .orElseThrow(() -> new MetaNoEncontradaException(codigoMeta));
        if (!meta.getCreador().getCodigoUsuario().equals(codigoUsuario)) {
            throw new PermisoDenegadoException("Solo el creador puede manejar el código de la meta");
        }
        return meta;
    }

    private String nuevoCodigo() {
        StringBuilder sb = new StringBuilder(LARGO);
        for (int i = 0; i < LARGO; i++) sb.append(ALFABETO.charAt(aleatorio.nextInt(ALFABETO.length())));
        return sb.toString();
    }

    /** "abcd-efgh " → "ABCDEFGH". */
    static String normalizar(String codigo) {
        if (codigo == null) return "";
        return codigo.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private void revisarIntentos(Integer codigoUsuario) {
        Deque<Instant> lista = fallos.get(codigoUsuario);
        if (lista == null) return;
        synchronized (lista) {
            Instant limite = reloj.instant().minus(VENTANA_FALLOS);
            while (!lista.isEmpty() && lista.peekFirst().isBefore(limite)) lista.pollFirst();
            if (lista.size() >= MAXIMO_FALLOS) {
                throw new SolicitudBloqueadaException("Demasiados intentos con códigos. Espera unos minutos e inténtalo de nuevo.");
            }
        }
    }

    private void registrarFallo(Integer codigoUsuario) {
        Deque<Instant> lista = fallos.computeIfAbsent(codigoUsuario, k -> new ArrayDeque<>());
        synchronized (lista) {
            lista.addLast(reloj.instant());
        }
    }
}
