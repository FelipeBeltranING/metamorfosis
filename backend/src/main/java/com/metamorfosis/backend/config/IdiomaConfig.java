package com.metamorfosis.backend.config;

import com.metamorfosis.backend.i18n.TraductorMensajes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * Internacionalización: el idioma de cada petición sale de la cabecera Accept-Language que manda
 * el frontend (español o inglés). Sin cabecera, o con otro idioma, se responde en español.
 */
@Configuration
public class IdiomaConfig {

    @Bean
    LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(TraductorMensajes.IDIOMAS);
        resolver.setDefaultLocale(TraductorMensajes.ESPANOL);
        return resolver;
    }
}
