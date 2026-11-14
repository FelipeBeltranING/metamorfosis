package com.metamorfosis.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Un paso del camino hacia una meta (HU28). Hay dos clases de pasos:
 * <ul>
 *   <li><b>Hábito</b> (HU5): Sí/No ("¿lo hiciste?") o medible ("¿cuánto?", con unidad, objetivo y
 *       tipo de objetivo). Se registra por día y solo toca los días marcados en {@code dias}.</li>
 *   <li><b>Hito</b>: un paso que se logra una sola vez. Los hitos logrados sobre el total son el
 *       avance de la meta (HU29). En una meta compartida el hito es del equipo.</li>
 * </ul>
 */
@Entity
@Table(name = "sub_meta")
public class SubMeta {

    /** Todos los días: lunes (bit 0) a domingo (bit 6). */
    public static final int TODOS_LOS_DIAS = 127;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codigo_sub_meta")
    private Integer codigoSubMeta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codigo_meta", nullable = false)
    private Meta meta;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 200)
    private String pregunta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoSubMeta tipo;

    @Column(length = 30)
    private String unidad;

    @Column(precision = 12, scale = 2)
    private BigDecimal objetivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_objetivo", length = 12)
    private TipoObjetivo tipoObjetivo;

    @Column(nullable = false)
    private short dias = TODOS_LOS_DIAS;

    @Column(length = 500)
    private String notas;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDate fechaCreacion;

    /** Hito: el día en que se logró (null = pendiente). */
    @Column(name = "fecha_logrado")
    private LocalDate fechaLogrado;

    /** Hito: el participante que lo marcó como logrado. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_usuario_logro")
    private Usuario logradoPor;

    protected SubMeta() { }

    public SubMeta(Meta meta, TipoSubMeta tipo, LocalDate fechaCreacion) {
        this.meta = meta;
        this.tipo = tipo;
        this.fechaCreacion = fechaCreacion;
    }

    /**
     * Datos editables. En Sí/No, unidad, objetivo y tipo de objetivo quedan vacíos. En un hito
     * tampoco hay pregunta ni días (se guarda "todos los días" porque la columna no admite vacío).
     */
    public void actualizar(String nombre, String pregunta, String unidad, BigDecimal objetivo,
                           TipoObjetivo tipoObjetivo, int dias, String notas) {
        this.nombre = nombre;
        boolean medible = tipo == TipoSubMeta.MEDIBLE;
        this.pregunta = esHito() ? null : pregunta;
        this.unidad = medible ? unidad : null;
        this.objetivo = medible ? objetivo : null;
        this.tipoObjetivo = medible ? tipoObjetivo : null;
        this.dias = (short) (esHito() ? TODOS_LOS_DIAS : dias);
        this.notas = notas;
    }

    public boolean esHito() {
        return tipo == TipoSubMeta.HITO;
    }

    public boolean esHabito() {
        return !esHito();
    }

    public boolean isLogrado() {
        return fechaLogrado != null;
    }

    /** HU5: marca el hito como logrado ese día por ese participante. */
    public void lograr(Usuario participante, LocalDate fecha) {
        this.fechaLogrado = fecha;
        this.logradoPor = participante;
    }

    /** HU5: el hito vuelve a quedar pendiente (por si se marcó por error). */
    public void deshacerLogro() {
        this.fechaLogrado = null;
        this.logradoPor = null;
    }

    /** ¿Toca registrar este hábito ese día de la semana? Un hito nunca se registra por días. */
    public boolean tocaEl(LocalDate fecha) {
        return esHabito() && (dias & bitDe(fecha.getDayOfWeek())) != 0;
    }

    /** ¿Ese valor cumple la sub-meta? Sí/No: 1 = hecho. Medible: según el tipo de objetivo. */
    public boolean cumple(BigDecimal valor) {
        if (valor == null) return false;
        if (tipo == TipoSubMeta.SI_NO) return valor.compareTo(BigDecimal.ONE) >= 0;
        int comparacion = valor.compareTo(objetivo);
        return tipoObjetivo == TipoObjetivo.AL_MENOS ? comparacion >= 0 : comparacion <= 0;
    }

    /** Días como números ISO (1 = lunes … 7 = domingo). */
    public List<Integer> getDiasSemana() {
        List<Integer> lista = new ArrayList<>();
        for (DayOfWeek d : DayOfWeek.values()) {
            if ((dias & bitDe(d)) != 0) lista.add(d.getValue());
        }
        return lista;
    }

    public static int bitDe(DayOfWeek dia) {
        return 1 << (dia.getValue() - 1);
    }

    public Integer getCodigoSubMeta() { return codigoSubMeta; }
    public Meta getMeta() { return meta; }
    public String getNombre() { return nombre; }
    public String getPregunta() { return pregunta; }
    public TipoSubMeta getTipo() { return tipo; }
    public String getUnidad() { return unidad; }
    public BigDecimal getObjetivo() { return objetivo; }
    public TipoObjetivo getTipoObjetivo() { return tipoObjetivo; }
    public int getDias() { return dias; }
    public String getNotas() { return notas; }
    public LocalDate getFechaCreacion() { return fechaCreacion; }
    public LocalDate getFechaLogrado() { return fechaLogrado; }
    public Usuario getLogradoPor() { return logradoPor; }
}
