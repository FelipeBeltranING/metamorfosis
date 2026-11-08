package com.metamorfosis.backend.service;

import com.metamorfosis.backend.auditoria.Auditar;
import com.metamorfosis.backend.auditoria.ContextoAuditoria;
import com.metamorfosis.backend.dto.MetaResponse;
import com.metamorfosis.backend.dto.PerfilPublicoRequest;
import com.metamorfosis.backend.dto.PerfilPublicoResponse;
import com.metamorfosis.backend.dto.PersonalizacionPerfil;
import com.metamorfosis.backend.dto.PerfilResponse;
import com.metamorfosis.backend.dto.PrivacidadRequest;
import com.metamorfosis.backend.dto.PrivacidadResponse;
import com.metamorfosis.backend.dto.PerfilResponse.InsigniaObtenida;
import com.metamorfosis.backend.dto.PerfilResponse.ResumenMetas;
import com.metamorfosis.backend.dto.UsuarioResumenResponse;
import com.metamorfosis.backend.exception.UsuarioNoEncontradoException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.AccionLog;
import com.metamorfosis.backend.model.EntidadLog;
import com.metamorfosis.backend.model.EstadoMeta;
import com.metamorfosis.backend.model.Meta;
import com.metamorfosis.backend.model.Portada;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.InsigniaRepository;
import com.metamorfosis.backend.repository.MetaRepository;
import com.metamorfosis.backend.repository.UsuarioInsigniaRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * HU22: arma Mi perfil con los datos del usuario, el resumen de metas, las insignias y las últimas cumplidas.
 * HU24: busca personas y arma el perfil público de otra persona con los mismos bloques.
 * HU26: perfil privado.
 */
@Service
public class PerfilService {

    static final int ULTIMAS_CUMPLIDAS = 3;
    static final int MAXIMO_RESULTADOS = 20;   // HU24
    static final int MINIMO_LETRAS = 2;        // HU24
    static final int MAXIMO_FRASE = 100;       // HU19

    /** De la cumplida más reciente a la más antigua; si dos tienen la misma fecha, la de código mayor primero. */
    private static final Comparator<MetaResponse> MAS_RECIENTE_PRIMERO = Comparator
            .comparing(MetaResponse::fechaCumplida, Comparator.nullsFirst(Comparator.<LocalDate>naturalOrder()))
            .thenComparing(MetaResponse::codigoMeta)
            .reversed();

    private final UsuarioRepository usuarioRepository;
    private final MetaRepository metaRepository;
    private final UsuarioInsigniaRepository usuarioInsigniaRepository;
    private final InsigniaRepository insigniaRepository;

    public PerfilService(UsuarioRepository usuarioRepository, MetaRepository metaRepository,
                         UsuarioInsigniaRepository usuarioInsigniaRepository,
                         InsigniaRepository insigniaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.metaRepository = metaRepository;
        this.usuarioInsigniaRepository = usuarioInsigniaRepository;
        this.insigniaRepository = insigniaRepository;
    }

    @Auditar(accion = AccionLog.VER_PERFIL, entidad = EntidadLog.USUARIO)
    @Transactional(readOnly = true)
    public PerfilResponse verMiPerfil(Integer codigoUsuario) {
        Usuario usuario = usuarioRepository.findById(codigoUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException(codigoUsuario));

        // Las metas en las que participa con invitación aceptada (igual que el reporte, HU7).
        List<Meta> metas = metaRepository.findByParticipante(codigoUsuario);
        ResumenMetas resumen = resumen(metas);
        List<MetaResponse> ultimasCumplidas = ultimasCumplidas(metas, codigoUsuario);
        List<InsigniaObtenida> insignias = insignias(codigoUsuario);

        ContextoAuditoria.agregar("totalMetas", resumen.totalMetas());
        ContextoAuditoria.agregar("metasCumplidas", resumen.metasCumplidas());
        ContextoAuditoria.agregar("insignias", insignias.size());

        return new PerfilResponse(usuario.getNombre(), usuario.getApellido(), usuario.getEmail(),
                usuario.getFechaRegistro(), resumen, insignias, insigniaRepository.count(), ultimasCumplidas,
                usuario.isPerfilPrivado(), personalizacion(usuario));
    }

    /**
     * HU24: busca personas por nombre o apellido, sin distinguir mayúsculas ni tildes. Pide al
     * menos dos letras, no incluye a quien busca y devuelve máximo 20 personas ordenadas por nombre.
     */
    @Auditar(accion = AccionLog.BUSCAR_USUARIOS, entidad = EntidadLog.USUARIO)
    @Transactional(readOnly = true)
    public List<UsuarioResumenResponse> buscarUsuarios(String nombre, Integer codigoUsuario) {
        String patron = patronDeBusqueda(nombre);
        ContextoAuditoria.agregar("busqueda", patron.substring(1, patron.length() - 1));
        return usuarioRepository.buscarPorNombre(codigoUsuario, patron, PageRequest.of(0, MAXIMO_RESULTADOS))
                .stream()
                .map(UsuarioResumenResponse::desde)
                .toList();
    }

    /**
     * HU24: perfil público de otra persona. Los mismos bloques de Mi perfil, sin el correo y
     * contando solo las metas que el dueño no ocultó (HU25). El perfil privado es la HU26.
     */
    @Auditar(accion = AccionLog.VER_PERFIL_PUBLICO, entidad = EntidadLog.USUARIO)
    @Transactional(readOnly = true)
    public PerfilPublicoResponse verPerfilPublico(Integer codigoPerfil, Integer codigoUsuario) {
        Usuario dueno = usuarioRepository.findById(codigoPerfil)
                .orElseThrow(() -> new UsuarioNoEncontradoException(codigoPerfil));

        // HU26: de un perfil privado solo sale la cabecera; ni se consultan sus metas ni sus insignias.
        if (dueno.isPerfilPrivado()) {
            ContextoAuditoria.agregar("privado", true);
            return new PerfilPublicoResponse(dueno.getCodigoUsuario(), dueno.getNombre(), dueno.getApellido(),
                    dueno.getFotoPerfil(), dueno.getFechaRegistro(), null, List.of(),
                    insigniaRepository.count(), List.of(), true,
                    PersonalizacionPerfil.soloCabecera(dueno.getFrase(), dueno.getPortada()));
        }

        List<Meta> visibles = metaRepository.findByParticipante(codigoPerfil).stream()
                .filter(m -> !m.isOcultaPara(codigoPerfil))
                .toList();
        ResumenMetas resumen = resumen(visibles);
        List<InsigniaObtenida> insignias = insignias(codigoPerfil);

        ContextoAuditoria.agregar("metasVisibles", resumen.totalMetas());

        return new PerfilPublicoResponse(dueno.getCodigoUsuario(), dueno.getNombre(), dueno.getApellido(),
                dueno.getFotoPerfil(), dueno.getFechaRegistro(), resumen, insignias,
                insigniaRepository.count(), ultimasCumplidas(visibles, codigoPerfil), false,
                personalizacion(dueno));
    }

    /**
     * HU26: hace privado (true) o público (false) el perfil del usuario. Con el perfil privado, las
     * demás personas solo ven su foto, su nombre y su fecha de registro (verPerfilPublico).
     */
    @Auditar(accion = AccionLog.CAMBIAR_PRIVACIDAD, entidad = EntidadLog.USUARIO)
    @Transactional
    public PrivacidadResponse cambiarPrivacidad(PrivacidadRequest solicitud, Integer codigoUsuario) {
        if (solicitud == null || solicitud.privado() == null) {
            throw new ValidacionException(List.of("Indica si el perfil queda privado"));
        }
        Usuario usuario = usuarioRepository.findById(codigoUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException(codigoUsuario));

        boolean privado = solicitud.privado();
        ContextoAuditoria.agregar("privado", privado);
        usuario.setPerfilPrivado(privado);
        usuarioRepository.save(usuario);
        return new PrivacidadResponse(privado);
    }

    /**
     * HU19: guarda lo que los demás ven del perfil: la frase (se quitan espacios de más; vacía se
     * borra; máximo 100 caracteres) y la portada. Devuelve Mi perfil actualizado.
     */
    @Auditar(accion = AccionLog.EDITAR_PERFIL_PUBLICO, entidad = EntidadLog.USUARIO)
    @Transactional
    public PerfilResponse editarPerfilPublico(PerfilPublicoRequest pedido, Integer codigoUsuario) {
        Usuario usuario = usuarioRepository.findById(codigoUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException(codigoUsuario));
        if (pedido == null) throw new ValidacionException(List.of("Faltan los datos del perfil"));

        List<String> errores = new ArrayList<>();
        String frase = limpiarFrase(pedido.frase());
        if (frase != null && frase.length() > MAXIMO_FRASE) {
            errores.add("La frase puede tener hasta " + MAXIMO_FRASE + " caracteres");
        }
        Portada portada = portadaDe(pedido.portada(), errores);
        if (!errores.isEmpty()) throw new ValidacionException(errores);

        usuario.setFrase(frase);
        usuario.setPortada(portada);
        usuarioRepository.save(usuario);
        ContextoAuditoria.agregar("portada", portada.name());
        return verMiPerfil(codigoUsuario);
    }

    /** "  Un   libro a la vez " → "Un libro a la vez"; vacía → null. */
    static String limpiarFrase(String frase) {
        if (frase == null) return null;
        String limpia = frase.strip().replaceAll("\\s+", " ");
        return limpia.isEmpty() ? null : limpia;
    }

    private static Portada portadaDe(String valor, List<String> errores) {
        if (valor == null || valor.isBlank()) return Portada.NINGUNA;
        for (Portada p : Portada.values()) {
            if (p.name().equalsIgnoreCase(valor.strip())) return p;
        }
        errores.add("Elige una portada válida");
        return Portada.NINGUNA;
    }

    /** HU19: lo que el usuario eligió mostrar. */
    private PersonalizacionPerfil personalizacion(Usuario usuario) {
        return new PersonalizacionPerfil(usuario.getFrase(), usuario.getPortada(), null, List.of());
    }

    /**
     * HU24: arma el patrón LIKE de la búsqueda: sin espacios de más, en minúsculas, sin tildes
     * (la consulta quita las tildes del nombre con translate) y sin los comodines % y _.
     */
    static String patronDeBusqueda(String nombre) {
        String texto = nombre == null ? "" : Normalizer.normalize(nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[%_\\\\]", "")
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
        if (texto.length() < MINIMO_LETRAS) {
            throw new ValidacionException(List.of("Escribe al menos 2 letras para buscar"));
        }
        return "%" + texto + "%";
    }

    /** Activas, cumplidas y porcentaje (el mismo cálculo del reporte, HU7). */
    private static ResumenMetas resumen(List<Meta> metas) {
        long total = metas.size();
        long cumplidas = metas.stream().filter(m -> m.getEstado() == EstadoMeta.CUMPLIDA).count();
        return new ResumenMetas(total, total - cumplidas, cumplidas, ReporteService.porcentaje(cumplidas, total));
    }

    /** Las tres cumplidas más recientes, vistas por ese usuario (HU25: con su marca de oculta). */
    private static List<MetaResponse> ultimasCumplidas(List<Meta> metas, Integer codigoUsuario) {
        return metas.stream()
                .filter(m -> m.getEstado() == EstadoMeta.CUMPLIDA)
                .map(m -> MetaResponse.desde(m, codigoUsuario).sinParticipantes()) // HU13: el perfil no lista personas
                .sorted(MAS_RECIENTE_PRIMERO)
                .limit(ULTIMAS_CUMPLIDAS)
                .toList();
    }

    /** Insignias ganadas (HU11), en el orden de la consulta: la más reciente primero. */
    private List<InsigniaObtenida> insignias(Integer codigoUsuario) {
        return usuarioInsigniaRepository.insigniasDelUsuario(codigoUsuario).stream()
                .map(InsigniaObtenida::desde)
                .toList();
    }
}
