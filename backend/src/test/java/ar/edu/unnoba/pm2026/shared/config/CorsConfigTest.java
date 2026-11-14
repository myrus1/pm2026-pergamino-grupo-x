package ar.edu.unnoba.pm2026.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

// Configuracion CORS: origenes permitidos, metodos, headers y el preflight de OPTIONS.
class CorsConfigTest {

    @Test
    void permiteLosOrigenesConfiguradosEnTodaLaApi() {
        CorsConfiguration configuration = configurationOf(
                new CorsConfig("http://localhost:5173, http://localhost:3000"));

        assertThat(configuration.getAllowedOriginPatterns())
                .containsExactly("http://localhost:5173", "http://localhost:3000");
    }

    @Test
    void porDefectoPermiteCualquierOrigenDeDesarrollo() {
        CorsConfiguration configuration = configurationOf(new CorsConfig("*"));

        assertThat(configuration.getAllowedOriginPatterns()).containsExactly("*");
    }

    @Test
    void ignoraOrigenesVaciosDeLaLista() {
        CorsConfiguration configuration = configurationOf(new CorsConfig(" ,http://localhost:5173,"));

        assertThat(configuration.getAllowedOriginPatterns()).containsExactly("http://localhost:5173");
    }

    @Test
    void permiteLosMetodosDeLaApiYElHeaderAuthorization() {
        CorsConfiguration configuration = configurationOf(new CorsConfig("*"));

        assertThat(configuration.getAllowedMethods())
                .containsExactlyInAnyOrder("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(configuration.getAllowedHeaders()).containsExactly("*");
        assertThat(configuration.getAllowCredentials()).isFalse();
        assertThat(configuration.getMaxAge()).isEqualTo(3600L);
    }

    private CorsConfiguration configurationOf(CorsConfig config) {
        RegistroVisible registry = new RegistroVisible();
        config.addCorsMappings(registry);
        return registry.configurations().get("/**");
    }

    // getCorsConfigurations() es protected: se expone desde una subclase para poder inspeccionarlo
    static class RegistroVisible extends CorsRegistry {
        Map<String, CorsConfiguration> configurations() {
            return getCorsConfigurations();
        }
    }
}
