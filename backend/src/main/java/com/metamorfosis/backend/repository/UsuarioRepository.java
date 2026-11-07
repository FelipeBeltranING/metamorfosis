package com.metamorfosis.backend.repository;

import com.metamorfosis.backend.model.Usuario;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    boolean existsByEmail(String email);
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    /**
     * HU24: personas cuyo "nombre apellido" contiene el patrón, sin mayúsculas ni tildes (el patrón
     * ya llega así), sin incluir a quien busca, ordenadas por nombre. La página limita la cantidad.
     */
    @Query("""
            select u from Usuario u
            where u.codigoUsuario <> :codigoUsuario
              and function('translate', lower(concat(u.nombre, ' ', u.apellido)), 'áéíóúüñ', 'aeiouun') like :patron
            order by u.nombre, u.apellido, u.codigoUsuario
            """)
    List<Usuario> buscarPorNombre(@Param("codigoUsuario") Integer codigoUsuario,
                                  @Param("patron") String patron, Pageable pagina);
}
