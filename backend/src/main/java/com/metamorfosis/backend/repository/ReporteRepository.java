package com.metamorfosis.backend.repository;

import com.metamorfosis.backend.model.Meta;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * HU7: consultas de agregación para el reporte general. Solo cuentan las metas en las que el
 * usuario participa con invitación ACEPTADA (las que creó y las compartidas que aceptó).
 */
public interface ReporteRepository extends Repository<Meta, Integer> {

    /** Filas [TipoMeta tipo, EstadoMeta estado, Long cantidad]. */
    @Query("""
            select m.tipo, m.estado, count(m) from Meta m
            join m.participaciones p
            where p.usuario.codigoUsuario = :codigoUsuario
              and p.estadoInvitacion = com.metamorfosis.backend.model.EstadoInvitacion.ACEPTADA
            group by m.tipo, m.estado
            """)
    List<Object[]> contarPorTipoYEstado(@Param("codigoUsuario") Integer codigoUsuario);

    /** Filas [Plazo plazo, EstadoMeta estado, Long cantidad]. */
    @Query("""
            select m.plazo, m.estado, count(m) from Meta m
            join m.participaciones p
            where p.usuario.codigoUsuario = :codigoUsuario
              and p.estadoInvitacion = com.metamorfosis.backend.model.EstadoInvitacion.ACEPTADA
            group by m.plazo, m.estado
            """)
    List<Object[]> contarPorPlazoYEstado(@Param("codigoUsuario") Integer codigoUsuario);

    /**
     * Fechas de cumplimiento (desde una fecha) de las metas cumplidas del usuario.
     * La fecha está en la participación del creador (HU6).
     */
    @Query("""
            select pc.fechaCumplida from Meta m
            join m.participaciones p
            join m.participaciones pc
            where p.usuario.codigoUsuario = :codigoUsuario
              and p.estadoInvitacion = com.metamorfosis.backend.model.EstadoInvitacion.ACEPTADA
              and pc.usuario = m.creador
              and m.estado = com.metamorfosis.backend.model.EstadoMeta.CUMPLIDA
              and pc.fechaCumplida >= :desde
            """)
    List<LocalDate> fechasCumplidasDesde(@Param("codigoUsuario") Integer codigoUsuario,
                                         @Param("desde") LocalDate desde);
}
