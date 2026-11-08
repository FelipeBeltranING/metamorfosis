package com.metamorfosis.backend.dto;

import java.util.List;

/**
 * HU19: lo que los demás ven del perfil. {@code frase}: hasta 100 caracteres (vacía la quita).
 * {@code portada}: un valor del enum Portada. {@code codigoInsigniaDestacada}: una insignia ganada
 * o null. {@code metasDestacadas}: hasta 3 metas propias visibles en el perfil.
 */
public record PerfilPublicoRequest(String frase, String portada, Integer codigoInsigniaDestacada,
                                   List<Integer> metasDestacadas) { }
