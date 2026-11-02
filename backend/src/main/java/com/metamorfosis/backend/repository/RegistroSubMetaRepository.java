package com.metamorfosis.backend.repository;

import com.metamorfosis.backend.model.RegistroSubMeta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RegistroSubMetaRepository extends JpaRepository<RegistroSubMeta, Integer> {

    Optional<RegistroSubMeta> findBySubMetaCodigoSubMetaAndUsuarioCodigoUsuarioAndFecha(
            Integer codigoSubMeta, Integer codigoUsuario, LocalDate fecha);

    /** Registros de un usuario en varias sub-metas entre dos fechas (inclusive). */
    List<RegistroSubMeta> findBySubMetaCodigoSubMetaInAndUsuarioCodigoUsuarioAndFechaBetween(
            Collection<Integer> codigosSubMeta, Integer codigoUsuario, LocalDate desde, LocalDate hasta);

    long countBySubMetaCodigoSubMeta(Integer codigoSubMeta);

    long countBySubMetaMetaCodigoMeta(Integer codigoMeta);
}
