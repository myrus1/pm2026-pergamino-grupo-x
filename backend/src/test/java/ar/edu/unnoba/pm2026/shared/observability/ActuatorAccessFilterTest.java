package ar.edu.unnoba.pm2026.shared.observability;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.unnoba.pm2026.auth.exception.AuthenticationException;
import ar.edu.unnoba.pm2026.auth.exception.AuthorizationException;
import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.shared.exception.ApiExceptionHandler;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.shared.security.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// /actuator queda solo para administradores, salvo /actuator/health (lo usan los chequeos de
// disponibilidad, que no tienen token). Los filtros se registran con ObservabilityConfig, como en la
// app, y los errores los arma ApiExceptionHandler: salen con el mismo ProblemDetail que el resto de la API.
// Un controller de prueba ocupa el lugar de los endpoints de Actuator, asi no hace falta la base.
@WebMvcTest(controllers = ActuatorAccessFilterTest.ActuatorStubController.class)
@Import({ ObservabilityConfig.class, ApiExceptionHandler.class, ActuatorAccessFilterTest.ActuatorStubController.class })
class ActuatorAccessFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthorizationService authorizationService;

    @Test
    void healthEsPublico() throws Exception {
        for (String path : new String[] {"/actuator/health", "/actuator/health/liveness"}) {
            mockMvc.perform(get(path)).andExpect(status().isOk());
        }
        verifyNoInteractions(authorizationService);
    }

    @Test
    void lasRutasQueNoSonDeActuatorNoSeTocan() throws Exception {
        mockMvc.perform(get("/api/courses")).andExpect(status().isOk());

        verifyNoInteractions(authorizationService);
    }

    @Test
    void sinTokenResponde401ConProblemDetail() throws Exception {
        when(authorizationService.authorize(eq(null), any(Role[].class)))
                .thenThrow(new AuthenticationException("Token invalido o vencido"));

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Token invalido o vencido"))
                .andExpect(jsonPath("$.code").value("authentication.failed"))
                .andExpect(jsonPath("$.instance").value("/actuator/prometheus"))
                .andExpect(jsonPath("$.mensaje").doesNotExist())
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void conTokenDeOtroRolResponde403ConProblemDetail() throws Exception {
        when(authorizationService.authorize("Bearer instructor", Role.ADMINISTRATOR))
                .thenThrow(new AuthorizationException("No tiene permisos para esta operacion"));

        mockMvc.perform(get("/actuator/metrics").header("Authorization", "Bearer instructor"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("No tiene permisos para esta operacion"))
                .andExpect(jsonPath("$.code").value("authorization.denied"));
    }

    @Test
    void conTokenDeAdministradorPasa() throws Exception {
        when(authorizationService.authorize("Bearer admin", Role.ADMINISTRATOR))
                .thenReturn(new AuthenticatedUser(1L, "ada@example.com", Role.ADMINISTRATOR));

        mockMvc.perform(get("/actuator/prometheus").header("Authorization", "Bearer admin"))
                .andExpect(status().isOk());
    }

    // "/actuator/health/../prometheus" termina en /actuator/prometheus: no puede pasar como health
    @Test
    void unaRutaConPuntosNoSeSaltaElControl() throws Exception {
        when(authorizationService.authorize(eq(null), any(Role[].class)))
                .thenThrow(new AuthenticationException("Token invalido o vencido"));
        for (String path : new String[] {"/actuator/health/../prometheus", "//actuator/metrics", "/actuator"}) {
            mockMvc.perform(get("/").with(rawUri(path)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("authentication.failed"));
        }
    }

    // MockMvc interpreta "//actuator" como un host: la ruta cruda se pone directo en el request
    private static RequestPostProcessor rawUri(String path) {
        return request -> {
            request.setRequestURI(path);
            return request;
        };
    }

    @RestController
    public static class ActuatorStubController {

        @GetMapping({"/actuator/**", "/api/courses"})
        String ok() {
            return "ok";
        }
    }
}
