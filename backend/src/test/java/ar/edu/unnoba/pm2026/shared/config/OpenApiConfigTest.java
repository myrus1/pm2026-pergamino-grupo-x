package ar.edu.unnoba.pm2026.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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

        config.removeAuthorizationHeader().customize(operation, handlerMethod("read"));

        assertThat(operation.getParameters())
                .extracting(Parameter::getName)
                .containsExactly("X-Ejemplo", "page");
    }

    @Test
    void noFallaCuandoLaOperacionNoTieneParametros() throws NoSuchMethodException {
        Operation operation = new Operation();

        config.removeAuthorizationHeader().customize(operation, handlerMethod("read"));

        assertThat(operation.getParameters()).isNull();
    }

    @Test
    void registraElEsquemaProblemDetail() {
        Schema<?> schema = config.apiDocumentation().getComponents().getSchemas().get("ProblemDetail");

        assertThat(schema).isNotNull();
        assertThat(schema.getProperties().keySet())
                .contains("type", "title", "status", "detail", "instance", "code", "timestamp", "errors");
    }

    @Test
    void documentaErroresYLocationEnAltasConCuerpo() throws Exception {
        Operation operation = new Operation()
                .requestBody(new io.swagger.v3.oas.models.parameters.RequestBody())
                .responses(new ApiResponses().addApiResponse("201", new ApiResponse().description("Creado")));

        config.documentApiResponses().customize(operation, handlerMethod("create"));

        assertProblemResponse(operation, "400");
        assertProblemResponse(operation, "405");
        assertProblemResponse(operation, "409");
        assertProblemResponse(operation, "415");
        assertProblemResponse(operation, "500");
        assertThat(operation.getResponses().get("201").getHeaders()).containsKey("Location");
        assertThat(operation.getResponses().get("201").getHeaders().get("Location").getSchema().getFormat())
                .isEqualTo("uri");
    }

    @Test
    void documentaAutorizacionYNoEncontradoEnOperacionesProtegidasConId() throws Exception {
        Operation operation = new Operation().addParametersItem(new Parameter().name("id").in("path"));

        config.documentApiResponses().customize(operation, handlerMethod("getById"));

        assertProblemResponse(operation, "401");
        assertProblemResponse(operation, "403");
        assertProblemResponse(operation, "404");
    }

    @Test
    void documentaAltasResponseEntityYErroresDeAutenticacion() throws Exception {
        Operation alta = new Operation().responses(
                new ApiResponses().addApiResponse("201", new ApiResponse().description("Creado")));
        config.documentApiResponses().customize(alta, handlerMethod("createLegacy"));
        assertThat(alta.getResponses().get("201").getHeaders()).containsKey("Location");

        Operation login = new Operation().requestBody(new io.swagger.v3.oas.models.parameters.RequestBody());
        config.documentApiResponses().customize(login, handlerMethod("authentication"));
        assertProblemResponse(login, "401");
        assertThat(login.getResponses()).doesNotContainKey("409");
    }

    @Test
    void completaProblemDetailEnRespuestas404DeclaradasPorLaOperacion() throws Exception {
        Operation operation = new Operation()
                .requestBody(new io.swagger.v3.oas.models.parameters.RequestBody())
                .responses(new ApiResponses().addApiResponse("404",
                        new ApiResponse().description("No existe el recurso relacionado")));

        config.documentApiResponses().customize(operation, handlerMethod("create"));

        assertProblemResponse(operation, "404");
    }

    @Test
    void documenta400SoloCuandoLaOperacionRecibeParametrosOCuerpo() throws Exception {
        Operation sinEntrada = new Operation();
        config.documentApiResponses().customize(sinEntrada, handlerMethod("read"));
        assertThat(sinEntrada.getResponses()).doesNotContainKey("400");

        Operation conQuery = new Operation().addParametersItem(new Parameter().name("filter").in("query"));
        config.documentApiResponses().customize(conQuery, handlerMethod("read"));
        assertProblemResponse(conQuery, "400");
    }

    @Test
    void conservaYCompletaRespuestasDeAutorizacionCondicional() throws Exception {
        Operation operation = new Operation().responses(new ApiResponses()
                .addApiResponse("401", new ApiResponse().description("Token inválido"))
                .addApiResponse("403", new ApiResponse().description("Sin permisos")));
        config.documentApiResponses().customize(operation, handlerMethod("conditionalRead"));

        assertProblemResponse(operation, "401");
        assertProblemResponse(operation, "403");
    }

    private void assertProblemResponse(Operation operation, String code) {
        ApiResponse response = operation.getResponses().get(code);
        assertThat(response).as("falta la respuesta HTTP " + code).isNotNull();
        assertThat(response.getContent()).containsKey("application/problem+json");
        Schema<?> schema = response.getContent().get("application/problem+json").getSchema();
        assertThat(schema.get$ref()).isEqualTo("#/components/schemas/ProblemDetail");
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

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        public String create() {
            return "created";
        }

        @PostMapping("/legacy")
        public String createLegacy() {
            return "created";
        }

        @PostMapping("/auth")
        public String authentication() {
            return "authenticated";
        }

        @GetMapping("/{id}")
        @SecurityRequirement(name = OpenApiConfig.ESQUEMA_TOKEN)
        public String getById(Long id) {
            return id.toString();
        }

        @GetMapping("/conditional")
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token inválido")
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Sin permisos")
        public String conditionalRead() {
            return "conditional";
        }
    }

    private HandlerMethod handlerMethod(String methodName) throws NoSuchMethodException {
        if ("create".equals(methodName) || "createLegacy".equals(methodName)
                || "authentication".equals(methodName) || "conditionalRead".equals(methodName)) {
            return new HandlerMethod(new TestController(), TestController.class.getMethod(methodName));
        }
        if ("getById".equals(methodName)) {
            return new HandlerMethod(new TestController(), TestController.class.getMethod(methodName, Long.class));
        }
        return new HandlerMethod(new TestController(),
                TestController.class.getMethod(methodName, String.class));
    }
}
