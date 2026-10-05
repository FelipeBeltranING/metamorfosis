package com.metamorfosis.backend.dto;

import com.metamorfosis.backend.model.Plazo;
import com.metamorfosis.backend.model.TipoMeta;

public record CrearMetaRequest(String nombre, Plazo plazo, TipoMeta tipo) { }