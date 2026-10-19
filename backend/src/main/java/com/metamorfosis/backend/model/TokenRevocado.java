package com.metamorfosis.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Token JWT cerrado con el logout. Mientras no expire, el sistema debe rechazarlo. */
@Entity
@Table(name = "token_revocado")
public class TokenRevocado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codigo_token")
    private Integer codigoToken;

    @Column(nullable = false, unique = true, length = 64)
    private String jti;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    protected TokenRevocado() { }

    public TokenRevocado(String jti, LocalDateTime fechaExpiracion) {
        this.jti = jti;
        this.fechaExpiracion = fechaExpiracion;
    }

    public Integer getCodigoToken() { return codigoToken; }
    public String getJti() { return jti; }
    public LocalDateTime getFechaExpiracion() { return fechaExpiracion; }
}