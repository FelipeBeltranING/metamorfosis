package com.metamorfosis.backend.repository;

import com.metamorfosis.backend.model.SolicitudRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SolicitudRegistroRepository extends JpaRepository<SolicitudRegistro, Integer> {

    Optional<SolicitudRegistro> findByEmail(String email);

    Optional<SolicitudRegistro> findByTokenHash(String tokenHash);

    /** Borra las solicitudes vencidas (y los bloqueos que ya terminaron). Devuelve cuántas borró. */
    @Modifying
    @Query("delete from SolicitudRegistro s where s.fechaExpiracion < :ahora")
    int borrarVencidas(@Param("ahora") LocalDateTime ahora);
}
