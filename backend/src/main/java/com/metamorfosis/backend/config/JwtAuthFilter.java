package com.metamorfosis.backend.config;

import com.metamorfosis.backend.exception.TokenInvalidoException;
import com.metamorfosis.backend.repository.TokenRevocadoRepository;
import com.metamorfosis.backend.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final TokenRevocadoRepository tokenRevocadoRepository;

    public JwtAuthFilter(JwtService jwtService, TokenRevocadoRepository tokenRevocadoRepository) {
        this.jwtService = jwtService;
        this.tokenRevocadoRepository = tokenRevocadoRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain cadena) throws ServletException, IOException {
        String cabecera = request.getHeader("Authorization");
        if (cabecera != null && cabecera.startsWith(PREFIJO)) {
            autenticar(cabecera.substring(PREFIJO.length()).trim());
        }
        cadena.doFilter(request, response);
    }

    private void autenticar(String token) {
        try {
            String jti = jwtService.extraerId(token);
            Integer codigoUsuario = jwtService.extraerCodigoUsuario(token);
            if (tokenRevocadoRepository.existsByJti(jti)) {
                return;
            }
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(codigoUsuario, null, List.of()));
        } catch (TokenInvalidoException e) {
            // Token falso, mal formado o expirado: la petición sigue sin autenticar.
        }
    }
}
