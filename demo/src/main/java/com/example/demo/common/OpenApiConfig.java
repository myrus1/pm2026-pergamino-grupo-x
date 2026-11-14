package com.example.demo.common;

import org.springdoc.core.customizers.OperationCustomizer;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

// Documentacion OpenAPI de la API (springdoc).
// Swagger UI: /swagger-ui.html  |  spec JSON: /v3/api-docs
// El token se envía desde el botón Authorize con el valor completo "Bearer <jwt>".
@Configuration
public class OpenApiConfig {

    // Nombre del esquema de seguridad que usan los @SecurityRequirement de los controladores
    public static final String ESQUEMA_TOKEN = "token";

    @Bean
    public OpenAPI apiDocumentacion() {
        return new OpenAPI()
                .info(new Info()
                        .title("PM2026 - API de la academia")
                        .description("API REST del trabajo practico: cursos, especialidades, inscripciones, "
                                + "aprobaciones de cursos y usuarios. La autenticacion es por JWT: se llama a "
                                + "POST /student/auth, POST /instructor/auth o POST /administrator/auth y el "
                                + "token obtenido se envía en el header Authorization (con el prefijo Bearer).")
                        .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_TOKEN, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")
                                .description("Token JWT devuelto por los logins, con el prefijo Bearer")));
    }

    // Quitar el header Authorization que springdoc infiere de los @RequestHeader: el token no es un
    // parámetro mas del endpoint, se envía desde el botón Authorize con el esquema "token".
    @Bean
    public OperationCustomizer quitarHeaderDeAutorizacion() {
        return new QuitarHeaderDeAutorizacion();
    }

    static class QuitarHeaderDeAutorizacion implements OperationCustomizer {
        @Override
        public Operation customize(Operation operation, HandlerMethod handlerMethod) {
            if (operation.getParameters() != null) {
                operation.getParameters().removeIf(parametro -> "Authorization".equals(parametro.getName()));
            }
            return operation;
        }
    }
}
