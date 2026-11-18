package ar.edu.unnoba.pm2026.shared.observability;

import ar.edu.unnoba.pm2026.auth.service.AuthorizationService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

// Registra los filtros de observabilidad con un orden fijo: primero el correlationId (asi hasta un
// 401 de /actuator lleva su id) y despues el control de acceso a /actuator.
@Configuration
public class ObservabilityConfig {

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {
        FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>(new CorrelationIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<ActuatorAccessFilter> actuatorAccessFilter(AuthorizationService authorizationService) {
        FilterRegistrationBean<ActuatorAccessFilter> registration =
                new FilterRegistrationBean<>(new ActuatorAccessFilter(authorizationService));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
