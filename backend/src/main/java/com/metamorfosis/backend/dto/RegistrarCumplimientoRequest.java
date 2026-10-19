package com.metamorfosis.backend.dto;

import java.time.LocalDate;

/** HU5: solicitud para registrar el cumplimiento de una meta en una fecha determinada. */
public record RegistrarCumplimientoRequest(LocalDate fecha, Boolean cumplimiento) { }
