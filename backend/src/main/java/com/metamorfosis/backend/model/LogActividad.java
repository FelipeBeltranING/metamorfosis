package com.metamorfosis.backend.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/** Registro de auditoría. Es de solo escritura: sin setters y la base bloquea UPDATE/DELETE. */
@Entity
@Table(name = "log_actividad")
public class LogActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codigo_log")
    private Integer codigoLog;

    @Column(name = "fecha_hora", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime fechaHora;

    @Column(name = "codigo_usuario")
    private Integer codigoUsuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccionLog accion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EntidadLog entidad;

    @Column(name = "codigo_entidad")
    private Integer codigoEntidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ResultadoLog resultado;

    @Column(columnDefinition = "text")
    private String detalle;

    protected LogActividad() { }

    public LogActividad(Integer codigoUsuario, AccionLog accion, EntidadLog entidad,
                        Integer codigoEntidad, ResultadoLog resultado, String detalle) {
        this.fechaHora = OffsetDateTime.now();
        this.codigoUsuario = codigoUsuario;
        this.accion = accion;
        this.entidad = entidad;
        this.codigoEntidad = codigoEntidad;
        this.resultado = resultado;
        this.detalle = detalle;
    }

    public Integer getCodigoLog() { return codigoLog; }
}
