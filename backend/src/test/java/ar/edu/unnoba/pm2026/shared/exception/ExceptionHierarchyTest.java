package ar.edu.unnoba.pm2026.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.unnoba.pm2026.approval.exception.ApprovalRequestException;
import ar.edu.unnoba.pm2026.auth.exception.AuthenticationException;
import ar.edu.unnoba.pm2026.auth.exception.AuthorizationException;
import ar.edu.unnoba.pm2026.course.exception.CourseStateException;
import ar.edu.unnoba.pm2026.enrollment.exception.CourseEnrollmentException;
import ar.edu.unnoba.pm2026.user.exception.InstructorEmailAlreadyExistsException;
import org.junit.jupiter.api.Test;

class ExceptionHierarchyTest {

    @Test
    void excepcionesDeReglaDeNegocioCompartenBase() {
        assertBase(InstructorEmailAlreadyExistsException.class, "BusinessRuleException");
        assertBase(ApprovalRequestException.class, "BusinessRuleException");
        assertBase(CourseEnrollmentException.class, "BusinessRuleException");
        assertBase(CourseStateException.class, "BusinessRuleException");
    }

    @Test
    void excepcionesDeAutenticacionYAutorizacionCompartenSusBases() {
        assertBase(AuthenticationException.class, "UnauthenticatedException");
        assertBase(AuthorizationException.class, "ForbiddenException");
    }

    private void assertBase(Class<?> exceptionType, String expectedBaseName) {
        assertThat(exceptionType.getSuperclass().getSimpleName()).isEqualTo(expectedBaseName);
    }
}
