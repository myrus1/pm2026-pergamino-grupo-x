package com.example.demo.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

// Configuracion CORS: origenes permitidos, metodos, headers y el preflight de OPTIONS.
class CorsConfigTest {

    @Test
    void permiteLosOrigenesConfiguradosEnTodaLaApi() {
        CorsConfiguration configuracion = configuracionDe(
                new CorsConfig("http://localhost:5173, http://localhost:3000"));

        assertThat(configuracion.getAllowedOriginPatterns())
                .containsExactly("http://localhost:5173", "http://localhost:3000");
    }

    @Test
    void porDefectoPermiteCualquierOrigenDeDesarrollo() {
        CorsConfiguration configuracion = configuracionDe(new CorsConfig("*"));

        assertThat(configuracion.getAllowedOriginPatterns()).containsExactly("*");
    }

    @Test
    void ignoraOrigenesVaciosDeLaLista() {
        CorsConfiguration configuracion = configuracionDe(new CorsConfig(" ,http://localhost:5173,"));

        assertThat(configuracion.getAllowedOriginPatterns()).containsExactly("http://localhost:5173");
    }

    @Test
    void permiteLosMetodosDeLaApiYElHeaderAuthorization() {
        CorsConfiguration configuracion = configuracionDe(new CorsConfig("*"));

        assertThat(configuracion.getAllowedMethods())
                .containsExactlyInAnyOrder("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(configuracion.getAllowedHeaders()).containsExactly("*");
        assertThat(configuracion.getAllowCredentials()).isFalse();
        assertThat(configuracion.getMaxAge()).isEqualTo(3600L);
    }

    private CorsConfiguration configuracionDe(CorsConfig config) {
        RegistroVisible registry = new RegistroVisible();
        config.addCorsMappings(registry);
        return registry.configuraciones().get("/**");
    }

    // getCorsConfigurations() es protected: se expone desde una subclase para poder inspeccionarlo
    static class RegistroVisible extends CorsRegistry {
        Map<String, CorsConfiguration> configuraciones() {
            return getCorsConfigurations();
        }
    }
}
