package com.metamorfosis.backend.dto;

import com.metamorfosis.backend.model.ColorAcento;
import com.metamorfosis.backend.model.ModoTema;
import com.metamorfosis.backend.model.Usuario;

/**
 * HU-23: datos editables de la cuenta del usuario autenticado.
 * {@code foto} es la imagen en data URL ("data:image/jpeg;base64,..."), o null si no tiene.
 * {@code apariencia} (HU19) es el modo y el color de acento que eligió.
 */
public record CuentaResponse(
        Integer codigoUsuario,
        String nombre,
        String apellido,
        String email,
        String foto,
        Apariencia apariencia
) {

    /** HU19: modo (CLARO, OSCURO o AUTOMATICO) y color de acento. */
    public record Apariencia(ModoTema modo, ColorAcento acento) { }

    public static CuentaResponse desde(Usuario usuario) {
        return new CuentaResponse(
                usuario.getCodigoUsuario(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getFotoPerfil(),
                new Apariencia(usuario.getModoTema(), usuario.getColorAcento()));
    }
}
