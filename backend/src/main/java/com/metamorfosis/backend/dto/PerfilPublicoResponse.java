package com.metamorfosis.backend.dto;

import com.metamorfosis.backend.dto.PerfilResponse.InsigniaObtenida;
import com.metamorfosis.backend.dto.PerfilResponse.ResumenMetas;

import java.time.LocalDate;
import java.util.List;

/**
 * HU24: perfil de otra persona. Los mismos bloques de Mi perfil (HU22) sin el correo; el resumen
 * y las últimas cumplidas cuentan solo las metas que el dueño no ocultó (HU25).
 */
public record PerfilPublicoResponse(Integer codigoUsuario, String nombre, String apellido, String foto,
                                    LocalDate fechaRegistro, ResumenMetas resumen,
                                    List<InsigniaObtenida> insignias, long totalInsignias,
                                    List<MetaResponse> ultimasCumplidas) { }
