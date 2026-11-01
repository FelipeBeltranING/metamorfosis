package com.metamorfosis.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** HU9: intentos fallidos de inicio de sesión por email y hasta cuándo está bloqueado. */
@Entity
@Table(name = "intento_login")
public class IntentoLogin {

    @Id
    @Column(length = 255)
    private String email;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    protected IntentoLogin() {}

    public IntentoLogin(String email) {
        this.email = email;
    }

    public String getEmail() { return email; }
    public int getIntentos() { return intentos; }
    public void setIntentos(int intentos) { this.intentos = intentos; }
    public LocalDateTime getBloqueadoHasta() { return bloqueadoHasta; }
    public void setBloqueadoHasta(LocalDateTime bloqueadoHasta) { this.bloqueadoHasta = bloqueadoHasta; }
}
