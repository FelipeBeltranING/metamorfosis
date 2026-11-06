package com.metamorfosis.backend.service;

import com.metamorfosis.backend.auditoria.Auditar;
import com.metamorfosis.backend.auditoria.ContextoAuditoria;
import com.metamorfosis.backend.dto.GaleriaInsigniasResponse;
import com.metamorfosis.backend.dto.GaleriaInsigniasResponse.InsigniaGaleria;
import com.metamorfosis.backend.dto.InsigniaResponse;
import com.metamorfosis.backend.model.AccionLog;
import com.metamorfosis.backend.model.EntidadLog;
import com.metamorfosis.backend.model.Insignia;
import com.metamorfosis.backend.model.Plazo;
import com.metamorfosis.backend.model.TipoMeta;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.model.UsuarioInsignia;
import com.metamorfosis.backend.repository.InsigniaRepository;
import com.metamorfosis.backend.repository.MetaRepository;
import com.metamorfosis.backend.repository.UsuarioInsigniaRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * HU11: otorga las insignias del catálogo. Se llama al marcar una meta como cumplida (HU6),
 * dentro de la misma transacción: si algo falla, ni la meta ni la insignia quedan guardadas.
 * HU12: arma la galería con el mismo conteo, así la galería y los avisos siempre coinciden.
 */
@Service
public class InsigniaService {

    private final InsigniaRepository insigniaRepository;
    private final UsuarioInsigniaRepository usuarioInsigniaRepository;
    private final MetaRepository metaRepository;
    private final UsuarioRepository usuarioRepository;

    public InsigniaService(InsigniaRepository insigniaRepository,
                           UsuarioInsigniaRepository usuarioInsigniaRepository,
                           MetaRepository metaRepository, UsuarioRepository usuarioRepository) {
        this.insigniaRepository = insigniaRepository;
        this.usuarioInsigniaRepository = usuarioInsigniaRepository;
        this.metaRepository = metaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Cuenta las metas cumplidas del usuario y le otorga, con la fecha de hoy, las insignias que
     * ya alcanza y todavía no tiene. Devuelve las nuevas en el orden del catálogo.
     */
    @Transactional
    public List<InsigniaResponse> otorgarNuevas(Integer codigoUsuario, LocalDate hoy) {
        List<Object[]> filas = metaRepository.plazosYTiposCumplidos(codigoUsuario);
        if (filas.isEmpty()) return List.of();

        Conteo conteo = Conteo.desde(filas);
        Set<Integer> obtenidas = new HashSet<>(usuarioInsigniaRepository.codigosObtenidos(codigoUsuario));
        Usuario usuario = usuarioRepository.getReferenceById(codigoUsuario);

        List<InsigniaResponse> nuevas = new ArrayList<>();
        for (Insignia insignia : insigniaRepository.findAllByOrderByCodigoInsigniaAsc()) {
            if (obtenidas.contains(insignia.getCodigoInsignia()) || !alcanza(insignia, conteo)) continue;
            usuarioInsigniaRepository.save(new UsuarioInsignia(usuario, insignia, hoy));
            nuevas.add(InsigniaResponse.desde(insignia));
        }
        return nuevas;
    }

    /**
     * HU12: las seis insignias del catálogo en su orden; las ganadas con su fecha y las pendientes
     * con el avance del usuario hacia cada una.
     */
    @Auditar(accion = AccionLog.VER_INSIGNIAS, entidad = EntidadLog.USUARIO)
    @Transactional(readOnly = true)
    public GaleriaInsigniasResponse verGaleria(Integer codigoUsuario) {
        Conteo conteo = Conteo.desde(metaRepository.plazosYTiposCumplidos(codigoUsuario));
        Map<Integer, LocalDate> obtenidas = new HashMap<>();
        for (UsuarioInsignia ui : usuarioInsigniaRepository.insigniasDelUsuario(codigoUsuario)) {
            obtenidas.put(ui.getInsignia().getCodigoInsignia(), ui.getFechaObtenida());
        }

        List<InsigniaGaleria> galeria = insigniaRepository.findAllByOrderByCodigoInsigniaAsc().stream()
                .map(i -> InsigniaGaleria.de(i, obtenidas.get(i.getCodigoInsignia()), avance(i, conteo)))
                .toList();
        int ganadas = (int) galeria.stream().filter(InsigniaGaleria::obtenida).count();

        ContextoAuditoria.agregar("ganadas", ganadas);
        return new GaleriaInsigniasResponse(ganadas, galeria.size(), galeria);
    }

    /** true si el conteo de metas cumplidas llega a lo que pide la insignia. */
    static boolean alcanza(Insignia insignia, Conteo conteo) {
        return valor(insignia, conteo) >= insignia.getCantidadRequerida();
    }

    /** HU12: cuánto lleva el usuario hacia la insignia, sin pasar de lo que pide. */
    static int avance(Insignia insignia, Conteo conteo) {
        return (int) Math.min(valor(insignia, conteo), insignia.getCantidadRequerida());
    }

    /** Lo que mide el criterio de la insignia: total, metas del plazo pedido o tipos distintos. */
    private static long valor(Insignia insignia, Conteo conteo) {
        return switch (insignia.getCriterio()) {
            case METAS_CUMPLIDAS -> conteo.total();
            case METAS_CUMPLIDAS_PLAZO -> conteo.porPlazo().getOrDefault(insignia.getPlazoRequerido(), 0L);
            case TIPOS_DISTINTOS -> conteo.tipos().size();
        };
    }

    /** Metas cumplidas del usuario: total, cuántas de cada plazo y qué tipos distintos. */
    record Conteo(long total, Map<Plazo, Long> porPlazo, Set<TipoMeta> tipos) {

        static Conteo desde(List<Object[]> filas) {
            Map<Plazo, Long> porPlazo = new EnumMap<>(Plazo.class);
            Set<TipoMeta> tipos = EnumSet.noneOf(TipoMeta.class);
            for (Object[] fila : filas) {
                porPlazo.merge((Plazo) fila[0], 1L, Long::sum);
                tipos.add((TipoMeta) fila[1]);
            }
            return new Conteo(filas.size(), porPlazo, tipos);
        }
    }
}
