package com.metamorfosis.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/** HU11: llave compuesta de usuario_insignia (un usuario no puede tener dos veces la misma insignia). */
@Embeddable
public class UsuarioInsigniaId implements Serializable {

    @Column(name = "codigo_usuario")
    private Integer codigoUsuario;

    @Column(name = "codigo_insignia")
    private Integer codigoInsignia;

    protected UsuarioInsigniaId() { }

    public UsuarioInsigniaId(Integer codigoUsuario, Integer codigoInsignia) {
        this.codigoUsuario = codigoUsuario;
        this.codigoInsignia = codigoInsignia;
    }

    public Integer getCodigoUsuario() { return codigoUsuario; }
    public Integer getCodigoInsignia() { return codigoInsignia; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsuarioInsigniaId that)) return false;
        return Objects.equals(codigoUsuario, that.codigoUsuario)
                && Objects.equals(codigoInsignia, that.codigoInsignia);
    }

    @Override
    public int hashCode() { return Objects.hash(codigoUsuario, codigoInsignia); }
}
