package ar.edu.unnoba.pm2026.shared.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

// Cada request lleva un correlationId: se toma del header X-Correlation-Id si viene uno valido,
// si no se genera. Se devuelve en la respuesta y queda en el MDC para que salga en los logs.
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void generaUnIdSiLaRequestNoTraeYLoDevuelveEnElHeader() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/api/courses"), response, new MockFilterChain());

        assertThat(response.getHeader("X-Correlation-Id")).matches("[0-9a-f-]{36}");
    }

    @Test
    void reusaElIdQueMandaElCliente() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/courses");
        request.addHeader("X-Correlation-Id", "front-1234_abc.def");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("X-Correlation-Id")).isEqualTo("front-1234_abc.def");
    }

    // Un id con saltos de linea o muy largo podria ensuciar los logs: se reemplaza por uno nuevo
    @Test
    void reemplazaUnIdInvalido() throws Exception {
        for (String invalid : new String[] {"abc\nINFO falso", "x".repeat(65), ""}) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/courses");
            request.addHeader("X-Correlation-Id", invalid);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, new MockFilterChain());

            assertThat(response.getHeader("X-Correlation-Id")).matches("[0-9a-f-]{36}");
        }
    }

    @Test
    void elIdEstaEnElMdcDuranteLaRequestYSeLimpiaAlTerminar() throws Exception {
        AtomicReference<String> duringRequest = new AtomicReference<>();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/api/courses"), response,
                (request, servletResponse) -> duringRequest.set(MDC.get("correlationId")));

        assertThat(duringRequest.get()).isEqualTo(response.getHeader("X-Correlation-Id"));
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void limpiaElMdcAunqueLaRequestFalle() {
        try {
            filter.doFilter(new MockHttpServletRequest("GET", "/api/courses"), new MockHttpServletResponse(),
                    (request, response) -> {
                        throw new IllegalStateException("fallo");
                    });
        } catch (Exception expected) {
            // la excepcion sigue de largo; lo que importa es que el MDC quede limpio
        }

        assertThat(MDC.get("correlationId")).isNull();
    }
}
