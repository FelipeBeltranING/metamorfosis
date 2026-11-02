package com.metamorfosis.backend.dto;

/** HU8: en qué va la solicitud que espera la página donde se pidió el registro. */
public record EstadoSolicitudResponse(Estado estado) {

    public enum Estado { PENDIENTE, AUTORIZADA, RECHAZADA, VENCIDA }
}
