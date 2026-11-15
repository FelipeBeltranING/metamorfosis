package com.metamorfosis.backend.repository;

import com.metamorfosis.backend.model.CodigoRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;

/** HU33: el código de recuperación vigente de cada cuenta (la llave es el código del usuario). */
public interface CodigoRecuperacionRepository extends JpaRepository<CodigoRecuperacion, Integer> {
}
