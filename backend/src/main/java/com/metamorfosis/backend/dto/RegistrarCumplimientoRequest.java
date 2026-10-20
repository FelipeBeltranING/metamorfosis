package com.metamorfosis.backend.dto;

import java.time.LocalDate;

/**
 * HU5: solicitud para registrar el cumplimiento de una meta en una fecha determinada.
 * fechaFinalizacion: la fecha elegida por el usuario para el registro (no necesariamente hoy,
 * pero nunca una fecha futura); indica el día al que corresponde el cumplimiento reportado.
 */
public record RegistrarCumplimientoRequest(LocalDate fechaFinalizacion, Boolean cumplimiento) { }
