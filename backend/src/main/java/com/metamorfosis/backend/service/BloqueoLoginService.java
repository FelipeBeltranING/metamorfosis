package com.metamorfosis.backend.service;
 
import com.metamorfosis.backend.model.IntentoLogin;
import com.metamorfosis.backend.repository.IntentoLoginRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class BloqueoLoginService {
 
    public static final int MAX_INTENTOS = 5;
    public static final Duration DURACION_BLOQUEO = Duration.ofMinutes(5);
 
    private final IntentoLoginRepository repositorio;
    private final Clock reloj;
 
    // Con dos constructores, Spring necesita saber cuál usar: este.
    @Autowired
    public BloqueoLoginService(IntentoLoginRepository repositorio) {
        this(repositorio, Clock.systemDefaultZone());
    }
 
    /** Constructor para pruebas: permite fijar la hora. */
    BloqueoLoginService(IntentoLoginRepository repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }
 
    /** Segundos que faltan para que termine el bloqueo; 0 si el email no está bloqueado. */
    @Transactional(readOnly = true)
    public long segundosBloqueado(String email) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return repositorio.findById(email)
                .map(IntentoLogin::getBloqueadoHasta)
                .filter(hasta -> hasta.isAfter(ahora))
                .map(hasta -> Duration.between(ahora, hasta).toSeconds() + 1)
                .orElse(0L);
    }
 
    /** Suma un fallo; al llegar a MAX_INTENTOS bloquea el email durante DURACION_BLOQUEO. */
    @Transactional
    public void registrarFallo(String email) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        IntentoLogin intento = repositorio.findById(email).orElseGet(() -> new IntentoLogin(email));
 
        // Si un bloqueo anterior ya venció, el conteo empieza de cero.
        if (intento.getBloqueadoHasta() != null && !intento.getBloqueadoHasta().isAfter(ahora)) {
            intento.setIntentos(0);
            intento.setBloqueadoHasta(null);
        }
 
        intento.setIntentos(intento.getIntentos() + 1);
        if (intento.getIntentos() >= MAX_INTENTOS) {
            intento.setBloqueadoHasta(ahora.plus(DURACION_BLOQUEO));
        }
        repositorio.save(intento);
    }
 
    /** Login exitoso: se borra el historial de fallos de ese email. */
    @Transactional
    public void limpiar(String email) {
        if (repositorio.existsById(email)) {
            repositorio.deleteById(email);
        }
    }
}