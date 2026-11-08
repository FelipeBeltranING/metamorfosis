package com.metamorfosis.backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    /** HU19: el participante fijó la meta en su perfil. */
    @Column(nullable = false)
    private boolean destacada = false;

    /** HU13: quién invitó, cuándo, y el SHA-256 del token del correo con su vencimiento. */
    @Column(name = "codigo_invitador")
    private Integer codigoInvitador;

    @Column(name = "fecha_invitacion")
    private LocalDateTime fechaInvitacion;

    @Column(name = "token_hash", length = 64)
    private String tokenHash;

    @Column(name = "token_expira")
    private LocalDateTime tokenExpira;

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
        if (oculta) this.destacada = false; // HU19: una meta oculta no se puede destacar
    }

    /** HU19: fija (true) o quita (false) la meta de las destacadas de su perfil. */
    public void cambiarDestacada(boolean destacada) {
        this.destacada = destacada;
    }

    public boolean isDestacada() { return destacada; }

    /** HU13: queda invitada (PENDIENTE) con un token nuevo; también sirve para volver a invitar a quien rechazó. */
    public void invitar(Integer codigoInvitador, String tokenHash, LocalDateTime ahora, LocalDateTime expira) {
        this.estadoInvitacion = EstadoInvitacion.PENDIENTE;
        this.codigoInvitador = codigoInvitador;
        this.fechaInvitacion = ahora;
        this.tokenHash = tokenHash;
        this.tokenExpira = expira;
    }

    /** HU14: acepta (ACEPTADA) o rechaza (RECHAZADA). El enlace del correo deja de servir. */
    public void responder(boolean aceptar) {
        this.estadoInvitacion = aceptar ? EstadoInvitacion.ACEPTADA : EstadoInvitacion.RECHAZADA;
        this.tokenHash = null;
        this.tokenExpira = null;
    }

    public Meta getMeta() { return meta; }
    public Integer getCodigoInvitador() { return codigoInvitador; }
    public LocalDateTime getFechaInvitacion() { return fechaInvitacion; }
    public LocalDateTime getTokenExpira() { return tokenExpira; }
    public Usuario getUsuario() { return usuario; }
    public EstadoInvitacion getEstadoInvitacion() { return estadoInvitacion; }
    public LocalDate getFechaCumplida() { return fechaCumplida; }
    public boolean isOculta() { return oculta; }
}