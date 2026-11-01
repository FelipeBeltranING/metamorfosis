package com.metamorfosis.backend.repository;

import com.metamorfosis.backend.model.IntentoLogin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntentoLoginRepository extends JpaRepository<IntentoLogin, String> {
}
