package com.metamorfosis.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ParticipacionId implements Serializable {

    @Column(name = "codigo_usuario")
    private Integer codigoUsuario;

    @Column(name = "codigo_meta")
    private Integer codigoMeta;

    protected ParticipacionId() { }

    /** HU14: para buscar la participación de un usuario en una meta. */
    public ParticipacionId(Integer codigoUsuario, Integer codigoMeta) {
        this.codigoUsuario = codigoUsuario;
        this.codigoMeta = codigoMeta;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParticipacionId that)) return false;
        return Objects.equals(codigoUsuario, that.codigoUsuario)
                && Objects.equals(codigoMeta, that.codigoMeta);
    }

    @Override
    public int hashCode() { return Objects.hash(codigoUsuario, codigoMeta); }
}