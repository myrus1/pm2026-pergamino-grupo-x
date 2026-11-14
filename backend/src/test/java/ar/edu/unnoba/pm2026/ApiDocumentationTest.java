package ar.edu.unnoba.pm2026;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.unnoba.pm2026.shared.config.OpenApiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Iterator;
import java.util.Map;
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
