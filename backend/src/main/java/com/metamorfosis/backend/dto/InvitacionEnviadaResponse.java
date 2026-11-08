package com.metamorfosis.backend.dto;

/**
 * HU13: la persona invitada (queda PENDIENTE) y si salió el correo. Si el correo falla la
 * invitación igual queda: la persona la ve en sus invitaciones dentro de la app (HU14).
 */
public record InvitacionEnviadaResponse(ParticipanteResponse participante, boolean correoEnviado) { }
