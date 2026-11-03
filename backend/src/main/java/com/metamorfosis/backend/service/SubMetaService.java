package com.metamorfosis.backend.service;

import com.metamorfosis.backend.auditoria.Auditar;
import com.metamorfosis.backend.config.CalendarioUsuario;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

/**
 * Sub-metas: crear, editar y eliminar los pasos diarios de una meta. Solo el creador de la meta,
 * y solo mientras la meta está activa (una meta cumplida queda en solo lectura).
 */
@Service
public class SubMetaService {

    static final BigDecimal OBJETIVO_MAXIMO = new BigDecimal("1000000");
    static final String MENSAJE_SOLO_LECTURA = "La meta ya está cumplida: su camino es solo de lectura";

    private final MetaRepository metaRepository;
    private final SubMetaRepository subMetaRepository;
    private final CalendarioUsuario calendario;

    public SubMetaService(MetaRepository metaRepository, SubMetaRepository subMetaRepository,
                          CalendarioUsuario calendario) {
        this.metaRepository = metaRepository;
        this.subMetaRepository = subMetaRepository;
        this.calendario = calendario;
    }

    @Auditar(accion = AccionLog.CREAR_SUB_META, entidad = EntidadLog.SUB_META)
    @Transactional
    public SubMetaResponse crear(Integer codigoMeta, SubMetaRequest pedido, Integer codigoUsuario) {
        Meta meta = metaRepository.findById(codigoMeta)
                .orElseThrow(() -> new MetaNoEncontradaException(codigoMeta));
        exigirCreadorYActiva(meta, codigoUsuario);
        if (pedido == null || pedido.tipo() == null) {
            throw new ValidacionException(List.of("Elige si la sub-meta es Sí / No o medible"));
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
            errores.add("Ya tienes una sub-meta con este nombre en esta meta");
        }

        String pregunta = recortar(p.pregunta());
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
        if (p.dias() == null || p.dias().isEmpty()) {
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
