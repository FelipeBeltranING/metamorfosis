package com.metamorfosis.backend.repository;

import com.metamorfosis.backend.model.Registro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** HU5: acceso a los registros de cumplimiento de una meta. */
public interface RegistroRepository extends JpaRepository<Registro, Integer> {

    /**
     * El registro de un usuario para una meta en una fecha concreta, si ya existe
     * (permite actualizar el cumplimiento en vez de duplicar el registro del día).
     */
    Optional<Registro> findByMeta_CodigoMetaAndUsuario_CodigoUsuarioAndFechaRegistro(
            Integer codigoMeta, Integer codigoUsuario, LocalDate fechaRegistro);

    /**
     * HU5: códigos de meta con cumplimiento marcado por el usuario en una fecha dada.
     * Se usa para que el listado de metas (HU3) indique cuáles ya quedaron marcadas hoy.
     */
    @Query("""
            select r.meta.codigoMeta from Registro r
            where r.usuario.codigoUsuario = :codigoUsuario
              and r.fechaRegistro = :fecha
              and r.cumplimiento = true
            """)
    List<Integer> findCodigosMetaCumplidasEnFecha(@Param("codigoUsuario") Integer codigoUsuario,
                                                   @Param("fecha") LocalDate fecha);
}
