package com.metamorfosis.backend.dto;

import java.util.List;

/** Un hábito en "Tu camino" (HU5): sus datos y los últimos 7 días, del más reciente al más antiguo. */
public record SubMetaCaminoResponse(SubMetaResponse subMeta, List<DiaResponse> dias, Integer constancia7) {}
