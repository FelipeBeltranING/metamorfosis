package com.metamorfosis.backend.repository;

import com.metamorfosis.backend.model.Insignia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** HU11: catálogo de insignias. */
public interface InsigniaRepository extends JpaRepository<Insignia, Integer> {

    /** El catálogo en su orden (el de la migración V14), que también es el orden de los avisos. */
    List<Insignia> findAllByOrderByCodigoInsigniaAsc();
}
