package ar.edu.unnoba.pm2026.shared.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.unnoba.pm2026.auth.exception.AuthenticationException;
import ar.edu.unnoba.pm2026.auth.exception.AuthorizationException;
import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.shared.security.Role;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

// /actuator queda solo para administradores, salvo /actuator/health (lo usan los chequeos de
// disponibilidad, que no tienen token). Los errores salen con el mismo formato que el resto de la API.
class ActuatorAccessFilterTest {

    private final AuthorizationService authorizationService = mock(AuthorizationService.class);
    private final ActuatorAccessFilter filter = new ActuatorAccessFilter(authorizationService);

    @Test
    void healthEsPublico() throws Exception {
        for (String path : new String[] {"/actuator/health", "/actuator/health/liveness"}) {
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(new MockHttpServletRequest("GET", path), new MockHttpServletResponse(), chain);

            assertThat(chain.getRequest()).isNotNull();
        }
        verifyNoInteractions(authorizationService);
    }

    @Test
    void lasRutasQueNoSonDeActuatorNoSeTocan() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest("GET", "/api/courses"), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(authorizationService);
    }

    @Test
    void sinTokenResponde401() throws Exception {
        when(authorizationService.authorize(eq(null), any(Role[].class)))
                .thenThrow(new AuthenticationException("Token invalido o vencido"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest("GET", "/actuator/prometheus"), response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).isEqualTo("{\"mensaje\":\"Token invalido o vencido\"}");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void conTokenDeOtroRolResponde403() throws Exception {
        when(authorizationService.authorize("Bearer instructor", Role.ADMINISTRATOR))
                .thenThrow(new AuthorizationException("No tiene permisos para esta operacion"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/metrics");
        request.addHeader("Authorization", "Bearer instructor");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).isEqualTo("{\"mensaje\":\"No tiene permisos para esta operacion\"}");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void conTokenDeAdministradorPasa() throws Exception {
        when(authorizationService.authorize("Bearer admin", Role.ADMINISTRATOR))
                .thenReturn(new AuthenticatedUser(1L, "ada@example.com", Role.ADMINISTRATOR));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/prometheus");
        request.addHeader("Authorization", "Bearer admin");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    // "/actuator/health/../prometheus" termina en /actuator/prometheus: no puede pasar como health
    @Test
    void unaRutaConPuntosNoSeSaltaElControl() throws Exception {
        when(authorizationService.authorize(eq(null), any(Role[].class)))
                .thenThrow(new AuthenticationException("Token invalido o vencido"));
        for (String path : new String[] {"/actuator/health/../prometheus", "//actuator/metrics", "/actuator"}) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(new MockHttpServletRequest("GET", path), response, chain);

            assertThat(response.getStatus()).as(path).isEqualTo(401);
            assertThat(chain.getRequest()).as(path).isNull();
        }
    }
}
