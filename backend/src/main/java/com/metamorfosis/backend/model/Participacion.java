package com.metamorfosis.backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "participacion")
public class Participacion {

    @EmbeddedId
    private ParticipacionId id = new ParticipacionId();

    @MapsId("codigoUsuario")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codigo_usuario")
    private Usuario usuario;

    @MapsId("codigoMeta")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codigo_meta")
    private Meta meta;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_invitacion", nullable = false, length = 10)
    private EstadoInvitacion estadoInvitacion;

    @Column(name = "fecha_cumplida")
    private LocalDate fechaCumplida;

    protected Participacion() { }

    public Participacion(Usuario usuario, Meta meta, EstadoInvitacion estadoInvitacion) {
        this.usuario = usuario;
        this.meta = meta;
        this.estadoInvitacion = estadoInvitacion;
    }

    public EstadoInvitacion getEstadoInvitacion() { return estadoInvitacion; }
    public LocalDate getFechaCumplida() { return fechaCumplida; }
}