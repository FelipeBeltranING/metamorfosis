package com.metamorfosis.backend.dto;

import com.metamorfosis.backend.model.CriterioInsignia;
import com.metamorfosis.backend.model.Insignia;
import com.metamorfosis.backend.model.Plazo;

import java.time.LocalDate;
import java.util.List;

/**
 * HU12: galería de insignias. Trae el catálogo completo en su orden; ganadas y total
 * alimentan el resumen "Has ganado N de 6 insignias".
 */
public record GaleriaInsigniasResponse(int ganadas, int total, List<InsigniaGaleria> insignias) {

    /**
     * Una insignia del catálogo vista por el usuario. avance va de 0 a cantidadRequerida;
     * fechaObtenida es null mientras la insignia esté pendiente.
     */
    public record InsigniaGaleria(Integer codigoInsignia, String nombre, String descripcion, String icono,
                                  CriterioInsignia criterio, Plazo plazoRequerido, int cantidadRequerida,
                                  int avance, boolean obtenida, LocalDate fechaObtenida) {

        /** Una insignia ganada se muestra completa aunque el conteo actual sea menor (metas borradas, HU4). */
        public static InsigniaGaleria de(Insignia insignia, LocalDate fechaObtenida, int avance) {
            boolean obtenida = fechaObtenida != null;
            return new InsigniaGaleria(insignia.getCodigoInsignia(), insignia.getNombre(), insignia.getDescripcion(),
                    insignia.getIcono(), insignia.getCriterio(), insignia.getPlazoRequerido(),
                    insignia.getCantidadRequerida(), obtenida ? insignia.getCantidadRequerida() : avance,
                    obtenida, fechaObtenida);
        }
    }
}
