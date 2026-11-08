package com.metamorfosis.backend.dto;

import com.metamorfosis.backend.dto.PerfilResponse.InsigniaObtenida;
import com.metamorfosis.backend.dto.PerfilResponse.ResumenMetas;

import java.time.LocalDate;
import java.util.List;

/**
 * HU24: perfil de otra persona. Los mismos bloques de Mi perfil (HU22) sin el correo; el resumen
 * y las últimas cumplidas cuentan solo las metas que el dueño no ocultó (HU25).
 * HU26: con privado true solo van los datos de la cabecera; el resumen es null y las listas vienen vacías.
 * HU19: personalizacion; de un perfil privado solo la frase y la portada (que son de la cabecera).
 */
public record PerfilPublicoResponse(Integer codigoUsuario, String nombre, String apellido, String foto,
                                    LocalDate fechaRegistro, ResumenMetas resumen,
                                    List<InsigniaObtenida> insignias, long totalInsignias,
                                    List<MetaResponse> ultimasCumplidas, boolean privado,
                                    PersonalizacionPerfil personalizacion) { }
