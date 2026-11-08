package com.metamorfosis.backend.dto;

/** HU14: {"aceptar": true} acepta la invitación; {"aceptar": false} la rechaza. */
public record ResponderInvitacionRequest(Boolean aceptar) { }
