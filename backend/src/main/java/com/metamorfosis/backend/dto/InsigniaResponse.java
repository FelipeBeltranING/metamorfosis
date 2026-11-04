package com.metamorfosis.backend.dto;

import com.metamorfosis.backend.model.Insignia;

/** HU11: insignia que se muestra en el aviso (icono = nombre del ícono de lucide-react). */
public record InsigniaResponse(Integer codigoInsignia, String nombre, String descripcion, String icono) {

    public static InsigniaResponse desde(Insignia i) {
        return new InsigniaResponse(i.getCodigoInsignia(), i.getNombre(), i.getDescripcion(), i.getIcono());
    }
}
