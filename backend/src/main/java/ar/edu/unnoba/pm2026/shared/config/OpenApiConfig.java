package ar.edu.unnoba.pm2026.shared.config;

import org.springdoc.core.customizers.OperationCustomizer;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.HandlerMethod;

// Documentacion OpenAPI de la API (springdoc).
// Swagger UI: /swagger-ui.html  |  spec JSON: /v3/api-docs
// El token se envía desde el botón Authorize con el valor completo "Bearer <jwt>".
@Configuration
public class OpenApiConfig {

    // Nombre del esquema de seguridad que usan los @SecurityRequirement de los controladores
    public static final String ESQUEMA_TOKEN = "token";

    @Bean
    public OpenAPI apiDocumentation() {
        return new OpenAPI()
                .info(new Info()
                        .title("PM2026 - API de la academia")
                        .description("API REST del trabajo practico: cursos, especialidades, inscripciones, "
                                + "aprobaciones de cursos y usuarios. La autenticacion es por JWT: se llama al "
                                + "login unico POST /api/auth/token o al login de cada rol (POST /students/auth, "
                                + "POST /auth o POST /admin/auth) y el token se envía en el header Authorization "
                                + "con el prefijo Bearer. Los logins viejos POST /student/auth, /instructor/auth "
                                + "y /administrator/auth siguen andando pero quedan deprecados.")
                        .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_TOKEN, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")
                                .description("Valor completo \"Bearer <jwt>\": el token de los logins por rol, "
                                        + "o el accessToken de POST /api/auth/token con el prefijo agregado"))
                        .addSchemas("ProblemDetail", problemDetailSchema()));
    }

    // Quitar el header Authorization que springdoc infiere de los @RequestHeader: el token no es un
    // parámetro mas del endpoint, se envía desde el botón Authorize con el esquema "token".
    @Bean
    public OperationCustomizer removeAuthorizationHeader() {
        return new RemoveAuthorizationHeader();
    }

    @Bean
    public OperationCustomizer documentApiResponses() {
        return new DocumentApiResponses();
    }

    private static Schema<?> problemDetailSchema() {
        ObjectSchema violation = new ObjectSchema();
        violation.addProperty("field", new StringSchema());
        violation.addProperty("message", new StringSchema());
        violation.setRequired(List.of("field", "message"));

        ObjectSchema problem = new ObjectSchema();
        problem.addProperty("type", new StringSchema().format("uri"));
        problem.addProperty("title", new StringSchema());
        problem.addProperty("status", new IntegerSchema());
        problem.addProperty("detail", new StringSchema());
        problem.addProperty("instance", new StringSchema().format("uri"));
        problem.addProperty("code", new StringSchema());
        problem.addProperty("timestamp", new StringSchema().format("date-time"));
        problem.addProperty("errors", new ArraySchema().items(violation));
        problem.setRequired(List.of("title", "status", "detail", "instance", "code", "timestamp"));
        return problem;
    }

    static class RemoveAuthorizationHeader implements OperationCustomizer {
        @Override
        public Operation customize(Operation operation, HandlerMethod handlerMethod) {
            if (operation.getParameters() != null) {
                operation.getParameters().removeIf(parameter -> "Authorization".equals(parameter.getName()));
            }
            return operation;
        }
    }

    static class DocumentApiResponses implements OperationCustomizer {
        private static final String PROBLEM_MEDIA_TYPE = "application/problem+json";
        private static final String PROBLEM_SCHEMA_REF = "#/components/schemas/ProblemDetail";

        @Override
        public Operation customize(Operation operation, HandlerMethod handlerMethod) {
            ApiResponses responses = operation.getResponses();
            if (responses == null) {
                responses = new ApiResponses();
                operation.setResponses(responses);
            }

            addSuccessResponse(responses, handlerMethod);
            addProblemResponse(responses, "405", "Método HTTP no permitido");
            addProblemResponse(responses, "406", "Formato de respuesta no aceptable");
            addProblemResponse(responses, "500", "Error interno");

            if (hasRequestInput(operation)) {
                addProblemResponse(responses, "400", "Solicitud inválida");
            }
            if (hasPathParameter(operation) || responses.containsKey("404")) {
                addProblemResponse(responses, "404", "Recurso o ruta no encontrados");
            }
            if (operation.getRequestBody() != null) {
                addProblemResponse(responses, "415", "Tipo de contenido no soportado");
            }
            boolean authenticationEndpoint = isAuthenticationEndpoint(handlerMethod);
            if (authenticationEndpoint) {
                addProblemResponse(responses, "401", "Credenciales inválidas");
            } else if (isProtected(handlerMethod)) {
                addProblemResponse(responses, "401", "Autenticación requerida o inválida");
                addProblemResponse(responses, "403", "Permisos insuficientes");
            } else {
                addProblemResponseIfDeclared(responses, "401", "Autenticación requerida o inválida");
                addProblemResponseIfDeclared(responses, "403", "Permisos insuficientes");
            }
            if (isWriteOperation(handlerMethod) && !authenticationEndpoint) {
                addProblemResponse(responses, "409", "Conflicto con el estado o los datos del recurso");
            }

            ApiResponse created = responses.get("201");
            if (created != null) {
                created.addHeaderObject("Location", new Header()
                        .description("URI del recurso creado")
                        .schema(new StringSchema().format("uri")));
            }
            return operation;
        }

        private void addSuccessResponse(ApiResponses responses, HandlerMethod handlerMethod) {
            if (responses.keySet().stream().anyMatch(code -> code.startsWith("2"))) {
                return;
            }

            ResponseStatus responseStatus = AnnotatedElementUtils.findMergedAnnotation(
                    handlerMethod.getMethod(), ResponseStatus.class);
            if (responseStatus == null) {
                responseStatus = AnnotatedElementUtils.findMergedAnnotation(
                        handlerMethod.getBeanType(), ResponseStatus.class);
            }
            String code = responseStatus == null
                    ? "200"
                    : Integer.toString(responseStatus.code().value());
            String description = "204".equals(code)
                    ? "Operación completada sin contenido"
                    : "Operación completada";
            responses.addApiResponse(code, new ApiResponse().description(description));
        }

        private void addProblemResponse(ApiResponses responses, String code, String description) {
            ApiResponse response = responses.get(code);
            if (response == null) {
                response = new ApiResponse();
                responses.addApiResponse(code, response);
            }
            if (response.getDescription() == null) {
                response.setDescription(description);
            }
            response.setContent(new Content().addMediaType(PROBLEM_MEDIA_TYPE,
                    new MediaType()
                            .schema(new Schema<>().$ref(PROBLEM_SCHEMA_REF))));
        }

        private void addProblemResponseIfDeclared(ApiResponses responses, String code, String description) {
            if (responses.containsKey(code)) {
                addProblemResponse(responses, code, description);
            }
        }

        private boolean hasRequestInput(Operation operation) {
            return operation.getRequestBody() != null
                    || (operation.getParameters() != null
                            && operation.getParameters().stream().anyMatch(parameter ->
                                    "path".equals(parameter.getIn()) || "query".equals(parameter.getIn())));
        }

        private boolean hasPathParameter(Operation operation) {
            return operation.getParameters() != null
                    && operation.getParameters().stream().anyMatch(parameter -> "path".equals(parameter.getIn()));
        }

        private boolean isProtected(HandlerMethod handlerMethod) {
            return AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), SecurityRequirement.class)
                    || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), SecurityRequirement.class);
        }

        private boolean isWriteOperation(HandlerMethod handlerMethod) {
            RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(),
                    RequestMapping.class);
            return mapping != null && Arrays.stream(mapping.method()).anyMatch(method ->
                    method == RequestMethod.POST || method == RequestMethod.PUT
                            || method == RequestMethod.PATCH || method == RequestMethod.DELETE);
        }

        private boolean isAuthenticationEndpoint(HandlerMethod handlerMethod) {
            RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(),
                    RequestMapping.class);
            return mapping != null && Arrays.stream(mapping.path())
                    .anyMatch(path -> path.endsWith("/auth") || path.endsWith("/auth/token"));
        }
    }
}
