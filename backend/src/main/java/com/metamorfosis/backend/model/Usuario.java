package com.metamorfosis.backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "codigo_usuario")
    private Integer codigoUsuario;

    @Column(nullable = false, length = 255)
    private String nombre;

    @Column(nullable = false, length = 255)
    private String apellido;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "foto_perfil", length = 255)
    private String fotoPerfil;

    @Column(name = "perfil_privado", nullable = false)
    private Boolean perfilPrivado = false;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDate fechaRegistro;

    public Usuario() {}

    public Usuario(String nombre, String apellido, String email, String passwordHash) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    @PrePersist
    protected void onCreate() {
        this.fechaRegistro = LocalDate.now();
        if (this.perfilPrivado == null) {
            this.perfilPrivado = false;
        }
    }

    public Integer getCodigoUsuario() { return codigoUsuario; }
    public void setCodigoUsuario(Integer codigoUsuario) { this.codigoUsuario = codigoUsuario; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getFotoPerfil() { return fotoPerfil; }
    public void setFotoPerfil(String fotoPerfil) { this.fotoPerfil = fotoPerfil; }
    public Boolean getPerfilPrivado() { return perfilPrivado; }
    public void setPerfilPrivado(Boolean perfilPrivado) { this.perfilPrivado = perfilPrivado; }
    public LocalDate getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDate fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}