package com.metamorfosis.backend.dto;

import com.metamorfosis.backend.model.Usuario;

public record UsuarioResponse(
        Integer codigoUsuario,
        String nombre,
        String apellido,
        String email) {

    public static UsuarioResponse desde(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getCodigoUsuario(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail());
    }
}
