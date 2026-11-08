package com.metamorfosis.backend.dto;

import com.metamorfosis.backend.model.EstadoMeta;
import com.metamorfosis.backend.model.Plazo;
import com.metamorfosis.backend.model.TipoMeta;

import java.time.LocalDate;
import java.util.List;

/**
 * "Tu camino" de una meta para el usuario que consulta. Constancias en % (null si aún no hay días
 * que contar). {@code soloLectura}: la meta está cumplida. {@code puedeEditar}: es su creador y está activa.
 * HU13: {@code participantes} son quienes participan o están invitados (el creador primero).
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
        Integer constancia7,
        Integer constancia30,
        int cumplidasHoy,
        int tocanHoy,
        int diasSeguidosCompletos,
        long totalRegistros,
        List<SubMetaCaminoResponse> subMetas,
        List<ParticipanteResponse> participantes
) {}
