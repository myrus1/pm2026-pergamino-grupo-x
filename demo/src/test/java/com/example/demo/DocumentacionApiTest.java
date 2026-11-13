package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.common.OpenApiConfig;
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
class DocumentacionApiTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Test
    void documentaComoPublicosLosEndpointsQueNoPidenToken() throws Exception {
        JsonNode paths = doc().get("paths");

        // Registro publico y logins
        exigirPublico(paths, "/api/students", "post");
        exigirPublico(paths, "/student/auth", "post");
        exigirPublico(paths, "/instructor/auth", "post");
        exigirPublico(paths, "/administrator/auth", "post");

        // Consultas publicas (hacen falta para mostrar los cursos)
        exigirPublico(paths, "/api/courses", "get");
        exigirPublico(paths, "/api/courses/{id}", "get");
        exigirPublico(paths, "/api/instructors", "get");
        exigirPublico(paths, "/api/instructors/{id}", "get");
        exigirPublico(paths, "/api/specialties", "get");
        exigirPublico(paths, "/api/specialties/{id}", "get");
    }

    @Test
    void documentaElTokenEnLosEndpointsProtegidos() throws Exception {
        JsonNode paths = doc().get("paths");

        exigirToken(paths, "/api/students", "get");
        exigirToken(paths, "/api/students/{id}", "put");
        exigirToken(paths, "/api/students/{id}", "delete");
        exigirToken(paths, "/api/courses", "post");
        exigirToken(paths, "/api/courses/{id}", "put");
        exigirToken(paths, "/api/instructors", "post");
        exigirToken(paths, "/api/instructors/{id}/revenue", "get");
        exigirToken(paths, "/api/specialties", "post");
        exigirToken(paths, "/api/administrators", "get");
        exigirToken(paths, "/api/enrollments", "post");
        exigirToken(paths, "/api/enrollments/revenue", "get");
        exigirToken(paths, "/api/approval-requests", "post");
        exigirToken(paths, "/api/approval-requests/{id}/approve", "post");
        exigirToken(paths, "/instructor", "get");
        exigirToken(paths, "/instructor/courses", "get");
    }

    @Test
    void noDejaElHeaderAuthorizationComoParametroDeNingunEndpoint() throws Exception {
        JsonNode paths = doc().get("paths");

        Iterator<Map.Entry<String, JsonNode>> rutas = paths.fields();
        while (rutas.hasNext()) {
            JsonNode operaciones = rutas.next().getValue();
            Iterator<Map.Entry<String, JsonNode>> metodos = operaciones.fields();
            while (metodos.hasNext()) {
                JsonNode operacion = metodos.next().getValue();
                JsonNode parametros = operacion.get("parameters");
                if (parametros == null) {
                    continue;
                }
                assertThat(parametros.toString()).doesNotContain("\"Authorization\"");
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
        String cuerpo = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return MAPPER.readTree(cuerpo);
    }

    private void exigirPublico(JsonNode paths, String ruta, String metodo) {
        JsonNode seguridad = operacionDe(paths, ruta, metodo).get("security");
        assertThat(seguridad).as(ruta + " " + metodo + " deberia ser publico").isNull();
    }

    private void exigirToken(JsonNode paths, String ruta, String metodo) {
        JsonNode seguridad = operacionDe(paths, ruta, metodo).get("security");
        assertThat(seguridad).as(ruta + " " + metodo + " deberia pedir token").isNotNull();
        assertThat(seguridad.toString()).as(ruta + " " + metodo).contains(OpenApiConfig.ESQUEMA_TOKEN);
    }

    private JsonNode operacionDe(JsonNode paths, String ruta, String metodo) {
        JsonNode rutaDocumentada = paths.get(ruta);
        assertThat(rutaDocumentada).as("la ruta " + ruta + " no esta documentada").isNotNull();
        JsonNode operacion = rutaDocumentada.get(metodo);
        assertThat(operacion).as("el metodo " + metodo + " de " + ruta + " no esta documentado")
                .isNotNull();
        return operacion;
    }
}
