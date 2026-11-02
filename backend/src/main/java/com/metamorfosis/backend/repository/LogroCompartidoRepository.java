package com.metamorfosis.backend.repository;

import com.metamorfosis.backend.model.LogroCompartido;
import com.metamorfosis.backend.model.Meta;
import com.metamorfosis.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LogroCompartidoRepository extends JpaRepository<LogroCompartido, Integer> {

    /** HU21: enlace ya creado por este usuario para esta meta (se reutiliza). */
    Optional<LogroCompartido> findByMetaAndUsuario(Meta meta, Usuario usuario);

    /** HU21: logro que abre quien recibe el enlace. */
    Optional<LogroCompartido> findByToken(String token);
}
