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

    /** HU25: el participante ocultó la meta de su perfil. */
    @Column(nullable = false)
    private boolean oculta = false;

    protected Participacion() { }

    public Participacion(Usuario usuario, Meta meta, EstadoInvitacion estadoInvitacion) {
        this.usuario = usuario;
        this.meta = meta;
        this.estadoInvitacion = estadoInvitacion;
    }

    /** HU6: guarda el día en que el usuario cumplió la meta. */
    public void marcarCumplida(LocalDate fecha) {
        this.fechaCumplida = fecha;
    }

    /** HU25: oculta (true) o vuelve a mostrar (false) la meta en el perfil de este participante. */
    public void cambiarVisibilidad(boolean oculta) {
        this.oculta = oculta;
    }

    public Usuario getUsuario() { return usuario; }
    public EstadoInvitacion getEstadoInvitacion() { return estadoInvitacion; }
    public LocalDate getFechaCumplida() { return fechaCumplida; }
    public boolean isOculta() { return oculta; }
}