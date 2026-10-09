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

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "foto_perfil", length = 500)
    private String fotoPerfil;

    @Column(name = "perfil_privado", nullable = false)
    private boolean perfilPrivado = false;

    @Column(nullable = false)
    private int puntos = 0;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDate fechaRegistro = LocalDate.now();

    protected Usuario() { }

    public Usuario(String nombre, String apellido, String email, String passwordHash) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.passwordHash = passwordHash;
    }

 
    public Integer getCodigoUsuario() { return codigoUsuario; }
    public String getNombre() { return nombre; }
    public String getApellido() { return apellido; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getFotoPerfil() { return fotoPerfil; }
    public boolean isPerfilPrivado() { return perfilPrivado; }
    public int getPuntos() { return puntos; }
    public LocalDate getFechaRegistro() { return fechaRegistro; }
}