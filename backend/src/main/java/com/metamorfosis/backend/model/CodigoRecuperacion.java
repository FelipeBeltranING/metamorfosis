package com.metamorfosis.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * HU33: código de 6 dígitos para cambiar la contraseña olvidada. Uno por cuenta (la llave es el
 * código del usuario); guarda el código cifrado, cuándo vence (UTC), cuándo se mandó y cuántas veces
 * se escribió mal.
 */
@Entity
@Table(name = "codigo_recuperacion")
public class CodigoRecuperacion {

    @Id
    @Column(name = "codigo_usuario")
    private Integer codigoUsuario;

    @Column(name = "codigo_hash", nullable = false, length = 100)
    private String codigoHash;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @Column(name = "enviado_en", nullable = false)
    private LocalDateTime enviadoEn;

    @Column(nullable = false)
    private int intentos;

    protected CodigoRecuperacion() { }

    public CodigoRecuperacion(Integer codigoUsuario) {
        this.codigoUsuario = codigoUsuario;
    }

    /** Pone un código nuevo (ya cifrado): el anterior deja de servir y los intentos vuelven a cero. */
    public void renovar(String codigoHash, LocalDateTime enviadoEn, LocalDateTime expiraEn) {
        this.codigoHash = codigoHash;
        this.enviadoEn = enviadoEn;
        this.expiraEn = expiraEn;
        this.intentos = 0;
    }

    public boolean estaVigente(LocalDateTime ahora) {
        return expiraEn.isAfter(ahora);
    }

    /** Suma un intento fallido y devuelve cuántos lleva. */
    public int sumarIntento() {
        return ++intentos;
    }

    public Integer getCodigoUsuario() { return codigoUsuario; }
    public String getCodigoHash() { return codigoHash; }
    public LocalDateTime getExpiraEn() { return expiraEn; }
    public LocalDateTime getEnviadoEn() { return enviadoEn; }
    public int getIntentos() { return intentos; }
}
