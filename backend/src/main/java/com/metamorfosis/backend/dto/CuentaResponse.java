package com.metamorfosis.backend.dto;

import com.metamorfosis.backend.model.Usuario;

/**
 * HU-23: datos editables de la cuenta del usuario autenticado.
 * {@code foto} es la imagen en data URL ("data:image/jpeg;base64,..."), o null si no tiene.
 */
public record CuentaResponse(
        Integer codigoUsuario,
        String nombre,
        String apellido,
        String email,
        String foto
) {

    public static CuentaResponse desde(Usuario usuario) {
        return new CuentaResponse(
                usuario.getCodigoUsuario(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getFotoPerfil());
    }
}
