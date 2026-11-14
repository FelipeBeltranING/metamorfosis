package com.metamorfosis.backend.dto;

import com.metamorfosis.backend.dto.PerfilResponse.InsigniaObtenida;
import com.metamorfosis.backend.model.Portada;

import java.util.List;

/**
 * HU19: lo que el usuario eligió mostrar en su perfil: frase, portada, insignia destacada (o null)
 * y metas destacadas (máximo tres, sin participantes). HU20: el avatar con el que se muestra.
 */
public record PersonalizacionPerfil(String frase, Portada portada, InsigniaObtenida insigniaDestacada,
                                    List<MetaResponse> metasDestacadas, AvatarPerfil avatar) {

    /** Perfil privado (HU26): solo la cabecera (frase, portada y avatar). */
    public static PersonalizacionPerfil soloCabecera(String frase, Portada portada, AvatarPerfil avatar) {
        return new PersonalizacionPerfil(frase, portada, null, List.of(), avatar);
    }
}
