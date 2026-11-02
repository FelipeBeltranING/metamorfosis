package com.metamorfosis.backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/** HU21: enlace público de un logro. Lo identifica un token aleatorio, no el código de la meta. */
@Entity
@Table(name = "logro_compartido")
public class LogroCompartido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codigo_logro")
    private Integer codigoLogro;

    @Column(nullable = false, unique = true, length = 16)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codigo_meta", nullable = false)
    private Meta meta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codigo_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDate fechaCreacion;

    protected LogroCompartido() { }

    public LogroCompartido(Meta meta, Usuario usuario, String token) {
        this.meta = meta;
        this.usuario = usuario;
        this.token = token;
        this.fechaCreacion = LocalDate.now();
    }

    public Integer getCodigoLogro() { return codigoLogro; }
    public String getToken() { return token; }
    public Meta getMeta() { return meta; }
    public Usuario getUsuario() { return usuario; }
    public LocalDate getFechaCreacion() { return fechaCreacion; }
}
