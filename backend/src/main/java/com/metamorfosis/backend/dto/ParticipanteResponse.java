package com.metamorfosis.backend.dto;

import com.metamorfosis.backend.model.EstadoInvitacion;
import com.metamorfosis.backend.model.Participacion;
import com.metamorfosis.backend.model.Usuario;

/**
 * HU13: una persona en una meta compartida. {@code estado}: ACEPTADA (participa) o PENDIENTE
 * (invitada, aún no responde). {@code creador}: es quien creó la meta. No lleva el correo.
 */
public record ParticipanteResponse(Integer codigoUsuario, String nombre, String apellido, String foto,
                                   EstadoInvitacion estado, boolean creador) {

    public static ParticipanteResponse desde(Participacion p, Usuario creadorMeta) {
        Usuario u = p.getUsuario();
        return new ParticipanteResponse(u.getCodigoUsuario(), u.getNombre(), u.getApellido(), u.getFotoPerfil(),
                p.getEstadoInvitacion(), u.getCodigoUsuario().equals(creadorMeta.getCodigoUsuario()));
    }
}
