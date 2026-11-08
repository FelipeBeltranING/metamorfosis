package com.metamorfosis.backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDate fechaCreacion;

    @OneToMany(mappedBy = "meta", cascade = CascadeType.ALL)
    private List<Participacion> participaciones = new ArrayList<>();

    protected Meta() { }

    public Meta(String nombre, Plazo plazo, TipoMeta tipo) {
        this.nombre = nombre;
        this.plazo = plazo;
        this.tipo = tipo;
    }

    /** Inicializa la meta: estado ACTIVA, fecha de hoy y el creador como participante. */
    public void crear(Usuario creador) {
        this.creador = creador;
        this.estado = EstadoMeta.ACTIVA;
        this.fechaCreacion = LocalDate.now();
        this.participaciones.add(new Participacion(creador, this, EstadoInvitacion.ACEPTADA));
    }

    /** HU2: actualiza los datos editables. No cambia estado, visibilidad (HU25) ni creador. */
    public void editar(String nombre, Plazo plazo, TipoMeta tipo) {
        this.nombre = nombre;
        this.plazo = plazo;
        this.tipo = tipo;
    }

    /** HU6: la meta queda CUMPLIDA (no se puede deshacer) y la fecha se guarda en la participación del usuario. */
    public void marcarCumplida(Integer codigoUsuario, LocalDate fecha) {
        this.estado = EstadoMeta.CUMPLIDA;
        participacionDe(codigoUsuario).ifPresent(p -> p.marcarCumplida(fecha));
    }

    /** HU6: fecha en que el usuario cumplió la meta, o null si no la ha cumplido. */
    public LocalDate getFechaCumplida(Integer codigoUsuario) {
        return participacionDe(codigoUsuario).map(Participacion::getFechaCumplida).orElse(null);
    }

    /** HU25: el usuario participa en la meta con la invitación aceptada (el creador siempre). */
    public boolean participaAceptado(Integer codigoUsuario) {
        return participacionDe(codigoUsuario)
                .filter(p -> p.getEstadoInvitacion() == EstadoInvitacion.ACEPTADA)
                .isPresent();
    }

    /**
     * HU25: oculta o muestra la meta en el perfil del usuario. La marca vive en su participación,
     * así que en una meta compartida no cambia lo que ven los demás participantes.
     */
    public void cambiarVisibilidad(Integer codigoUsuario, boolean oculta) {
        participacionDe(codigoUsuario).ifPresent(p -> p.cambiarVisibilidad(oculta));
    }

    /** HU25: la meta está oculta en el perfil de ese usuario. */
    public boolean isOcultaPara(Integer codigoUsuario) {
        return participacionDe(codigoUsuario).map(Participacion::isOculta).orElse(false);
    }

    /**
     * HU13: quienes participan o están invitados (sin los que rechazaron), el creador primero.
     */
    public List<Participacion> participantesVisibles() {
        return participaciones.stream()
                .filter(p -> p.getEstadoInvitacion() != EstadoInvitacion.RECHAZADA)
                .sorted(Comparator.comparing((Participacion p) -> !p.getUsuario().getCodigoUsuario()
                        .equals(creador.getCodigoUsuario())))
                .toList();
    }

    private Optional<Participacion> participacionDe(Integer codigoUsuario) {
        return participaciones.stream()
                .filter(p -> p.getUsuario().getCodigoUsuario().equals(codigoUsuario))
                .findFirst();
    }

    public Integer getCodigoMeta() { return codigoMeta; }
    public Usuario getCreador() { return creador; }
    public String getNombre() { return nombre; }
    public Plazo getPlazo() { return plazo; }
    public TipoMeta getTipo() { return tipo; }
    public EstadoMeta getEstado() { return estado; }
    public LocalDate getFechaCreacion() { return fechaCreacion; }
}