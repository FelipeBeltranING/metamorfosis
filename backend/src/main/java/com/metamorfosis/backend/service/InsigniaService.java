package com.metamorfosis.backend.service;

import com.metamorfosis.backend.dto.InsigniaResponse;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * HU11: otorga las insignias del catálogo. Se llama al marcar una meta como cumplida (HU6),
 * dentro de la misma transacción: si algo falla, ni la meta ni la insignia quedan guardadas.
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

    /** true si el conteo de metas cumplidas llega a lo que pide la insignia. */
    static boolean alcanza(Insignia insignia, Conteo conteo) {
        long valor = switch (insignia.getCriterio()) {
            case METAS_CUMPLIDAS -> conteo.total();
            case METAS_CUMPLIDAS_PLAZO -> conteo.porPlazo().getOrDefault(insignia.getPlazoRequerido(), 0L);
            case TIPOS_DISTINTOS -> conteo.tipos().size();
        };
        return valor >= insignia.getCantidadRequerida();
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
