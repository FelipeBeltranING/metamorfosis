package com.metamorfosis.backend.dto;

import com.metamorfosis.backend.model.Meta;
import com.metamorfosis.backend.model.Participacion;
import com.metamorfosis.backend.model.Plazo;
import com.metamorfosis.backend.model.TipoMeta;
import com.metamorfosis.backend.model.Usuario;

import java.time.LocalDateTime;

/**
 * HU14: una invitación pendiente del usuario: la meta, quién lo invitó (la creadora) y cuántas
 * personas ya participan. No lleva correos.
 */
public record InvitacionResponse(Integer codigoMeta, String nombreMeta, TipoMeta tipo, Plazo plazo,
                                 Persona invitador, int participantes, LocalDateTime fechaInvitacion) {

    public record Persona(Integer codigoUsuario, String nombre, String apellido, String foto) {
        static Persona desde(Usuario u) {
            return new Persona(u.getCodigoUsuario(), u.getNombre(), u.getApellido(), u.getFotoPerfil());
        }
    }

    public static InvitacionResponse desde(Participacion p) {
        Meta m = p.getMeta();
        int aceptados = (int) m.participantesVisibles().stream()
                .filter(x -> x.getEstadoInvitacion() == com.metamorfosis.backend.model.EstadoInvitacion.ACEPTADA)
                .count();
        return new InvitacionResponse(m.getCodigoMeta(), m.getNombre(), m.getTipo(), m.getPlazo(),
                Persona.desde(m.getCreador()), aceptados, p.getFechaInvitacion());
    }
}
