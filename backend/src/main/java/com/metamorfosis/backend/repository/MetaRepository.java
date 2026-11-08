package com.metamorfosis.backend.repository;

import com.metamorfosis.backend.model.Meta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MetaRepository extends JpaRepository<Meta, Integer> {

    /** Meta con ese código para unirse (vigente o no; el servicio revisa el vencimiento). */
    Optional<Meta> findByCodigoUnion(String codigoUnion);

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

    /**
     * HU27: todas las metas (activas y cumplidas) en las que el usuario participa con invitación
     * ACEPTADA, de la más reciente a la más antigua. El servicio aplica los filtros.
     */
    @Query("""
            select m from Meta m
            join m.participaciones p
            where p.usuario.codigoUsuario = :codigoUsuario
              and p.estadoInvitacion = com.metamorfosis.backend.model.EstadoInvitacion.ACEPTADA
            order by m.fechaCreacion desc, m.codigoMeta desc
            """)
    List<Meta> findByParticipante(@Param("codigoUsuario") Integer codigoUsuario);

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

    /**
     * HU11: plazo y tipo de cada meta CUMPLIDA en la que el usuario participa con invitación
     * ACEPTADA. Una fila por meta: [Plazo, TipoMeta]. Con esto se revisan las insignias.
     */
    @Query("""
            select m.plazo, m.tipo from Meta m
            join m.participaciones p
            where p.usuario.codigoUsuario = :codigoUsuario
              and p.estadoInvitacion = com.metamorfosis.backend.model.EstadoInvitacion.ACEPTADA
              and m.estado = com.metamorfosis.backend.model.EstadoMeta.CUMPLIDA
            """)
    List<Object[]> plazosYTiposCumplidos(@Param("codigoUsuario") Integer codigoUsuario);
}
