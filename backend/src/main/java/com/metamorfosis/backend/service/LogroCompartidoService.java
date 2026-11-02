package com.metamorfosis.backend.service;

import com.metamorfosis.backend.auditoria.Auditar;
import com.metamorfosis.backend.dto.LogroCompartidoResponse;
import com.metamorfosis.backend.dto.LogroPublicoResponse;
import com.metamorfosis.backend.exception.LogroNoEncontradoException;
import com.metamorfosis.backend.exception.MetaNoEncontradaException;
import com.metamorfosis.backend.exception.PermisoDenegadoException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.AccionLog;
import com.metamorfosis.backend.model.EntidadLog;
import com.metamorfosis.backend.model.EstadoMeta;
import com.metamorfosis.backend.model.LogroCompartido;
import com.metamorfosis.backend.model.Meta;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.LogroCompartidoRepository;
import com.metamorfosis.backend.repository.MetaRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

/**
 * HU21: compartir un logro. Genera (o reutiliza) un enlace público con un token aleatorio;
 * quien lo recibe ve solo la meta cumplida y el nombre visible de quien la compartió.
 */
@Service
public class LogroCompartidoService {

    static final int LONGITUD_TOKEN = 10;
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final String MENSAJE_NO_CUMPLIDA = "Solo se puede compartir una meta cumplida";

    private final MetaRepository metaRepository;
    private final UsuarioRepository usuarioRepository;
    private final LogroCompartidoRepository logroRepository;
    private final String urlBase;
    private final SecureRandom aleatorio = new SecureRandom();

    public LogroCompartidoService(MetaRepository metaRepository, UsuarioRepository usuarioRepository,
                                  LogroCompartidoRepository logroRepository,
                                  @Value("${app.frontend.url}") String urlBase) {
        this.metaRepository = metaRepository;
        this.usuarioRepository = usuarioRepository;
        this.logroRepository = logroRepository;
        // Sin "/" final, para armar siempre urlBase + "/logros/" + token.
        this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
    }

    /**
     * Crea el enlace del logro, o devuelve el que ya existía para esa meta y ese usuario.
     * Solo un participante aceptado comparte, y solo si la meta está CUMPLIDA (HU6).
     */
    @Auditar(accion = AccionLog.COMPARTIR_LOGRO, entidad = EntidadLog.META)
    @Transactional
    public LogroCompartidoResponse compartirLogro(Integer codigoMeta, Integer codigoUsuario) {
        Meta meta = metaRepository.findById(codigoMeta)
                .orElseThrow(() -> new MetaNoEncontradaException(codigoMeta));

        if (!metaRepository.esParticipanteAceptado(codigoMeta, codigoUsuario)) {
            throw new PermisoDenegadoException("Solo un participante de la meta puede compartir el logro");
        }
        validar(meta);

        Usuario usuario = usuarioRepository.getReferenceById(codigoUsuario);
        LogroCompartido logro = logroRepository.findByMetaAndUsuario(meta, usuario)
                .orElseGet(() -> logroRepository.save(new LogroCompartido(meta, usuario, generarToken())));

        return new LogroCompartidoResponse(
                logro.getToken(),
                urlBase + "/logros/" + logro.getToken(),
                meta.getNombre(),
                meta.getFechaCumplida(codigoUsuario));
    }

    /** Lo que ve quien abre el enlace (ruta pública, sin sesión). */
    @Transactional(readOnly = true)
    public LogroPublicoResponse obtenerLogroPublico(String token) {
        LogroCompartido logro = logroRepository.findByToken(token)
                .orElseThrow(() -> new LogroNoEncontradoException("Este logro no existe o el enlace no es válido"));

        Meta meta = logro.getMeta();
        Usuario usuario = logro.getUsuario();
        return new LogroPublicoResponse(
                meta.getNombre(),
                meta.getTipo(),
                meta.getPlazo(),
                meta.getFechaCumplida(usuario.getCodigoUsuario()),
                nombreVisible(usuario));
    }

    /** La meta debe estar cumplida: compartir una meta activa no es un logro todavía. */
    private void validar(Meta meta) {
        if (meta.getEstado() != EstadoMeta.CUMPLIDA) {
            throw new ValidacionException(List.of(MENSAJE_NO_CUMPLIDA));
        }
    }

    /** Token aleatorio de 10 caracteres sin letras ambiguas (0/O, 1/l/I); se repite si ya existe. */
    private String generarToken() {
        String token;
        do {
            StringBuilder sb = new StringBuilder(LONGITUD_TOKEN);
            for (int i = 0; i < LONGITUD_TOKEN; i++) {
                sb.append(ALFABETO.charAt(aleatorio.nextInt(ALFABETO.length())));
            }
            token = sb.toString();
        } while (logroRepository.findByToken(token).isPresent());
        return token;
    }

    /** "Laura Gómez" → "Laura G.": quien recibe el enlace no ve el apellido completo. */
    static String nombreVisible(Usuario usuario) {
        String nombre = usuario.getNombre() == null ? "" : usuario.getNombre().trim();
        String apellido = usuario.getApellido() == null ? "" : usuario.getApellido().trim();
        return apellido.isEmpty() ? nombre : nombre + " " + apellido.charAt(0) + ".";
    }
}
