package com.metamorfosis.backend.repository;

import com.metamorfosis.backend.model.UsuarioInsignia;
import com.metamorfosis.backend.model.UsuarioInsigniaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** HU11: insignias que ya ganó cada usuario. */
public interface UsuarioInsigniaRepository extends JpaRepository<UsuarioInsignia, UsuarioInsigniaId> {

    /** Códigos de las insignias que el usuario ya tiene. */
    @Query("""
            select ui.id.codigoInsignia from UsuarioInsignia ui
            where ui.id.codigoUsuario = :codigoUsuario
            """)
    List<Integer> codigosObtenidos(@Param("codigoUsuario") Integer codigoUsuario);
}
