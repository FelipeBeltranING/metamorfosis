package com.metamorfosis.backend.repository;

import com.metamorfosis.backend.model.SubMeta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubMetaRepository extends JpaRepository<SubMeta, Integer> {

    /** Sub-metas de una meta, en el orden en que se crearon. */
    List<SubMeta> findByMetaCodigoMetaOrderByCodigoSubMetaAsc(Integer codigoMeta);

    boolean existsByMetaCodigoMetaAndNombreIgnoreCase(Integer codigoMeta, String nombre);

    boolean existsByMetaCodigoMetaAndNombreIgnoreCaseAndCodigoSubMetaNot(Integer codigoMeta, String nombre,
                                                                         Integer codigoSubMeta);
}
