package com.metamorfosis.backend.repository;

import com.metamorfosis.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> { 
    boolean existsByEmail(String email);
}