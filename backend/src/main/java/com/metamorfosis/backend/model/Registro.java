package com.metamorfosis.backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "registro")
public class Registro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codigo_registro")
    private Integer codigoRegistro;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codigo_meta", nullable = false)
    private Meta meta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codigo_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDate fechaRegistro = LocalDate.now();

    @Column(nullable = false)
    private boolean cumplimiento = false;

    protected Registro() { }

    public Registro(Meta meta, Usuario usuario) {
        this.meta = meta;
        this.usuario = usuario;
    }

    public Integer getCodigoRegistro() { return codigoRegistro; }
    public boolean isCumplimiento() { return cumplimiento; }
}