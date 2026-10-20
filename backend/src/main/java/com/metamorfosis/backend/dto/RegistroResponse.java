package com.metamorfosis.backend.dto;

import com.metamorfosis.backend.model.Registro;
import java.time.LocalDate;

/** HU5: respuesta pública de un registro de cumplimiento (no se expone la entidad). */
public record RegistroResponse(Integer codigoRegistro, Integer codigoMeta, Integer codigoUsuario,
                               LocalDate fechaFinalizacion, boolean cumplimiento) {

    public static RegistroResponse desde(Registro r) {
        return new RegistroResponse(r.getCodigoRegistro(), r.getMeta().getCodigoMeta(),
                r.getUsuario().getCodigoUsuario(), r.getFechaRegistro(), r.isCumplimiento());
    }
}
