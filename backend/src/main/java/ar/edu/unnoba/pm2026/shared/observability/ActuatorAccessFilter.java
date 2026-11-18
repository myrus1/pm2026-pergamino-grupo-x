package ar.edu.unnoba.pm2026.shared.observability;

import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import ar.edu.unnoba.pm2026.shared.exception.ApiException;
import ar.edu.unnoba.pm2026.shared.security.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.util.UrlPathHelper;

// /actuator queda solo para administradores (#28), salvo /actuator/health, que lo usan los chequeos
// de disponibilidad (Docker, Render, Prometheus) y no tienen token. Corre como filtro porque los
// endpoints de Actuator no pasan por los controllers, donde se hace el resto de los chequeos.
public class ActuatorAccessFilter extends OncePerRequestFilter {

    private static final String ACTUATOR = "/actuator";
    private static final String HEALTH = ACTUATOR + "/health";

    private final AuthorizationService authorizationService;
    private final HandlerExceptionResolver exceptionResolver;
    private final UrlPathHelper urlPathHelper = new UrlPathHelper();

    // El 401/403 lo arma ApiExceptionHandler a traves del resolver de Spring MVC, asi sale con el
    // mismo ProblemDetail (y el mismo code) que el resto de la API en vez de un JSON armado aca.
    public ActuatorAccessFilter(AuthorizationService authorizationService, HandlerExceptionResolver exceptionResolver) {
        this.authorizationService = authorizationService;
        this.exceptionResolver = exceptionResolver;
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
        } catch (ApiException exception) {
            if (exceptionResolver.resolveException(request, response, null, exception) == null) {
                throw exception;
            }
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
}
