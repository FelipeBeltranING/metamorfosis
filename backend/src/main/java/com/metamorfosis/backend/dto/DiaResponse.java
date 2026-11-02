package com.metamorfosis.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Un día de una sub-meta para un usuario. {@code toca}: ese día de la semana está en la sub-meta.
 * {@code valor} es null si no registró nada; {@code cumplido} solo es true con un valor que cumple.
 */
public record DiaResponse(LocalDate fecha, boolean toca, BigDecimal valor, boolean cumplido) {}
