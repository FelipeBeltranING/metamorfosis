package com.metamorfosis.backend.dto;

import com.metamorfosis.backend.model.SubMeta;
import com.metamorfosis.backend.model.TipoObjetivo;
import com.metamorfosis.backend.model.TipoSubMeta;
import com.metamorfosis.backend.model.Usuario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Un paso del camino (HU28). En los hábitos van la pregunta, la unidad, el objetivo y los días; en los
 * hitos van {@code fechaLogrado} (null = pendiente) y {@code logradoPor}, el nombre de quien lo marcó.
 */
public record SubMetaResponse(
        Integer codigoSubMeta,
        Integer codigoMeta,
        String nombre,
        String pregunta,
        TipoSubMeta tipo,
        String unidad,
        BigDecimal objetivo,
        TipoObjetivo tipoObjetivo,
        List<Integer> dias,
        String notas,
        LocalDate fechaCreacion,
        LocalDate fechaLogrado,
        String logradoPor
) {
    public static SubMetaResponse desde(SubMeta s) {
        Usuario quien = s.getLogradoPor();
        return new SubMetaResponse(s.getCodigoSubMeta(), s.getMeta().getCodigoMeta(), s.getNombre(),
                s.getPregunta(), s.getTipo(), s.getUnidad(), s.getObjetivo(), s.getTipoObjetivo(),
                s.esHito() ? List.of() : s.getDiasSemana(), s.getNotas(), s.getFechaCreacion(),
                s.getFechaLogrado(), quien == null ? null : quien.getNombre() + " " + quien.getApellido());
    }
}
