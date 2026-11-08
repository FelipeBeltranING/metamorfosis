package com.metamorfosis.backend.repository;

import com.metamorfosis.backend.model.Participacion;
import com.metamorfosis.backend.model.ParticipacionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParticipacionRepository extends JpaRepository<Participacion, ParticipacionId> {

    /** HU14: la invitación del enlace del correo (se busca por el SHA-256 del token). */
    Optional<Participacion> findByTokenHash(String tokenHash);

    /** HU14: invitaciones pendientes del usuario a metas activas, la más reciente primero. */
    @Query("""
            select p from Participacion p
            join fetch p.meta m
            join fetch m.creador
            where p.usuario.codigoUsuario = :codigoUsuario
              and p.estadoInvitacion = com.metamorfosis.backend.model.EstadoInvitacion.PENDIENTE
              and m.estado = com.metamorfosis.backend.model.EstadoMeta.ACTIVA
            order by p.fechaInvitacion desc
            """)
    List<Participacion> pendientesDe(@Param("codigoUsuario") Integer codigoUsuario);
}
