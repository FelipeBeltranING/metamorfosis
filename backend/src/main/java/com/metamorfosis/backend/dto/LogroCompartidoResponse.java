package com.metamorfosis.backend.dto;

import java.time.LocalDate;

/** HU21: enlace para compartir un logro. url = página pública que abre quien lo recibe. */
public record LogroCompartidoResponse(String token, String url, String nombreMeta, LocalDate fechaCumplida) { }
