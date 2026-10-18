package com.metamorfosis.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "log_actividad")
public class LogActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codigo_log")
    private Integer codigoLog;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "codigo_usuario")
    private Integer codigoUsuario;

    @Column(nullable = false, length = 255)
    private String accion;

    @Column(nullable = false, length = 255)
    private String entidad;

    @Column(name = "codigo_entidad")
    private Integer codigoEntidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 255)
    private ResultadoLog resultado;

    @Column(columnDefinition = "TEXT")
    private String detalle;

    public LogActividad() {}

    @PrePersist
    protected void onCreate() {
        this.fechaHora = LocalDateTime.now();
    }

    public Integer getCodigoLog() { return codigoLog; }
    public void setCodigoLog(Integer codigoLog) { this.codigoLog = codigoLog; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public Integer getCodigoUsuario() { return codigoUsuario; }
    public void setCodigoUsuario(Integer codigoUsuario) { this.codigoUsuario = codigoUsuario; }
    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }
    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }
    public Integer getCodigoEntidad() { return codigoEntidad; }
    public void setCodigoEntidad(Integer codigoEntidad) { this.codigoEntidad = codigoEntidad; }
    public ResultadoLog getResultado() { return resultado; }
    public void setResultado(ResultadoLog resultado) { this.resultado = resultado; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
}