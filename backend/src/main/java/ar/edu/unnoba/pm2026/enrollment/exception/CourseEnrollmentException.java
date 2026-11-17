package ar.edu.unnoba.pm2026.enrollment.exception;

import ar.edu.unnoba.pm2026.shared.exception.BusinessRuleException;

public class CourseEnrollmentException extends BusinessRuleException {

    public CourseEnrollmentException(String message) {
        super(message);
    }
}
