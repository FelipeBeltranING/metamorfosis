package com.metamorfosis.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Tokens para enlaces de correo (igual que HU8): aleatorios, y en la base solo su SHA-256. */
public final class TokensSeguros {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private TokensSeguros() {}

    /** 32 bytes aleatorios en Base64 URL (43 caracteres): imposible de adivinar. */
    public static String generar() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
