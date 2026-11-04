package com.metamorfosis.backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/** HU11: clase de asociación entre Usuario e Insignia, con la fecha en que se ganó. */
@Entity
@Table(name = "usuario_insignia")
public class UsuarioInsignia {

    @EmbeddedId
    private UsuarioInsigniaId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("codigoUsuario")
    @JoinColumn(name = "codigo_usuario")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("codigoInsignia")
    @JoinColumn(name = "codigo_insignia")
    private Insignia insignia;

    @Column(name = "fecha_obtenida", nullable = false)
    private LocalDate fechaObtenida;

    protected UsuarioInsignia() { }

    public UsuarioInsignia(Usuario usuario, Insignia insignia, LocalDate fechaObtenida) {
        this.id = new UsuarioInsigniaId(usuario.getCodigoUsuario(), insignia.getCodigoInsignia());
        this.usuario = usuario;
        this.insignia = insignia;
        this.fechaObtenida = fechaObtenida;
    }

    public UsuarioInsigniaId getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Insignia getInsignia() { return insignia; }
    public LocalDate getFechaObtenida() { return fechaObtenida; }
}
