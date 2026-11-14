package ar.edu.unnoba.pm2026.shared.observability;

import ar.edu.unnoba.pm2026.auth.exception.AuthenticationException;
import ar.edu.unnoba.pm2026.auth.exception.AuthorizationException;
import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.shared.security.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

// /actuator queda solo para administradores (#28), salvo /actuator/health, que lo usan los chequeos
// de disponibilidad (Docker, Render, Prometheus) y no tienen token. Corre como filtro porque los
// endpoints de Actuator no pasan por los controllers, donde se hace el resto de los chequeos.
public class ActuatorAccessFilter extends OncePerRequestFilter {

    private static final String ACTUATOR = "/actuator";
    private static final String HEALTH = ACTUATOR + "/health";

    private final AuthorizationService authorizationService;
    private final UrlPathHelper urlPathHelper = new UrlPathHelper();

    public ActuatorAccessFilter(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = normalizedPath(request);
        if (!isUnder(path, ACTUATOR) || isUnder(path, HEALTH)) {
            chain.doFilter(request, response);
            return;
        }
        try {
            authorizationService.authorize(request.getHeader(HttpHeaders.AUTHORIZATION), Role.ADMINISTRATOR);
        } catch (AuthenticationException exception) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, exception.getMessage());
            return;
        } catch (AuthorizationException exception) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, exception.getMessage());
            return;
        }
        chain.doFilter(request, response);
    }

    // Decodifica, saca "//" y resuelve "..": "/actuator/health/../prometheus" no puede pasar como health
    private String normalizedPath(HttpServletRequest request) {
        return StringUtils.cleanPath(urlPathHelper.getPathWithinApplication(request));
    }

    private static boolean isUnder(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    // Mismo formato que ApiExceptionHandler: {"mensaje": "..."}
    private static void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"mensaje\":\"" + escapeJson(message) + "\"}");
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
