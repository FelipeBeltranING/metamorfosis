package com.metamorfosis.backend.repository;

import com.metamorfosis.backend.model.TokenRevocado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenRevocadoRepository extends JpaRepository<TokenRevocado, Integer> {
    boolean existsByJti(String jti);
}
