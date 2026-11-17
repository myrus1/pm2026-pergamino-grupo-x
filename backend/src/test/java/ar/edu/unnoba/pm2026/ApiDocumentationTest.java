package ar.edu.unnoba.pm2026;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.unnoba.pm2026.shared.config.OpenApiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

// Contrato real de la documentacion OpenAPI: el spec publicado respeta la matriz de permisos
// (que endpoints piden token, que endpoints son publicos) y no filtra el header Authorization.
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ApiDocumentationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> HTTP_METHODS = Set.of("get", "post", "put", "patch", "delete", "options", "head",
            "trace");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void documentaComoPublicosLosEndpointsQueNoPidenToken() throws Exception {
        JsonNode paths = doc().get("paths");

        // Registro publico y logins
        assertPublic(paths, "/api/students", "post");
        assertPublic(paths, "/student/auth", "post");
        assertPublic(paths, "/instructor/auth", "post");
        assertPublic(paths, "/administrator/auth", "post");

        // Consultas publicas (hacen falta para mostrar los cursos)
        assertPublic(paths, "/api/courses", "get");
        assertPublic(paths, "/api/courses/{id}", "get");
        assertPublic(paths, "/api/instructors", "get");
        assertPublic(paths, "/api/instructors/{id}", "get");
        assertPublic(paths, "/api/specialties", "get");
        assertPublic(paths, "/api/specialties/{id}", "get");
    }

    @Test
    void documentaElTokenEnLosEndpointsProtegidos() throws Exception {
        JsonNode paths = doc().get("paths");

        assertRequiresToken(paths, "/api/students", "get");
        assertRequiresToken(paths, "/api/students/{id}", "put");
        assertRequiresToken(paths, "/api/students/{id}", "delete");
        assertRequiresToken(paths, "/api/courses", "post");
        assertRequiresToken(paths, "/api/courses/{id}", "put");
        assertRequiresToken(paths, "/api/instructors", "post");
        assertRequiresToken(paths, "/api/instructors/{id}/revenue", "get");
        assertRequiresToken(paths, "/api/specialties", "post");
        assertRequiresToken(paths, "/api/administrators", "get");
        assertRequiresToken(paths, "/api/enrollments", "post");
        assertRequiresToken(paths, "/api/enrollments/revenue", "get");
        assertRequiresToken(paths, "/api/approval-requests", "post");
        assertRequiresToken(paths, "/api/approval-requests/{id}/approve", "post");
        assertRequiresToken(paths, "/instructor", "get");
        assertRequiresToken(paths, "/instructor/courses", "get");
    }

    @Test
    void noDejaElHeaderAuthorizationComoParametroDeNingunEndpoint() throws Exception {
        JsonNode paths = doc().get("paths");

        Iterator<Map.Entry<String, JsonNode>> pathEntries = paths.fields();
        while (pathEntries.hasNext()) {
            JsonNode operations = pathEntries.next().getValue();
            Iterator<Map.Entry<String, JsonNode>> methodEntries = operations.fields();
            while (methodEntries.hasNext()) {
                JsonNode operation = methodEntries.next().getValue();
                JsonNode parameters = operation.get("parameters");
                if (parameters == null) {
                    continue;
                }
                assertThat(parameters.toString()).doesNotContain("\"Authorization\"");
            }
        }
    }

    @Test
    void publicaElSpecConLaInfoDeLaApiYElEsquemaDelToken() throws Exception {
        JsonNode doc = doc();

        assertThat(doc.get("info").get("title").asText()).isEqualTo("PM2026 - API de la academia");
        JsonNode esquema = doc.get("components").get("securitySchemes").get(OpenApiConfig.ESQUEMA_TOKEN);
        assertThat(esquema.get("type").asText()).isEqualTo("apiKey");
        assertThat(esquema.get("in").asText()).isEqualTo("header");
        assertThat(esquema.get("name").asText()).isEqualTo("Authorization");
    }

    @Test
    void documentaLosCodigosHttpYElEsquemaProblemDetail() throws Exception {
        JsonNode doc = doc();
        JsonNode altaAlumno = operationOf(doc.get("paths"), "/api/students", "post").get("responses");
        assertThat(altaAlumno.has("201")).isTrue();
        assertThat(altaAlumno.get("201").get("headers").get("Location").get("schema").get("format").asText())
                .isEqualTo("uri");
        assertProblemResponse(altaAlumno, "400");
        assertProblemResponse(altaAlumno, "409");
        assertProblemResponse(altaAlumno, "405");
        assertProblemResponse(altaAlumno, "406");
        assertProblemResponse(altaAlumno, "415");
        assertProblemResponse(altaAlumno, "500");

        JsonNode altaCurso = operationOf(doc.get("paths"), "/api/courses", "post").get("responses");
        assertProblemResponse(altaCurso, "404");
        JsonNode altaInscripcion = operationOf(doc.get("paths"), "/api/enrollments", "post").get("responses");
        assertProblemResponse(altaInscripcion, "404");
        JsonNode altaAprobacion = operationOf(doc.get("paths"), "/api/approval-requests", "post").get("responses");
        assertProblemResponse(altaAprobacion, "404");
        JsonNode alumnosDeCurso = operationOf(doc.get("paths"), "/api/students", "get").get("responses");
        assertProblemResponse(alumnosDeCurso, "404");
        JsonNode cursosDeAlumno = operationOf(doc.get("paths"), "/api/courses", "get").get("responses");
        assertProblemResponse(cursosDeAlumno, "400");
        assertProblemResponse(cursosDeAlumno, "401");
        assertProblemResponse(cursosDeAlumno, "403");
        assertProblemResponse(cursosDeAlumno, "404");
        assertThat(operationOf(doc.get("paths"), "/api/courses", "get").get("security")).isNull();

        JsonNode consultaProtegida = operationOf(doc.get("paths"), "/api/students/{id}", "get").get("responses");
        assertProblemResponse(consultaProtegida, "401");
        assertProblemResponse(consultaProtegida, "403");
        assertProblemResponse(consultaProtegida, "404");

        JsonNode borrado = operationOf(doc.get("paths"), "/api/students/{id}", "delete").get("responses");
        assertThat(borrado.has("204")).isTrue();
        assertProblemResponse(borrado, "409");

        JsonNode login = operationOf(doc.get("paths"), "/student/auth", "post").get("responses");
        assertProblemResponse(login, "401");

        JsonNode legacyAlta = operationOf(doc.get("paths"), "/instructor", "post").get("responses");
        assertThat(legacyAlta.get("201").get("headers").has("Location")).isTrue();

        JsonNode schema = doc.get("components").get("schemas").get("ProblemDetail");
        assertThat(schema).isNotNull();
        JsonNode properties = schema.get("properties");
        for (String property : List.of("type", "title", "status", "detail", "instance", "code", "timestamp",
                "errors")) {
            assertThat(properties.has(property)).as("falta la propiedad " + property).isTrue();
        }
    }

    @Test
    void cadaOperacionExponeExactamenteLosCodigosDeLaMatriz() throws Exception {
        JsonNode paths = doc().get("paths");
        List<OpenApiResponseExpectation> matrix = List.of(
                expected("/api/students", "get", "200", "400", "401", "403", "404", "405", "406", "500"),
                expected("/api/students/{id}", "get", "200", "400", "401", "403", "404", "405", "406", "500"),
                expected("/api/students", "post", "201", "400", "405", "406", "409", "415", "500"),
                expected("/api/students/{id}", "put", "200", "400", "401", "403", "404", "405", "406", "409",
                        "415", "500"),
                expected("/api/students/{id}", "delete", "204", "400", "401", "403", "404", "405", "406", "409",
                        "500"),
                expected("/api/instructors", "get", "200", "405", "406", "500"),
                expected("/api/instructors/{id}", "get", "200", "400", "404", "405", "406", "500"),
                expected("/api/instructors/{id}/specialties", "get", "200", "400", "404", "405", "406", "500"),
                expected("/api/instructors/{id}/revenue", "get", "200", "400", "401", "403", "404", "405", "406",
                        "500"),
                expected("/api/instructors", "post", "201", "400", "401", "403", "405", "406", "409", "415", "500"),
                expected("/api/instructors/{id}", "put", "200", "400", "401", "403", "404", "405", "406", "409",
                        "415", "500"),
                expected("/api/instructors/{id}", "delete", "204", "400", "401", "403", "404", "405", "406", "409",
                        "500"),
                expected("/api/instructors/{id}/specialties/{specialtyId}", "put", "204", "400", "401", "403",
                        "404", "405", "406", "409", "500"),
                expected("/api/instructors/{id}/specialties/{specialtyId}", "delete", "204", "400", "401", "403",
                        "404", "405", "406", "409", "500"),
                expected("/api/administrators", "get", "200", "401", "403", "405", "406", "500"),
                expected("/api/administrators/{id}", "get", "200", "400", "401", "403", "404", "405", "406",
                        "500"),
                expected("/api/administrators", "post", "201", "400", "401", "403", "405", "406", "409", "415",
                        "500"),
                expected("/api/administrators/{id}", "put", "200", "400", "401", "403", "404", "405", "406",
                        "409", "415", "500"),
                expected("/api/administrators/{id}", "delete", "204", "400", "401", "403", "404", "405", "406",
                        "409", "500"),
                expected("/api/courses", "get", "200", "400", "401", "403", "404", "405", "406", "500"),
                expected("/api/courses/{id}", "get", "200", "400", "404", "405", "406", "500"),
                expected("/api/courses", "post", "201", "400", "401", "403", "404", "405", "406", "409", "415",
                        "500"),
                expected("/api/courses/{id}", "put", "200", "400", "401", "403", "404", "405", "406", "409",
                        "415", "500"),
                expected("/api/courses/{id}", "delete", "204", "400", "401", "403", "404", "405", "406", "409",
                        "500"),
                expected("/api/specialties", "get", "200", "405", "406", "500"),
                expected("/api/specialties/{id}", "get", "200", "400", "404", "405", "406", "500"),
                expected("/api/specialties", "post", "201", "400", "401", "403", "405", "406", "409", "415", "500"),
                expected("/api/specialties/{id}", "put", "200", "400", "401", "403", "404", "405", "406", "409",
                        "415", "500"),
                expected("/api/specialties/{id}", "delete", "204", "400", "401", "403", "404", "405", "406",
                        "409", "500"),
                expected("/api/enrollments", "get", "200", "401", "403", "405", "406", "500"),
                expected("/api/enrollments/revenue", "get", "200", "401", "403", "405", "406", "500"),
                expected("/api/enrollments/{id}", "get", "200", "400", "401", "403", "404", "405", "406", "500"),
                expected("/api/enrollments", "post", "201", "400", "401", "403", "404", "405", "406", "409",
                        "415", "500"),
                expected("/api/approval-requests", "get", "200", "400", "401", "403", "405", "406", "500"),
                expected("/api/approval-requests/{id}", "get", "200", "400", "401", "403", "404", "405", "406",
                        "500"),
                expected("/api/approval-requests", "post", "201", "400", "401", "403", "404", "405", "406", "409",
                        "415", "500"),
                expected("/api/approval-requests/{id}/approve", "post", "200", "400", "401", "403", "404", "405",
                        "406", "409", "415", "500"),
                expected("/api/approval-requests/{id}/reject", "post", "200", "400", "401", "403", "404", "405",
                        "406", "409", "415", "500"),
                expected("/student/auth", "post", "200", "400", "401", "405", "406", "415", "500"),
                expected("/instructor/auth", "post", "200", "400", "401", "405", "406", "415", "500"),
                expected("/administrator/auth", "post", "200", "400", "401", "405", "406", "415", "500"),
                expected("/instructor", "post", "201", "400", "401", "403", "405", "406", "409", "415", "500"),
                expected("/instructor", "get", "200", "401", "403", "405", "406", "500"),
                expected("/instructor/courses", "get", "200", "401", "403", "405", "406", "500"));

        Set<String> expectedOperations = new HashSet<>();
        for (OpenApiResponseExpectation expectation : matrix) {
            expectedOperations.add(expectation.method() + " " + expectation.path());
            JsonNode responses = operationOf(paths, expectation.path(), expectation.method()).get("responses");
            Set<String> actual = new HashSet<>();
            responses.fieldNames().forEachRemaining(actual::add);
            assertThat(actual).as(expectation.method() + " " + expectation.path())
                    .containsExactlyInAnyOrderElementsOf(expectation.codes());
            if (expectation.codes().contains("201")) {
                assertThat(responses.get("201").get("headers").get("Location").get("schema").get("format").asText())
                        .as(expectation.method() + " " + expectation.path() + " debe documentar Location")
                        .isEqualTo("uri");
            }
        }

        Set<String> documentedOperations = new HashSet<>();
        Iterator<Map.Entry<String, JsonNode>> pathEntries = paths.fields();
        while (pathEntries.hasNext()) {
            Map.Entry<String, JsonNode> pathEntry = pathEntries.next();
            Iterator<String> methodNames = pathEntry.getValue().fieldNames();
            while (methodNames.hasNext()) {
                String method = methodNames.next();
                if (HTTP_METHODS.contains(method)) {
                    documentedOperations.add(method + " " + pathEntry.getKey());
                }
            }
        }
        assertThat(expectedOperations).containsExactlyInAnyOrderElementsOf(documentedOperations);
    }

    private static OpenApiResponseExpectation expected(String path, String method, String... codes) {
        return new OpenApiResponseExpectation(path, method, Set.of(codes));
    }

    private record OpenApiResponseExpectation(String path, String method, Set<String> codes) {
    }

    private void assertProblemResponse(JsonNode responses, String code) {
        JsonNode response = responses.get(code);
        assertThat(response).as("falta documentar la respuesta HTTP " + code).isNotNull();
        JsonNode mediaType = response.get("content").get("application/problem+json");
        assertThat(mediaType).as("la respuesta " + code + " debe usar ProblemDetail").isNotNull();
        assertThat(mediaType.get("schema").get("$ref").asText())
                .isEqualTo("#/components/schemas/ProblemDetail");
    }

    private JsonNode doc() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return MAPPER.readTree(body);
    }

    private void assertPublic(JsonNode paths, String path, String method) {
        JsonNode security = operationOf(paths, path, method).get("security");
        assertThat(security).as(path + " " + method + " deberia ser publico").isNull();
    }

    private void assertRequiresToken(JsonNode paths, String path, String method) {
        JsonNode security = operationOf(paths, path, method).get("security");
        assertThat(security).as(path + " " + method + " deberia pedir token").isNotNull();
        assertThat(security.toString()).as(path + " " + method).contains(OpenApiConfig.ESQUEMA_TOKEN);
    }

    private JsonNode operationOf(JsonNode paths, String path, String method) {
        JsonNode documentedPath = paths.get(path);
        assertThat(documentedPath).as("la ruta " + path + " no esta documentada").isNotNull();
        JsonNode operation = documentedPath.get(method);
        assertThat(operation).as("el metodo " + method + " de " + path + " no esta documentado")
                .isNotNull();
        return operation;
    }
}
