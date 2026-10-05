package com.metamorfosis.backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meta")
public class Meta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codigo_meta")
    private Integer codigoMeta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codigo_creador", nullable = false)
    private Usuario creador;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Plazo plazo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMeta tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoMeta estado = EstadoMeta.ACTIVA;

    @Column(nullable = false)
    private boolean oculta = false;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDate fechaCreacion;

    @OneToMany(mappedBy = "meta", cascade = CascadeType.ALL)
    private List<Participacion> participaciones = new ArrayList<>();

    @OneToMany(mappedBy = "meta", cascade = CascadeType.ALL)
    private List<Registro> registros = new ArrayList<>();

    protected Meta() { }

    public Meta(String nombre, Plazo plazo, TipoMeta tipo) {
        this.nombre = nombre;
        this.plazo = plazo;
        this.tipo = tipo;
    }

    /** Inicializa la meta: estado ACTIVA, fecha de hoy, creador como participante y primer registro. */
    public void crear(Usuario creador) {
        this.creador = creador;
        this.estado = EstadoMeta.ACTIVA;
        this.oculta = false;
        this.fechaCreacion = LocalDate.now();
        this.participaciones.add(new Participacion(creador, this, EstadoInvitacion.ACEPTADA));
        this.registros.add(new Registro(this, creador));
    }

    public Integer getCodigoMeta() { return codigoMeta; }
    public Usuario getCreador() { return creador; }
    public String getNombre() { return nombre; }
    public Plazo getPlazo() { return plazo; }
    public TipoMeta getTipo() { return tipo; }
    public EstadoMeta getEstado() { return estado; }
    public boolean isOculta() { return oculta; }
    public LocalDate getFechaCreacion() { return fechaCreacion; }
}