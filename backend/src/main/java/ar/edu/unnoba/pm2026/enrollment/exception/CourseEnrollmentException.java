package ar.edu.unnoba.pm2026.enrollment.exception;

import ar.edu.unnoba.pm2026.shared.exception.BusinessRuleException;
import ar.edu.unnoba.pm2026.shared.exception.ErrorCode;

public class CourseEnrollmentException extends BusinessRuleException {

    public CourseEnrollmentException(ErrorCode code, Object... arguments) {
        super(code, arguments);
    }
}
