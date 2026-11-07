package com.metamorfosis.backend.dto;

import com.metamorfosis.backend.model.Usuario;

/** HU24: una persona en los resultados de la búsqueda. No lleva el correo. */
public record UsuarioResumenResponse(Integer codigoUsuario, String nombre, String apellido, String foto) {

    public static UsuarioResumenResponse desde(Usuario u) {
        return new UsuarioResumenResponse(u.getCodigoUsuario(), u.getNombre(), u.getApellido(), u.getFotoPerfil());
    }
}
