package com.metamorfosis.backend.dto;

import com.metamorfosis.backend.model.*;
import java.time.LocalDate;

/** Respuesta pública de una meta (no se expone la entidad). */
public record MetaResponse(Integer codigoMeta, String nombre, Plazo plazo, TipoMeta tipo,
                           EstadoMeta estado, boolean oculta, LocalDate fechaCreacion,
                           boolean cumplidaHoy) {

    public static MetaResponse desde(Meta m) {
        return desde(m, false);
    }

    /** HU5: cumplidaHoy indica si el usuario que consulta ya registró el cumplimiento de hoy. */
    public static MetaResponse desde(Meta m, boolean cumplidaHoy) {
        return new MetaResponse(m.getCodigoMeta(), m.getNombre(), m.getPlazo(), m.getTipo(),
                m.getEstado(), m.isOculta(), m.getFechaCreacion(), cumplidaHoy);
    }
}