package ar.edu.unnoba.pm2026;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.unnoba.pm2026.shared.security.JwtTokenUtil;
import ar.edu.unnoba.pm2026.shared.security.Role;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

// Observabilidad (#28) con el contexto completo. Usa la misma configuracion que ApiDocumentationTest
// para reutilizar el contexto y no abrir mas conexiones contra la BD compartida.
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ObservabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private PrometheusMeterRegistry prometheusMeterRegistry;

    @Test
    void healthEsPublicoYMuestraElEstadoDeLaBaseSinDetalles() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.db.status").value("UP"))
                .andExpect(jsonPath("$.components.db.details").doesNotExist());
    }

    @Test
    void elRestoDeActuatorPideTokenDeAdministrador() throws Exception {
        for (String path : new String[] {"/actuator/prometheus", "/actuator/metrics", "/actuator/info"}) {
            mockMvc.perform(get(path))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value("Token invalido o vencido"))
                    .andExpect(jsonPath("$.code").value("authentication.invalid-token"))
                    .andExpect(jsonPath("$.instance").value(path));
        }

        String instructorToken = jwtTokenUtil.generateToken("iris@example.com", Role.INSTRUCTOR, 4L);
        mockMvc.perform(get("/actuator/prometheus").header("Authorization", instructorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("No tiene permisos para esta operacion"))
                .andExpect(jsonPath("$.code").value("authorization.denied"));
    }

    @Test
    void cadaRespuestaTraeElCorrelationId() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(header().string("X-Correlation-Id", matchesPattern("[0-9a-f-]{36}")));
        mockMvc.perform(get("/actuator/health").header("X-Correlation-Id", "demo-123"))
                .andExpect(header().string("X-Correlation-Id", "demo-123"));
    }

    @Test
    void prometheusExponeLasMetricasDeNegocioYLasDeHttp() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());

        String scrape = prometheusMeterRegistry.scrape();

        assertThat(scrape)
                .contains("pm2026_auth_logins_total{")
                .contains("role=\"STUDENT\"")
                .contains("pm2026_enrollments_total")
                .contains("pm2026_course_approvals_total{")
                .contains("http_server_requests_seconds_count{")
                .contains("hikaricp_connections_max");
    }
}
