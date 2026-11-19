package ar.edu.unnoba.pm2026.shared.exception;

// Codigos de error de la API. Cada constante es, a la vez, la clave del mensaje en
// messages.properties (es) y messages_en.properties (en). El texto no vive en el codigo:
// ApiExceptionHandler lo resuelve con MessageSource segun el Accept-Language de la request.
public enum ErrorCode {

    // Solicitud (ProblemDetail)
    REQUEST_VALIDATION("request.validation"),
    REQUEST_MALFORMED("request.malformed"),
    REQUEST_MISSING_PARAMETER("request.missing-parameter"),
    REQUEST_INVALID_PARAMETER("request.invalid-parameter"),
    REQUEST_INVALID_DATE_RANGE("request.invalid-date-range"),
    REQUEST_BODY_REQUIRED("request.body.required"),
    REQUEST_ACTOR_REQUIRED("request.actor-required"),

    // HTTP e infraestructura
    HTTP_NOT_FOUND("http.not-found"),
    HTTP_METHOD_NOT_ALLOWED("http.method-not-allowed"),
    HTTP_UNSUPPORTED_MEDIA_TYPE("http.unsupported-media-type"),
    HTTP_NOT_ACCEPTABLE("http.not-acceptable"),
    HTTP_400("http.400"),
    HTTP_401("http.401"),
    HTTP_403("http.403"),
    HTTP_404("http.404"),
    HTTP_405("http.405"),
    HTTP_406("http.406"),
    HTTP_409("http.409"),
    HTTP_415("http.415"),
    HTTP_500("http.500"),
    DATA_INTEGRITY_CONFLICT("data.integrity.conflict"),
    DATA_INTEGRITY_INVALID("data.integrity.invalid"),
    INTERNAL_ERROR("internal.error"),
    RESOURCE_NOT_FOUND("resource.not-found"),

    // Autenticacion y autorizacion
    AUTHENTICATION_INVALID_CREDENTIALS("authentication.invalid-credentials"),
    AUTHENTICATION_INVALID_TOKEN("authentication.invalid-token"),
    AUTHENTICATION_UNAUTHENTICATED("authentication.unauthenticated"),
    AUTHORIZATION_DENIED("authorization.denied"),
    AUTHORIZATION_FORBIDDEN("authorization.forbidden"),
    AUTHORIZATION_ROLE_REQUIRED("authorization.role-required"),
    AUTHORIZATION_USER_NOT_FOUND("authorization.user-not-found"),
    AUTHORIZATION_INSTRUCTOR_ONLY("authorization.instructor-only"),
    AUTHORIZATION_INSTRUCTOR_PROFILE_NOT_OWNER("authorization.instructor-profile-not-owner"),
    AUTHORIZATION_COURSE_NOT_OWNER("authorization.course-not-owner"),
    AUTHORIZATION_COURSES_OF_OTHERS("authorization.courses-of-others"),
    AUTHORIZATION_REVENUE_NOT_OWNER("authorization.revenue-not-owner"),
    AUTHORIZATION_STUDENT_ONLY("authorization.student-only"),
    AUTHORIZATION_ENROLLMENT_NOT_SELF("authorization.enrollment-not-self"),
    AUTHORIZATION_ENROLLMENT_NOT_OWNER("authorization.enrollment-not-owner"),
    AUTHORIZATION_ENROLLMENT_NOT_IN_YOUR_COURSES("authorization.enrollment-not-in-your-courses"),
    AUTHORIZATION_APPROVAL_INSTRUCTOR_ONLY("authorization.approval-instructor-only"),
    AUTHORIZATION_APPROVAL_REQUEST_NOT_OWNER("authorization.approval-request-not-owner"),

    // Cursos
    COURSE_TITLE_REQUIRED("course.title.required"),
    COURSE_TITLE_BLANK("course.title.blank"),
    COURSE_BASE_PRICE_REQUIRED("course.base-price.required"),
    COURSE_BASE_PRICE_NEGATIVE("course.base-price.negative"),
    COURSE_START_DATE_REQUIRED("course.start-date.required"),
    COURSE_END_DATE_REQUIRED("course.end-date.required"),
    COURSE_END_DATE_BEFORE_START("course.end-date.before-start"),
    COURSE_CAPACITY_REQUIRED("course.capacity.required"),
    COURSE_CAPACITY_POSITIVE("course.capacity.positive"),
    COURSE_SPECIALTY_REQUIRED("course.specialty.required"),
    COURSE_INSTRUCTOR_REQUIRED("course.instructor.required"),
    COURSE_STATUS_REQUIRED("course.status.required"),
    COURSE_NOT_FOUND("course.not-found"),
    COURSE_NOT_DRAFT("course.not-draft"),
    SPECIALTY_NOT_FOUND("specialty.not-found"),

    // Usuarios
    USER_REQUIRED("user.required"),
    STUDENT_NOT_FOUND("student.not-found"),
    INSTRUCTOR_NOT_FOUND("instructor.not-found"),
    ADMINISTRATOR_NOT_FOUND("administrator.not-found"),
    INSTRUCTOR_EMAIL_ALREADY_EXISTS("instructor.email.already-exists"),
    INSTRUCTOR_SPECIALTY_REQUIRED("instructor.specialty.required"),
    INSTRUCTOR_SPECIALTY_MISMATCH("instructor.specialty.mismatch"),
    ADMINISTRATOR_SELF_DELETION("administrator.self-deletion"),
    ADMINISTRATOR_EMAIL_TAKEN("administrator.email-taken"),

    // Inscripciones
    ENROLLMENT_NOT_FOUND("enrollment.not-found"),
    ENROLLMENT_STUDENT_REQUIRED("enrollment.student.required"),
    ENROLLMENT_COURSE_REQUIRED("enrollment.course.required"),
    ENROLLMENT_DATE_REQUIRED("enrollment.date.required"),
    ENROLLMENT_COURSE_PRICE_MISSING("enrollment.course-price-missing"),
    ENROLLMENT_COURSE_NOT_PUBLISHED("enrollment.course-not-published"),
    ENROLLMENT_REGISTRATION_CLOSED("enrollment.registration-closed"),
    ENROLLMENT_COURSE_FULL("enrollment.course-full"),
    ENROLLMENT_COURSE_SPECIALTY_MISSING("enrollment.course-specialty-missing"),
    ENROLLMENT_ALREADY_ENROLLED("enrollment.already-enrolled"),

    // Solicitudes de aprobacion
    APPROVAL_REQUEST_NOT_FOUND("approval-request.not-found"),
    APPROVAL_REQUEST_COURSE_REQUIRED("approval-request.course.required"),
    APPROVAL_REQUEST_COURSE_NOT_DRAFT("approval-request.course-not-draft"),
    APPROVAL_REQUEST_ADMINISTRATOR_REQUIRED("approval-request.administrator.required"),
    APPROVAL_REQUEST_ALREADY_RESOLVED("approval-request.already-resolved"),
    APPROVAL_REQUEST_INVALID_DECISION("approval-request.invalid-decision");

    private final String key;

    ErrorCode(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
