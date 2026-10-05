package com.metamorfosis.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SeguridadConfig {

    @Bean
    SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)            // API stateless sin cookies de sesión
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(c -> {})
            .authorizeHttpRequests(a -> a
                // TODO (Sprint de login): cambiar a .authenticated() y validar el JWT
                .requestMatchers("/api/**").permitAll()
                .anyRequest().denyAll());
        return http.build();
    }

    @Bean
    CorsConfigurationSource cors(@Value("${app.cors.allowed-origins}") List<String> origenes) {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(origenes);
        c.setAllowedMethods(List.of(HttpMethod.GET.name(), HttpMethod.POST.name(),
                HttpMethod.PUT.name(), HttpMethod.DELETE.name()));
        c.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", c);
        return fuente;
    }
}