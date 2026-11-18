package ar.edu.unnoba.pm2026.shared.observability;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.unnoba.pm2026.shared.security.Role;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

// Metricas de negocio: se registran en cero al arrancar (asi aparecen en Prometheus desde el inicio)
// y cada evento suma uno en su contador.
class BusinessMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final BusinessMetrics metrics = new BusinessMetrics(registry);

    @AfterEach
    void cleanTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void registraTodosLosContadoresEnCeroAlArrancar() {
        for (Role role : Role.values()) {
            assertThat(logins(role, "success")).isZero();
            assertThat(logins(role, "failure")).isZero();
        }
        assertThat(registry.get("pm2026.enrollments").counter().count()).isZero();
        assertThat(approvals("approved")).isZero();
        assertThat(approvals("rejected")).isZero();
    }

    @Test
    void cuentaLosLoginsPorRolYResultado() {
        metrics.loginSucceeded(Role.STUDENT);
        metrics.loginSucceeded(Role.STUDENT);
        metrics.loginFailed(Role.ADMINISTRATOR);

        assertThat(logins(Role.STUDENT, "success")).isEqualTo(2);
        assertThat(logins(Role.ADMINISTRATOR, "failure")).isEqualTo(1);
        assertThat(logins(Role.INSTRUCTOR, "success")).isZero();
    }

    @Test
    void cuentaInscripcionesYCursosAprobadosYRechazados() {
        metrics.enrollmentCreated();
        metrics.courseApproved();
        metrics.courseRejected();
        metrics.courseRejected();

        assertThat(registry.get("pm2026.enrollments").counter().count()).isEqualTo(1);
        assertThat(approvals("approved")).isEqualTo(1);
        assertThat(approvals("rejected")).isEqualTo(2);
    }

    // Dentro de una transaccion, la inscripcion se cuenta recien cuando se confirma:
    // si la transaccion hace rollback no se cuenta
    @Test
    void dentroDeUnaTransaccionCuentaRecienDespuesDelCommit() {
        TransactionSynchronizationManager.initSynchronization();

        metrics.enrollmentCreated();
        assertThat(registry.get("pm2026.enrollments").counter().count()).isZero();

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        assertThat(registry.get("pm2026.enrollments").counter().count()).isEqualTo(1);
    }

    @Test
    void siLaTransaccionHaceRollbackNoCuenta() {
        TransactionSynchronizationManager.initSynchronization();
        AtomicBoolean anySynchronization = new AtomicBoolean();

        metrics.courseApproved();
        TransactionSynchronizationManager.getSynchronizations().forEach(synchronization -> {
            anySynchronization.set(true);
            synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        });

        assertThat(anySynchronization).isTrue();
        assertThat(approvals("approved")).isZero();
    }

    private double logins(Role role, String result) {
        return registry.get("pm2026.auth.logins").tag("role", role.name()).tag("result", result).counter().count();
    }

    private double approvals(String result) {
        return registry.get("pm2026.course.approvals").tag("result", result).counter().count();
    }
}
