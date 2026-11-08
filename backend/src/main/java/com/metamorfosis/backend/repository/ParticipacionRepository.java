package com.metamorfosis.backend.repository;

import com.metamorfosis.backend.model.Participacion;
import com.metamorfosis.backend.model.ParticipacionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParticipacionRepository extends JpaRepository<Participacion, ParticipacionId> {

    /** HU14: la invitación del enlace del correo (se busca por el SHA-256 del token). */
    Optional<Participacion> findByTokenHash(String tokenHash);
}
