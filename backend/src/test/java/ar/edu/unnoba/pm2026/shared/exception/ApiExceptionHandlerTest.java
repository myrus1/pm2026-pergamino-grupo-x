package ar.edu.unnoba.pm2026.shared.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = ApiExceptionHandlerTest.ErrorTestController.class)
@Import({ ApiExceptionHandler.class, ApiExceptionHandlerTest.ErrorTestConfiguration.class })
class ApiExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unaExcepcionDeDominioRespondeProblemDetail() throws Exception {
        mockMvc.perform(get("/test/errors/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("No se encontro el recurso"))
                .andExpect(jsonPath("$.instance").value("/test/errors/not-found"))
                .andExpect(jsonPath("$.code").value("resource.not-found"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.mensaje").doesNotExist());
    }

    @Test
    void erroresDeValidacionIncluyenCampoYMensaje() throws Exception {
        mockMvc.perform(post("/test/errors/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("request.validation"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("El campo 'name' es obligatorio"));
    }

    @Test
    void lasExcepcionesComunesConservanEstadoYCodigoEstable() throws Exception {
        mockMvc.perform(get("/test/errors/unauthenticated"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication.unauthenticated"));

        mockMvc.perform(get("/test/errors/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("authorization.forbidden"));

        mockMvc.perform(get("/test/errors/business"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("enrollment.course-full"));

        mockMvc.perform(get("/test/errors/validation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("request.validation"));
    }

    @Test
    void erroresDelProtocoloSeDevuelvenComoProblemDetail() throws Exception {
        mockMvc.perform(get("/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("http.not-found"))
                .andExpect(jsonPath("$.instance").value("/no-existe"));

        mockMvc.perform(post("/test/errors/not-found"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("http.method-not-allowed"))
                .andExpect(jsonPath("$.status").value(405));

        mockMvc.perform(post("/test/errors/validated")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("name"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("http.unsupported-media-type"))
                .andExpect(jsonPath("$.status").value(415));

        mockMvc.perform(post("/test/errors/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("request.malformed"))
                .andExpect(jsonPath("$.errors").doesNotExist());

        mockMvc.perform(get("/test/errors/not-acceptable").accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isNotAcceptable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("http.not-acceptable"))
                .andExpect(jsonPath("$.status").value(406));

        mockMvc.perform(delete("/test/errors/validated"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("http.method-not-allowed"));
    }

    @Test
    void elEspanolEsElIdiomaPorDefecto() throws Exception {
        mockMvc.perform(get("/test/errors/not-found"))
                .andExpect(jsonPath("$.detail").value("No se encontro el recurso"));

        mockMvc.perform(get("/test/errors/not-found").header("Accept-Language", "es"))
                .andExpect(jsonPath("$.detail").value("No se encontro el recurso"));

        mockMvc.perform(get("/test/errors/business").header("Accept-Language", "es-AR"))
                .andExpect(jsonPath("$.detail").value("El curso esta lleno"));
    }

    @Test
    void AcceptLanguageEnDevuelveLosMensajesEnIngles() throws Exception {
        mockMvc.perform(get("/test/errors/not-found").header("Accept-Language", "en"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("resource.not-found"))
                .andExpect(jsonPath("$.detail").value("Resource not found"));

        mockMvc.perform(get("/test/errors/business").header("Accept-Language", "en"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("enrollment.course-full"))
                .andExpect(jsonPath("$.detail").value("The course is full"));
    }

    @Test
    void unIdiomaRegionalUsaElBundleDeSuIdioma() throws Exception {
        mockMvc.perform(get("/test/errors/not-found").header("Accept-Language", "en-US"))
                .andExpect(jsonPath("$.detail").value("Resource not found"));
    }

    @Test
    void unIdiomaNoSoportadoCaeAlEspanol() throws Exception {
        mockMvc.perform(get("/test/errors/not-found").header("Accept-Language", "fr"))
                .andExpect(jsonPath("$.detail").value("No se encontro el recurso"));
    }

    @Test
    void losErroresDeValidacionTambienSeTraducen() throws Exception {
        mockMvc.perform(post("/test/errors/validated")
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("request.validation"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("Field 'name' is required"))
                .andExpect(jsonPath("$.detail").value("Field 'name' is required"));
    }

    @Test
    void erroresDeIntegridadSeMuestranSinDetallesTecnicos() throws Exception {
        mockMvc.perform(get("/test/errors/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("data.integrity.conflict"))
                .andExpect(jsonPath("$.detail").value("Los datos ya existen o estan en uso por otro registro"));

        mockMvc.perform(get("/test/errors/invalid-integrity"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("data.integrity.invalid"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void errorInternoNoExponeDetallesDeLaExcepcion() throws Exception {
        mockMvc.perform(get("/test/errors/internal"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("internal.error"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Ocurrio un error inesperado"))
                .andExpect(jsonPath("$.detail", not(containsString("dato-secreto"))));

        mockMvc.perform(get("/test/errors/null-pointer"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("internal.error"))
                .andExpect(jsonPath("$.status").value(500));
    }

    @RestController
    public static class ErrorTestController {

        @GetMapping("/test/errors/not-found")
        void notFound() {
            throw new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        @PostMapping(value = "/test/errors/validated", consumes = MediaType.APPLICATION_JSON_VALUE)
        void validated(@Valid @RequestBody RequiredBody body) {
        }

        @GetMapping("/test/errors/internal")
        void internalError() {
            throw new IllegalStateException("dato-secreto");
        }

        @GetMapping(value = "/test/errors/not-acceptable", produces = MediaType.APPLICATION_JSON_VALUE)
        String notAcceptable() {
            return "ok";
        }

        @GetMapping("/test/errors/invalid-integrity")
        void invalidIntegrity() {
            throw new DataIntegrityViolationException("Datos invalidos", new SQLException("valor invalido", "22000"));
        }

        @GetMapping("/test/errors/null-pointer")
        void nullPointer() {
            throw new NullPointerException("dato-secreto");
        }

        @GetMapping("/test/errors/unauthenticated")
        void unauthenticated() {
            throw new UnauthenticatedException(ErrorCode.AUTHENTICATION_UNAUTHENTICATED);
        }

        @GetMapping("/test/errors/forbidden")
        void forbidden() {
            throw new ForbiddenException(ErrorCode.AUTHORIZATION_FORBIDDEN);
        }

        @GetMapping("/test/errors/business")
        void business() {
            throw new BusinessRuleException(ErrorCode.ENROLLMENT_COURSE_FULL);
        }

        @GetMapping("/test/errors/validation")
        void validation() {
            throw new ValidationException(ErrorCode.REQUEST_VALIDATION);
        }

        @GetMapping("/test/errors/conflict")
        void conflict() {
            throw new DataIntegrityViolationException("Conflicto", new SQLException("duplicado", "23505"));
        }
    }

    record RequiredBody(@NotBlank String name) {
    }

    @TestConfiguration
    static class ErrorTestConfiguration {

        @Bean
        ErrorTestController errorTestController() {
            return new ErrorTestController();
        }
    }
}
