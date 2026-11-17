package ar.edu.unnoba.pm2026.shared.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// CORS: permite que un frontend servido desde otro origen (otro puerto o dominio) llame a la API.
// Los origenes se configuran con la variable CORS_ALLOWED_ORIGINS (lista separada por comas);
// por defecto "*" para desarrollo. El token viaja en el header Authorization, por eso se declara
// como header permitido y no se habilitan credenciales por cookie.
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final List<String> allowedOrigins;

    public CorsConfig(@Value("${cors.allowed-origins:*}") String allowedOrigins) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Location")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
