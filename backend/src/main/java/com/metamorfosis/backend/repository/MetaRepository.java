package com.metamorfosis.backend.repository;

import com.metamorfosis.backend.model.Meta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MetaRepository extends JpaRepository<Meta, Integer> {

    /**
     * Metas ACTIVAS en las que el usuario participa con invitación ACEPTADA
     * (las que creó y las compartidas que aceptó), de la más reciente a la más antigua.
     */
    @Query("""
            select m from Meta m
            join m.participaciones p
            where p.usuario.codigoUsuario = :codigoUsuario
              and p.estadoInvitacion = com.metamorfosis.backend.model.EstadoInvitacion.ACEPTADA
              and m.estado = com.metamorfosis.backend.model.EstadoMeta.ACTIVA
            order by m.fechaCreacion desc, m.codigoMeta desc
            """)
    List<Meta> findActivasByParticipante(@Param("codigoUsuario") Integer codigoUsuario);

    /** HU5: true si el usuario participa en la meta con invitación ACEPTADA (puede registrar cumplimiento). */
    @Query("""
            select case when count(p) > 0 then true else false end
            from Participacion p
            where p.meta.codigoMeta = :codigoMeta
              and p.usuario.codigoUsuario = :codigoUsuario
              and p.estadoInvitacion = com.metamorfosis.backend.model.EstadoInvitacion.ACEPTADA
            """)
    boolean esParticipanteAceptado(@Param("codigoMeta") Integer codigoMeta,
                                    @Param("codigoUsuario") Integer codigoUsuario);
}