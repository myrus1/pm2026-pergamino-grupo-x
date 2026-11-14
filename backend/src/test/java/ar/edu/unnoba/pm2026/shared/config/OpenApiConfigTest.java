package ar.edu.unnoba.pm2026.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

// Configuracion de la documentacion OpenAPI: info, esquema del token y limpieza del header.
class OpenApiConfigTest {

    private final OpenApiConfig config = new OpenApiConfig();

    @Test
    void laInfoDeLaApiTieneTituloVersionYComoSeObtieneElToken() {
        Info info = config.apiDocumentation().getInfo();

        assertThat(info.getTitle()).isEqualTo("PM2026 - API de la academia");
        assertThat(info.getVersion()).isEqualTo("1.0.0");
        assertThat(info.getDescription())
                .contains("POST /student/auth")
                .contains("header Authorization");
    }

    @Test
    void elTokenSeDocumentaComoApiKeyEnElHeaderAuthorization() {
        SecurityScheme esquema = config.apiDocumentation().getComponents().getSecuritySchemes()
                .get(OpenApiConfig.ESQUEMA_TOKEN);

        assertThat(esquema).isNotNull();
        assertThat(esquema.getType()).isEqualTo(SecurityScheme.Type.APIKEY);
        assertThat(esquema.getIn()).isEqualTo(SecurityScheme.In.HEADER);
        assertThat(esquema.getName()).isEqualTo("Authorization");
        assertThat(esquema.getDescription()).contains("Bearer");
    }

    @Test
    void quitaElHeaderAuthorizationPeroDejaLosDemasParametros() throws Exception {
        Operation operation = new Operation();
        operation.addParametersItem(header("Authorization"));
        operation.addParametersItem(header("X-Ejemplo"));
        operation.addParametersItem(queryParameter("page"));

        config.removeAuthorizationHeader().customize(operation, handlerMethod());

        assertThat(operation.getParameters())
                .extracting(Parameter::getName)
                .containsExactly("X-Ejemplo", "page");
    }

    @Test
    void noFallaCuandoLaOperacionNoTieneParametros() throws NoSuchMethodException {
        Operation operation = new Operation();

        config.removeAuthorizationHeader().customize(operation, handlerMethod());

        assertThat(operation.getParameters()).isNull();
    }

    private static Parameter header(String nombre) {
        return new Parameter().name(nombre).in("header");
    }

    private static Parameter queryParameter(String nombre) {
        return new Parameter().name(nombre).in("query");
    }

    // Controlador de mentira: el customizer solo necesita un HandlerMethod valido para la firma
    @SuppressWarnings("unused")
    static class TestController {
        public String read(String token) {
            return token;
        }
    }

    private HandlerMethod handlerMethod() throws NoSuchMethodException {
        return new HandlerMethod(new TestController(),
                TestController.class.getMethod("read", String.class));
    }
}
