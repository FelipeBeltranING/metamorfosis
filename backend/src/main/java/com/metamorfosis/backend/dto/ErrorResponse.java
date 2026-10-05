package com.metamorfosis.backend.dto;

import java.util.List;

public record ErrorResponse(int estado, String mensaje, List<String> errores) { }