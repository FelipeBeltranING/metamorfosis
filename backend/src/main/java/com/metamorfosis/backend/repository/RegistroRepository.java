package com.metamorfosis.backend.repository;

import com.metamorfosis.backend.model.Registro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

/** HU5: acceso a los registros de cumplimiento de una meta. */
public interface RegistroRepository extends JpaRepository<Registro, Integer> {

    /**
     * El registro de un usuario para una meta en una fecha concreta, si ya existe
     * (permite actualizar el cumplimiento en vez de duplicar el registro del día).
     */
    Optional<Registro> findByMeta_CodigoMetaAndUsuario_CodigoUsuarioAndFechaRegistro(
            Integer codigoMeta, Integer codigoUsuario, LocalDate fechaRegistro);
}
