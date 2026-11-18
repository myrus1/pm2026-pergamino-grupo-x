package ar.edu.unnoba.pm2026.shared.observability;

import ar.edu.unnoba.pm2026.shared.security.Role;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

// Metricas de negocio (#28). Se ven en /actuator/prometheus como:
//   pm2026_auth_logins_total{role="STUDENT|INSTRUCTOR|ADMINISTRATOR", result="success|failure"}
//   pm2026_enrollments_total
//   pm2026_course_approvals_total{result="approved|rejected"}
// Los contadores se registran en cero al arrancar para que aparezcan aunque todavia no haya eventos.
// (Prometheus reserva el sufijo "_created" en los contadores: por eso es pm2026_enrollments y no
// pm2026_enrollments_created.)
@Component
public class BusinessMetrics {

    private final Map<Role, Counter> successfulLogins = new EnumMap<>(Role.class);
    private final Map<Role, Counter> failedLogins = new EnumMap<>(Role.class);
    private final Counter enrollmentsCreated;
    private final Counter coursesApproved;
    private final Counter coursesRejected;

    public BusinessMetrics(MeterRegistry registry) {
        for (Role role : Role.values()) {
            successfulLogins.put(role, loginCounter(registry, role, "success"));
            failedLogins.put(role, loginCounter(registry, role, "failure"));
        }
        enrollmentsCreated = Counter.builder("pm2026.enrollments")
                .description("Inscripciones creadas")
                .register(registry);
        coursesApproved = approvalCounter(registry, "approved");
        coursesRejected = approvalCounter(registry, "rejected");
    }

    public void loginSucceeded(Role role) {
        successfulLogins.get(role).increment();
    }

    public void loginFailed(Role role) {
        failedLogins.get(role).increment();
    }

    public void enrollmentCreated() {
        afterCommit(enrollmentsCreated::increment);
    }

    public void courseApproved() {
        afterCommit(coursesApproved::increment);
    }

    public void courseRejected() {
        afterCommit(coursesRejected::increment);
    }

    private static Counter loginCounter(MeterRegistry registry, Role role, String result) {
        return Counter.builder("pm2026.auth.logins")
                .description("Intentos de login por rol y resultado")
                .tag("role", role.name())
                .tag("result", result)
                .register(registry);
    }

    private static Counter approvalCounter(MeterRegistry registry, String result) {
        return Counter.builder("pm2026.course.approvals")
                .description("Solicitudes de aprobacion de cursos resueltas por el administrador")
                .tag("result", result)
                .register(registry);
    }

    // Dentro de una transaccion se cuenta recien cuando se confirma, asi un rollback no suma.
    private static void afterCommit(Runnable increment) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            increment.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                increment.run();
            }
        });
    }
}
