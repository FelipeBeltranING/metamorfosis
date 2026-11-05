package com.metamorfosis.backend.model;

import jakarta.persistence.*;

/** HU11: insignia del catálogo. Se carga con la migración V14 y no se edita desde la API. */
@Entity
@Table(name = "insignia")
public class Insignia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codigo_insignia")
    private Integer codigoInsignia;

    @Column(nullable = false, length = 60, unique = true)
    private String nombre;

    @Column(nullable = false, length = 150)
    private String descripcion;

    /** Nombre del ícono de lucide-react que dibuja el frontend (por ejemplo "Flame"). */
    @Column(nullable = false, length = 30)
    private String icono;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private CriterioInsignia criterio;

    @Enumerated(EnumType.STRING)
    @Column(name = "plazo_requerido", length = 10)
    private Plazo plazoRequerido;

    @Column(name = "cantidad_requerida", nullable = false)
    private int cantidadRequerida;

    protected Insignia() { }

    public Insignia(String nombre, String descripcion, String icono, CriterioInsignia criterio,
                    Plazo plazoRequerido, int cantidadRequerida) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.icono = icono;
        this.criterio = criterio;
        this.plazoRequerido = plazoRequerido;
        this.cantidadRequerida = cantidadRequerida;
    }

    public Integer getCodigoInsignia() { return codigoInsignia; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getIcono() { return icono; }
    public CriterioInsignia getCriterio() { return criterio; }
    public Plazo getPlazoRequerido() { return plazoRequerido; }
    public int getCantidadRequerida() { return cantidadRequerida; }
}
