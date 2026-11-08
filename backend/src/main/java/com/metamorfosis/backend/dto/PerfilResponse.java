package com.metamorfosis.backend.dto;

import com.metamorfosis.backend.model.UsuarioInsignia;

import java.time.LocalDate;
import java.util.List;

/**
 * HU22: lo que muestra Mi perfil. Los datos personales, el resumen de metas (el mismo cálculo
 * del reporte, HU7), las insignias ganadas (HU11) y las últimas metas cumplidas (máximo tres).
 * totalInsignias es el tamaño del catálogo, para mostrar "N de 6 insignias".
 * HU26: perfilPrivado es el valor del interruptor Perfil privado.
 * HU19: personalizacion es lo que el usuario eligió mostrar a los demás.
 */
public record PerfilResponse(String nombre, String apellido, String email, LocalDate fechaRegistro,
                             ResumenMetas resumen, List<InsigniaObtenida> insignias,
                             long totalInsignias, List<MetaResponse> ultimasCumplidas,
                             boolean perfilPrivado, PersonalizacionPerfil personalizacion) {

    /** Conteo de las metas en las que el usuario participa con invitación aceptada. */
    public record ResumenMetas(long totalMetas, long metasActivas, long metasCumplidas,
                               int porcentajeCumplimiento) { }

    /** Insignia ganada con la fecha en que se ganó (icono = nombre del ícono de lucide-react). */
    public record InsigniaObtenida(Integer codigoInsignia, String nombre, String descripcion,
                                   String icono, LocalDate fechaObtenida) {

        public static InsigniaObtenida desde(UsuarioInsignia ui) {
            return new InsigniaObtenida(ui.getInsignia().getCodigoInsignia(), ui.getInsignia().getNombre(),
                    ui.getInsignia().getDescripcion(), ui.getInsignia().getIcono(), ui.getFechaObtenida());
        }
    }
}
