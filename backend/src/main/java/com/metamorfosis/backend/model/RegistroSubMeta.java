package com.metamorfosis.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Lo que un participante registró de una sub-meta en un día: 1/0 en Sí/No, la cantidad en medible. */
@Entity
@Table(name = "registro_sub_meta",
        uniqueConstraints = @UniqueConstraint(name = "uq_registro_sub_meta_dia",
                columnNames = {"codigo_sub_meta", "codigo_usuario", "fecha"}))
public class RegistroSubMeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codigo_registro")
    private Integer codigoRegistro;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codigo_sub_meta", nullable = false)
    private SubMeta subMeta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codigo_usuario", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    protected RegistroSubMeta() { }

    public RegistroSubMeta(SubMeta subMeta, Usuario usuario, LocalDate fecha, BigDecimal valor) {
        this.subMeta = subMeta;
        this.usuario = usuario;
        this.fecha = fecha;
        this.valor = valor;
    }

    public void cambiarValor(BigDecimal valor) {
        this.valor = valor;
    }

    public Integer getCodigoRegistro() { return codigoRegistro; }
    public SubMeta getSubMeta() { return subMeta; }
    public Usuario getUsuario() { return usuario; }
    public LocalDate getFecha() { return fecha; }
    public BigDecimal getValor() { return valor; }
}
