package com.metamorfosis.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * HU8: pedido de registro para un email. La cuenta solo se crea si el dueño del correo
 * autoriza desde el enlace; si responde "No fui yo", el email queda bloqueado un tiempo.
 */
@Entity
@Table(name = "solicitud_registro")
public class SolicitudRegistro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codigo_solicitud")
    private Integer codigoSolicitud;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "token_hash", unique = true, length = 64)
    private String tokenHash;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(name = "fecha_ultimo_envio", nullable = false)
    private LocalDateTime fechaUltimoEnvio;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    protected SolicitudRegistro() {}

    public SolicitudRegistro(String email) {
        this.email = email;
    }

    /** Deja la solicitud lista con un enlace nuevo (el anterior deja de servir). */
    public void renovar(String tokenHash, LocalDateTime ahora, LocalDateTime fechaExpiracion) {
        this.tokenHash = tokenHash;
        this.fechaUltimoEnvio = ahora;
        this.fechaExpiracion = fechaExpiracion;
        this.bloqueadoHasta = null;
    }

    /** "No fui yo": el enlace deja de servir y no se aceptan solicitudes hasta `hasta`. */
    public void bloquear(LocalDateTime hasta) {
        this.tokenHash = null;
        this.bloqueadoHasta = hasta;
        this.fechaExpiracion = hasta; // la limpieza la borra cuando termine el bloqueo
    }

    public boolean estaBloqueada(LocalDateTime ahora) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
    }

    public boolean estaVigente(LocalDateTime ahora) {
        return tokenHash != null && fechaExpiracion.isAfter(ahora);
    }

    public Integer getCodigoSolicitud() { return codigoSolicitud; }
    public String getEmail() { return email; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getFechaExpiracion() { return fechaExpiracion; }
    public LocalDateTime getFechaUltimoEnvio() { return fechaUltimoEnvio; }
    public LocalDateTime getBloqueadoHasta() { return bloqueadoHasta; }
}
