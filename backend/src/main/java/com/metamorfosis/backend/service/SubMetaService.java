package com.metamorfosis.backend.service;

import com.metamorfosis.backend.auditoria.Auditar;
import com.metamorfosis.backend.config.CalendarioUsuario;
import com.metamorfosis.backend.dto.AvanceResponse;
import com.metamorfosis.backend.dto.LogroHitoResponse;
import com.metamorfosis.backend.dto.SubMetaRequest;
import com.metamorfosis.backend.dto.SubMetaResponse;
import com.metamorfosis.backend.exception.MetaNoEncontradaException;
import com.metamorfosis.backend.exception.PermisoDenegadoException;
import com.metamorfosis.backend.exception.SubMetaNoEncontradaException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.AccionLog;
import com.metamorfosis.backend.model.EntidadLog;
import com.metamorfosis.backend.model.EstadoMeta;
import com.metamorfosis.backend.model.Meta;
import com.metamorfosis.backend.model.SubMeta;
import com.metamorfosis.backend.model.TipoObjetivo;
import com.metamorfosis.backend.model.TipoSubMeta;
import com.metamorfosis.backend.repository.MetaRepository;
import com.metamorfosis.backend.repository.SubMetaRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

/**
 * HU28: trazar el camino de una meta (crear, editar y eliminar sus hitos y hábitos). Solo el creador
 * de la meta, y solo mientras está activa (una meta cumplida queda en solo lectura).
 * HU5: marcar un hito como logrado o devolverlo a pendiente (cualquier participante: el hito es del equipo).
 */
@Service
public class SubMetaService {

    static final BigDecimal OBJETIVO_MAXIMO = new BigDecimal("1000000");
    static final String MENSAJE_SOLO_LECTURA = "La meta ya está cumplida: su camino es solo de lectura";

    private final MetaRepository metaRepository;
    private final SubMetaRepository subMetaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CalendarioUsuario calendario;

    public SubMetaService(MetaRepository metaRepository, SubMetaRepository subMetaRepository,
                          UsuarioRepository usuarioRepository, CalendarioUsuario calendario) {
        this.metaRepository = metaRepository;
        this.subMetaRepository = subMetaRepository;
        this.usuarioRepository = usuarioRepository;
        this.calendario = calendario;
    }

    @Auditar(accion = AccionLog.CREAR_SUB_META, entidad = EntidadLog.SUB_META)
    @Transactional
    public SubMetaResponse crear(Integer codigoMeta, SubMetaRequest pedido, Integer codigoUsuario) {
        Meta meta = metaRepository.findById(codigoMeta)
                .orElseThrow(() -> new MetaNoEncontradaException(codigoMeta));
        exigirCreadorYActiva(meta, codigoUsuario);
        if (pedido == null || pedido.tipo() == null) {
            throw new ValidacionException(List.of("Elige si el paso es un hito o un hábito"));
        }
        Datos datos = validar(pedido, pedido.tipo(), codigoMeta, null);

        SubMeta subMeta = new SubMeta(meta, pedido.tipo(), calendario.hoy());
        datos.aplicarA(subMeta);
        return SubMetaResponse.desde(subMetaRepository.save(subMeta));
    }

    @Auditar(accion = AccionLog.EDITAR_SUB_META, entidad = EntidadLog.SUB_META)
    @Transactional
    public SubMetaResponse editar(Integer codigoSubMeta, SubMetaRequest pedido, Integer codigoUsuario) {
        SubMeta subMeta = buscar(codigoSubMeta);
        exigirCreadorYActiva(subMeta.getMeta(), codigoUsuario);
        if (pedido == null) {
            throw new ValidacionException(List.of("Faltan los datos de la sub-meta"));
        }
        if (pedido.tipo() != null && pedido.tipo() != subMeta.getTipo()) {
            throw new ValidacionException(List.of("El tipo de una sub-meta no se puede cambiar"));
        }
        Datos datos = validar(pedido, subMeta.getTipo(), subMeta.getMeta().getCodigoMeta(), codigoSubMeta);
        datos.aplicarA(subMeta);
        return SubMetaResponse.desde(subMetaRepository.save(subMeta));
    }

    /** Elimina la sub-meta; sus registros se borran en cascada (ON DELETE CASCADE). */
    @Auditar(accion = AccionLog.ELIMINAR_SUB_META, entidad = EntidadLog.SUB_META)
    @Transactional
    public void eliminar(Integer codigoSubMeta, Integer codigoUsuario) {
        SubMeta subMeta = buscar(codigoSubMeta);
        exigirCreadorYActiva(subMeta.getMeta(), codigoUsuario);
        subMetaRepository.delete(subMeta);
    }

    /**
     * HU5: marca un hito como logrado (hoy, por quien lo marca). Si ya estaba logrado conserva la fecha
     * original. Responde el avance de la meta (HU29) para que la app proponga cumplirla al llegar al 100 %.
     */
    @Auditar(accion = AccionLog.LOGRAR_HITO, entidad = EntidadLog.SUB_META)
    @Transactional
    public LogroHitoResponse lograrHito(Integer codigoSubMeta, Integer codigoUsuario) {
        SubMeta hito = buscarHitoParaMarcar(codigoSubMeta, codigoUsuario);
        if (!hito.isLogrado()) {
            hito.lograr(usuarioRepository.getReferenceById(codigoUsuario), calendario.hoy());
            subMetaRepository.save(hito);
        }
        return new LogroHitoResponse(SubMetaResponse.desde(hito), avance(hito.getMeta().getCodigoMeta()));
    }

    /** HU5: devuelve un hito a pendiente (por si se marcó por error). */
    @Auditar(accion = AccionLog.DESHACER_HITO, entidad = EntidadLog.SUB_META)
    @Transactional
    public LogroHitoResponse deshacerHito(Integer codigoSubMeta, Integer codigoUsuario) {
        SubMeta hito = buscarHitoParaMarcar(codigoSubMeta, codigoUsuario);
        if (hito.isLogrado()) {
            hito.deshacerLogro();
            subMetaRepository.save(hito);
        }
        return new LogroHitoResponse(SubMetaResponse.desde(hito), avance(hito.getMeta().getCodigoMeta()));
    }

    /** El hito es del equipo: cualquier participante aceptado lo marca, mientras la meta esté activa. */
    private SubMeta buscarHitoParaMarcar(Integer codigoSubMeta, Integer codigoUsuario) {
        SubMeta hito = buscar(codigoSubMeta);
        Meta meta = hito.getMeta();
        if (!metaRepository.esParticipanteAceptado(meta.getCodigoMeta(), codigoUsuario)) {
            throw new PermisoDenegadoException("Solo los participantes de la meta pueden marcar sus hitos");
        }
        if (meta.getEstado() != EstadoMeta.ACTIVA) {
            throw new ValidacionException(List.of(MENSAJE_SOLO_LECTURA));
        }
        if (!hito.esHito()) {
            throw new ValidacionException(List.of("Solo los hitos se marcan como logrados; los hábitos se registran por día"));
        }
        return hito;
    }

    /** HU29: hitos logrados sobre el total de hitos de la meta. */
    public AvanceResponse avance(Integer codigoMeta) {
        List<SubMeta> pasos = subMetaRepository.findByMetaCodigoMetaOrderByCodigoSubMetaAsc(codigoMeta);
        int total = 0;
        int logrados = 0;
        for (SubMeta p : pasos) {
            if (!p.esHito()) continue;
            total++;
            if (p.isLogrado()) logrados++;
        }
        return AvanceResponse.de(logrados, total);
    }

    private SubMeta buscar(Integer codigoSubMeta) {
        return subMetaRepository.findById(codigoSubMeta)
                .orElseThrow(() -> new SubMetaNoEncontradaException(codigoSubMeta));
    }

    private void exigirCreadorYActiva(Meta meta, Integer codigoUsuario) {
        if (!meta.getCreador().getCodigoUsuario().equals(codigoUsuario)) {
            throw new PermisoDenegadoException("Solo el creador de la meta puede cambiar sus sub-metas");
        }
        if (meta.getEstado() != EstadoMeta.ACTIVA) {
            throw new ValidacionException(List.of(MENSAJE_SOLO_LECTURA));
        }
    }

    /** Valida y normaliza el pedido; junta todos los errores para mostrarlos de una vez. */
    private Datos validar(SubMetaRequest p, TipoSubMeta tipo, Integer codigoMeta, Integer codigoSubMeta) {
        List<String> errores = new ArrayList<>();

        String nombre = recortar(p.nombre());
        if (nombre == null) {
            errores.add("El nombre es obligatorio");
        } else if (nombre.length() > 100) {
            errores.add("El nombre no puede tener más de 100 caracteres");
        } else if (codigoSubMeta == null
                ? subMetaRepository.existsByMetaCodigoMetaAndNombreIgnoreCase(codigoMeta, nombre)
                : subMetaRepository.existsByMetaCodigoMetaAndNombreIgnoreCaseAndCodigoSubMetaNot(
                        codigoMeta, nombre, codigoSubMeta)) {
            errores.add("Ya tienes un paso con este nombre en esta meta");
        }

        boolean hito = tipo == TipoSubMeta.HITO;
        String pregunta = hito ? null : recortar(p.pregunta());
        if (pregunta != null && pregunta.length() > 200) {
            errores.add("La pregunta no puede tener más de 200 caracteres");
        }
        String notas = recortar(p.notas());
        if (notas != null && notas.length() > 500) {
            errores.add("Las notas no pueden tener más de 500 caracteres");
        }

        String unidad = null;
        BigDecimal objetivo = null;
        TipoObjetivo tipoObjetivo = null;
        if (tipo == TipoSubMeta.MEDIBLE) {
            unidad = recortar(p.unidad());
            if (unidad == null) errores.add("Escribe en qué lo mides (la unidad)");
            else if (unidad.length() > 30) errores.add("La unidad no puede tener más de 30 caracteres");

            objetivo = p.objetivo();
            if (objetivo == null) {
                errores.add("El objetivo es obligatorio");
            } else if (objetivo.signum() <= 0) {
                errores.add("El objetivo debe ser mayor que 0");
            } else if (objetivo.compareTo(OBJETIVO_MAXIMO) > 0) {
                errores.add("El objetivo no puede ser mayor que 1.000.000");
            } else if (objetivo.stripTrailingZeros().scale() > 2) {
                errores.add("El objetivo admite máximo 2 decimales");
            }

            tipoObjetivo = p.tipoObjetivo();
            if (tipoObjetivo == null) errores.add("Elige si el objetivo es \"al menos\" o \"como máximo\"");
        }

        int dias = 0;
        if (hito || p.dias() == null || p.dias().isEmpty()) {
            dias = SubMeta.TODOS_LOS_DIAS;
        } else {
            for (Integer d : p.dias()) {
                if (d == null || d < 1 || d > 7) {
                    errores.add("Los días van del 1 (lunes) al 7 (domingo)");
                    break;
                }
                dias |= SubMeta.bitDe(DayOfWeek.of(d));
            }
        }

        if (!errores.isEmpty()) throw new ValidacionException(errores);
        return new Datos(nombre, pregunta, unidad, objetivo, tipoObjetivo, dias, notas);
    }

    private static String recortar(String texto) {
        if (texto == null) return null;
        String limpio = texto.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private record Datos(String nombre, String pregunta, String unidad, BigDecimal objetivo,
                         TipoObjetivo tipoObjetivo, int dias, String notas) {
        void aplicarA(SubMeta s) {
            s.actualizar(nombre, pregunta, unidad, objetivo, tipoObjetivo, dias, notas);
        }
    }
}
