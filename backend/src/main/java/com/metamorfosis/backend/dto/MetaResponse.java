package com.metamorfosis.backend.dto;

import com.metamorfosis.backend.model.*;
import java.time.LocalDate;

/**
 * Respuesta pública de una meta (no se expone la entidad).
 * HU6: fechaCumplida es el día en que el creador la marcó como cumplida (null si sigue activa).
 */
public record MetaResponse(Integer codigoMeta, String nombre, Plazo plazo, TipoMeta tipo,
                           EstadoMeta estado, boolean oculta, LocalDate fechaCreacion,
                           LocalDate fechaCumplida) {

    public static MetaResponse desde(Meta m) {
        return new MetaResponse(m.getCodigoMeta(), m.getNombre(), m.getPlazo(), m.getTipo(),
                m.getEstado(), m.isOculta(), m.getFechaCreacion(),
                m.getFechaCumplida(m.getCreador().getCodigoUsuario()));
    }
}
