package com.metamorfosis.backend.service;

import com.metamorfosis.backend.exception.TokenInvalidoException;
import com.metamorfosis.backend.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final int MIN_BYTES_SECRETO = 32; // HS256 exige al menos 256 bits
    private static final String MENSAJE_INVALIDO = "Token inválido o expirado";

    private final SecretKey clave;
    private final Duration expiracion;

    public JwtService(@Value("${app.jwt.secret}") String secreto,
                      @Value("${app.jwt.expiracion-minutos:60}") long expiracionMinutos) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_BYTES_SECRETO) {
            throw new IllegalStateException("app.jwt.secret debe tener al menos 32 caracteres");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
        this.expiracion = Duration.ofMinutes(expiracionMinutos);
    }

    /**
     * Genera un JWT firmado cuyo subject es el código del usuario. Lleva un identificador único
     * (jti) para poder revocarlo al cerrar sesión. No incluye datos sensibles.
     */
    public String generarToken(Usuario usuario) {
        Date ahora = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(usuario.getCodigoUsuario()))
                .claim("email", usuario.getEmail())
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expiracion.toMillis()))
                .signWith(clave)
                .compact();
    }

    /** jti del token (HU10). Lanza TokenInvalidoException si el token no es válido. */
    public String extraerId(String token) {
        String jti = parsear(token).getId();
        if (jti == null || jti.isBlank()) {
            throw new TokenInvalidoException(MENSAJE_INVALIDO);
        }
        return jti;
    }

    /** Fecha de expiración del token en UTC (HU10). */
    public LocalDateTime extraerExpiracion(String token) {
        Date expiracionToken = parsear(token).getExpiration();
        if (expiracionToken == null) {
            throw new TokenInvalidoException(MENSAJE_INVALIDO);
        }
        return LocalDateTime.ofInstant(expiracionToken.toInstant(), ZoneOffset.UTC);
    }

    /** Código del usuario dueño del token (HU10). */
    public Integer extraerCodigoUsuario(String token) {
        try {
            return Integer.valueOf(parsear(token).getSubject());
        } catch (NumberFormatException e) {
            throw new TokenInvalidoException(MENSAJE_INVALIDO);
        }
    }

    /** Verifica firma y expiración; cualquier problema se traduce a TokenInvalidoException. */
    private Claims parsear(String token) {
        try {
            return Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new TokenInvalidoException(MENSAJE_INVALIDO);
        }
    }
}
