package com.metamorfosis.backend.config;

import com.metamorfosis.backend.i18n.TraductorMensajes;
import com.metamorfosis.backend.repository.TokenRevocadoRepository;
import com.metamorfosis.backend.service.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.LocaleResolver;

import java.util.List;

@Configuration
public class SeguridadConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filtros(HttpSecurity http, JwtService jwtService,
                                TokenRevocadoRepository tokenRevocadoRepository,
                                TraductorMensajes traductor, LocaleResolver localeResolver) throws Exception {
        http.csrf(csrf -> csrf.disable())                     // API stateless sin cookies de sesión
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(c -> {})
            .addFilterBefore(new JwtAuthFilter(jwtService, tokenRevocadoRepository),
                    UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(e -> e.authenticationEntryPoint((peticion, respuesta, error) -> {
                respuesta.setStatus(401);
                respuesta.setContentType("application/json;charset=UTF-8");
                // Este error sale antes de llegar a los controladores: el idioma se lee aquí de la petición.
                String mensaje = traductor.traducir("Autenticación requerida", localeResolver.resolveLocale(peticion));
                respuesta.getWriter().write(
                        "{\"estado\":401,\"mensaje\":\"" + mensaje + "\",\"errores\":[]}");
            }))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/logros/**").permitAll()   // HU21: logro compartido
                // HU14: la página del enlace del correo y "Rechazar" no piden iniciar sesión (aceptar sí)
                .requestMatchers(HttpMethod.GET, "/api/invitaciones/correo").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/invitaciones/correo/rechazar").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().denyAll());
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") List<String> origenes) {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(origenes);
        c.setAllowedMethods(List.of(HttpMethod.GET.name(), HttpMethod.POST.name(),
                HttpMethod.PUT.name(), HttpMethod.PATCH.name(), HttpMethod.DELETE.name()));
        c.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept-Language"));
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", c);
        return fuente;
    }
}
