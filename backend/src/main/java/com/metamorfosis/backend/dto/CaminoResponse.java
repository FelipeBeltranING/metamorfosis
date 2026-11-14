package com.metamorfosis.backend.dto;

import com.metamorfosis.backend.model.EstadoMeta;
import com.metamorfosis.backend.model.Plazo;
import com.metamorfosis.backend.model.TipoMeta;

import java.time.LocalDate;
import java.util.List;

/**
 * "Tu camino" de una meta para el usuario que consulta.
 * <ul>
 *   <li>HU29: {@code avance} son los hitos logrados sobre el total (avance real de la meta).</li>
 *   <li>HU5: constancias de los hábitos en % (null si aún no hay días que contar).</li>
 *   <li>{@code soloLectura}: la meta está cumplida. {@code puedeEditar}: es su creador y está activa.</li>
 *   <li>HU13: {@code participantes} son quienes participan o están invitados (el creador primero).</li>
 * </ul>
 */
public record CaminoResponse(
        Integer codigoMeta,
        String nombre,
        TipoMeta tipo,
        Plazo plazo,
        EstadoMeta estado,
        LocalDate fechaCreacion,
        LocalDate fechaCumplida,
        LocalDate hoy,
        boolean soloLectura,
        boolean puedeEditar,
        AvanceResponse avance,
        Integer constancia7,
        Integer constancia30,
        int cumplidasHoy,
        int tocanHoy,
        int diasSeguidosCompletos,
        long totalRegistros,
        List<SubMetaResponse> hitos,
        List<SubMetaCaminoResponse> habitos,
        List<ParticipanteResponse> participantes
) {}
