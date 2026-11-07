package com.metamorfosis.backend.dto;

import com.metamorfosis.backend.model.*;
import java.time.LocalDate;

/**
 * Respuesta pública de una meta (no se expone la entidad).
 * HU6: fechaCumplida es el día en que el creador la marcó como cumplida (null si sigue activa).
 * HU25: oculta dice si la meta está oculta en el perfil del usuario que la consulta.
 */
public record MetaResponse(Integer codigoMeta, String nombre, Plazo plazo, TipoMeta tipo,
                           EstadoMeta estado, boolean oculta, LocalDate fechaCreacion,
                           LocalDate fechaCumplida) {

    /** La meta vista por su creador. */
    public static MetaResponse desde(Meta m) {
        return desde(m, m.getCreador().getCodigoUsuario());
    }

    /** HU25: la meta vista por un participante; oculta es la de su propia participación. */
    public static MetaResponse desde(Meta m, Integer codigoUsuario) {
        return new MetaResponse(m.getCodigoMeta(), m.getNombre(), m.getPlazo(), m.getTipo(),
                m.getEstado(), m.isOcultaPara(codigoUsuario), m.getFechaCreacion(),
                m.getFechaCumplida(m.getCreador().getCodigoUsuario()));
    }
}
