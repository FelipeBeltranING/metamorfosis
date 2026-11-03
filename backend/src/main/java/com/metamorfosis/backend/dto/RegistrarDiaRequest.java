package com.metamorfosis.backend.dto;

import java.math.BigDecimal;

/** Valor del día: en Sí/No 1 = hecho y 0 = no hecho; en medible, la cantidad (≥ 0). */
public record RegistrarDiaRequest(BigDecimal valor) {}
