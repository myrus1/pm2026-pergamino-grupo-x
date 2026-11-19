package ar.edu.unnoba.pm2026.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

// Los mensajes al usuario viven en messages.properties (es, default) y messages_en.properties (en).
// Este test mantiene los dos bundles en sincronia con los ErrorCode y las claves de Bean Validation:
// si falta una traduccion o sobra una clave vieja, falla antes de llegar a produccion.
class MessageBundlesTest {

    private static final Set<String> SPANISH = load("messages.properties");
    private static final Set<String> ENGLISH = load("messages_en.properties");

    @Test
    void losDosBundlesTienenLasMismasClaves() {
        assertThat(ENGLISH).containsExactlyInAnyOrderElementsOf(SPANISH);
    }

    @Test
    void ningunMensajeEstaVacio() {
        for (String bundle : new String[] { "messages.properties", "messages_en.properties" }) {
            Properties properties = read(bundle);
            assertThat(properties.stringPropertyNames())
                    .allSatisfy(key -> assertThat(properties.getProperty(key)).as(bundle + ":" + key).isNotBlank());
        }
    }

    @Test
    void todosLosErrorCodeTienenMensajeEnLosDosIdiomas() {
        for (ErrorCode code : ErrorCode.values()) {
            assertThat(SPANISH).as("es:" + code.key()).contains(code.key());
            assertThat(ENGLISH).as("en:" + code.key()).contains(code.key());
        }
    }

    @Test
    void lasClavesDeBeanValidationTienenMensajeEnLosDosIdiomas() {
        Set<String> validationKeys = Stream.concat(
                ApiExceptionHandler.VALIDATION_KEYS.values().stream(),
                Stream.of(ApiExceptionHandler.VALIDATION_INVALID))
                .collect(Collectors.toSet());

        for (String key : validationKeys) {
            assertThat(SPANISH).as("es:" + key).contains(key);
            assertThat(ENGLISH).as("en:" + key).contains(key);
        }
    }

    @Test
    void noHayClavesHuerfanas() {
        Set<String> used = Stream.concat(
                Arrays.stream(ErrorCode.values()).map(ErrorCode::key),
                Stream.concat(
                        ApiExceptionHandler.VALIDATION_KEYS.values().stream(),
                        Stream.of(ApiExceptionHandler.VALIDATION_INVALID)))
                .collect(Collectors.toSet());

        assertThat(SPANISH)
                .as("claves sin ErrorCode ni uso en el handler")
                .allSatisfy(key -> assertThat(isUsed(key, used)).as(key).isTrue());
    }

    // El handler tambien resuelve claves armadas como "http." + status (fallback de ProblemDetail).
    private static boolean isUsed(String key, Set<String> used) {
        return used.contains(key) || key.matches("http\\.\\d{3}");
    }

    private static Set<String> load(String resource) {
        return read(resource).stringPropertyNames();
    }

    private static Properties read(String resource) {
        Properties properties = new Properties();
        try (InputStream stream = MessageBundlesTest.class.getResourceAsStream("/" + resource)) {
            if (stream == null) {
                throw new IllegalStateException("No se encontro " + resource + " en el classpath");
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer " + resource, exception);
        }
        return properties;
    }
}
