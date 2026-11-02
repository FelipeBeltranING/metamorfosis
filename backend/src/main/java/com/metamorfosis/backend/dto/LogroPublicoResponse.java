package com.metamorfosis.backend.dto;

import com.metamorfosis.backend.model.Plazo;
import com.metamorfosis.backend.model.TipoMeta;

import java.time.LocalDate;

/**
 * HU21: lo que ve quien abre el enlace. Solo la meta compartida y el nombre visible
 * ("Laura G."): nada del perfil, del correo ni de las demás metas.
 */
public record LogroPublicoResponse(String nombreMeta, TipoMeta tipo, Plazo plazo,
                                   LocalDate fechaCumplida, String nombreVisible) { }
