package ar.edu.unnoba.pm2026.shared.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

// Le asigna a cada request un correlationId (#28): lo toma del header X-Correlation-Id si el cliente
// manda uno valido o genera uno nuevo. Lo devuelve en la respuesta y lo deja en el MDC, asi aparece
// en todas las lineas de log de esa request. Al terminar loguea metodo, ruta, estado y duracion.
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);
    // Solo caracteres seguros y largo acotado: un id con saltos de linea podria inventar lineas de log
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = correlationIdOf(request);
        long start = System.nanoTime();
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            logRequest(request, response, start);
            MDC.remove(MDC_KEY);
        }
    }

    private String correlationIdOf(HttpServletRequest request) {
        String received = request.getHeader(HEADER);
        if (received != null && VALID_ID.matcher(received).matches()) {
            return received;
        }
        return UUID.randomUUID().toString();
    }

    // Las consultas a /actuator (p. ej. Prometheus cada 15 s) no se loguean para no tapar el resto
    private void logRequest(HttpServletRequest request, HttpServletResponse response, long start) {
        String path = request.getRequestURI();
        if (path.startsWith("/actuator")) {
            return;
        }
        long millis = (System.nanoTime() - start) / 1_000_000;
        log.info("{} {} -> {} ({} ms)", request.getMethod(), path, response.getStatus(), millis);
    }
}
