package com.metamorfosis.backend.dto;

/**
 * HU28: un paso del camino con lo que necesita el formulario para editarlo: el nombre de su meta,
 * si la meta ya está cumplida ({@code soloLectura}) y si quien consulta puede cambiarlo.
 */
public record PasoResponse(SubMetaResponse paso, String nombreMeta, boolean soloLectura, boolean puedeEditar) {}
