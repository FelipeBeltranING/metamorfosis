package com.metamorfosis.backend.repository;

import com.metamorfosis.backend.model.LogActividad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogRepository extends JpaRepository<LogActividad, Integer> {
}