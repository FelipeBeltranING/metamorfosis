package com.metamorfosis.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Sub-metas: el "hoy" de los usuarios. El servidor trabaja en UTC; entre las 7 p. m. y la
 * medianoche de Colombia ya sería "mañana", así que los días se cuentan en la zona de la app.
 */
@Component
public class CalendarioUsuario {

    private final Clock reloj;

    @Autowired
    public CalendarioUsuario(@Value("${app.zona-horaria:America/Bogota}") String zona) {
        this(Clock.system(ZoneId.of(zona)));
    }

    /** Para pruebas: un reloj fijo. */
    public CalendarioUsuario(Clock reloj) {
        this.reloj = reloj;
    }

    public LocalDate hoy() {
        return LocalDate.now(reloj);
    }
}
