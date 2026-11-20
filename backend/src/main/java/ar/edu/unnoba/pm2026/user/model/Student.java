package ar.edu.unnoba.pm2026.user.model;

import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.course.model.Course;
import ar.edu.unnoba.pm2026.course.model.CourseStatus;
import ar.edu.unnoba.pm2026.course.model.Specialty;
import ar.edu.unnoba.pm2026.enrollment.exception.CourseEnrollmentException;
import ar.edu.unnoba.pm2026.enrollment.model.Enrollment;
import ar.edu.unnoba.pm2026.shared.exception.ErrorCode;
import ar.edu.unnoba.pm2026.shared.exception.ValidationException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "students")
public class Student extends User {

    private static final BigDecimal LOYALTY_DISCOUNT = new BigDecimal("0.25");
    private static final BigDecimal FULL_PRICE_FACTOR = BigDecimal.ONE.subtract(LOYALTY_DISCOUNT);

    @Getter
    @Setter
    @Column(nullable = false, unique = true)
    private String dni;

    // Lado inverso de la relacion: lo ignoramos para evitar ciclos al serializar a JSON
    @JsonIgnore
    @OneToMany(mappedBy = "student")
    private Set<Enrollment> enrollments = new HashSet<>();

    // Constrctor vacio que exige JPA
    public Student() {
        super();
    }

    public Student(String firstName, String lastName, String email, String password, String dni) {
        super(firstName, lastName, email, password);
        this.dni = dni;
    }

    @Override
    public Role getRole() {
        return Role.STUDENT;
    }

    public Set<Enrollment> getEnrollments() {
        return Collections.unmodifiableSet(enrollments);
    }

    public Enrollment applyCourse(Course course) {
        return applyCourse(course, LocalDate.now());
    }

    Enrollment applyCourse(Course course, LocalDate enrollmentDate) {
        if (course == null) {
            throw new ValidationException(ErrorCode.ENROLLMENT_COURSE_REQUIRED);
        }
        if (enrollmentDate == null) {
            throw new ValidationException(ErrorCode.ENROLLMENT_DATE_REQUIRED);
        }
        CourseEnrollmentException rejection = rejectionFor(course, enrollmentDate);
        if (rejection != null) {
            throw rejection;
        }
        return new Enrollment(this, course, priceFor(course, enrollmentDate), enrollmentDate.atStartOfDay());
    }

    // TP6 (#42): los cursos "disponibles" son los que pasarian todas las reglas de applyCourse en esa fecha
    public boolean canApplyTo(Course course, LocalDate date) {
        return rejectionFor(course, date) == null;
    }

    // Precio que pagaria en esa fecha: 25% menos si ya se inscribio a otro curso de la misma especialidad en
    // el mismo mes. Es el que cobra applyCourse, asi el precio que se muestra es el que se paga.
    public BigDecimal priceFor(Course course, LocalDate date) {
        BigDecimal basePrice = course.getBasePrice();
        if (basePrice == null) {
            throw new CourseEnrollmentException(ErrorCode.ENROLLMENT_COURSE_PRICE_MISSING);
        }
        if (hasPreviousEnrollmentForSpecialtyInMonth(course, date)) {
            return basePrice.multiply(FULL_PRICE_FACTOR).setScale(2, RoundingMode.HALF_UP);
        }
        return basePrice;
    }

    public boolean isEnrolledIn(Course course) {
        return enrollments.stream().anyMatch(enrollment -> enrollment.getCourse() == course
                || (enrollment.getCourse().getId() != null
                && enrollment.getCourse().getId().equals(course.getId())));
    }

    // La primera regla que no se cumple, o null si se puede inscribir (mismo orden de siempre)
    private CourseEnrollmentException rejectionFor(Course course, LocalDate enrollmentDate) {
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            return new CourseEnrollmentException(ErrorCode.ENROLLMENT_COURSE_NOT_PUBLISHED);
        }
        if (course.getStartDate() == null || !enrollmentDate.isBefore(course.getStartDate())) {
            return new CourseEnrollmentException(ErrorCode.ENROLLMENT_REGISTRATION_CLOSED);
        }
        if (!course.hasAvailableCapacity()) {
            return new CourseEnrollmentException(ErrorCode.ENROLLMENT_COURSE_FULL);
        }
        if (course.getSpecialty() == null) {
            return new CourseEnrollmentException(ErrorCode.ENROLLMENT_COURSE_SPECIALTY_MISSING);
        }
        if (isEnrolledIn(course)) {
            return new CourseEnrollmentException(ErrorCode.ENROLLMENT_ALREADY_ENROLLED);
        }
        if (course.getBasePrice() == null) {
            return new CourseEnrollmentException(ErrorCode.ENROLLMENT_COURSE_PRICE_MISSING);
        }
        return null;
    }

    private boolean hasPreviousEnrollmentForSpecialtyInMonth(Course course, LocalDate enrollmentDate) {
        return enrollments.stream().anyMatch(enrollment -> {
            LocalDate previousEnrollmentDate = enrollment.getEnrollmentDate().toLocalDate();
            return previousEnrollmentDate.getYear() == enrollmentDate.getYear()
                    && previousEnrollmentDate.getMonth() == enrollmentDate.getMonth()
                    && isSameSpecialty(enrollment.getCourse().getSpecialty(), course.getSpecialty());
        });
    }

    private boolean isSameSpecialty(Specialty first, Specialty second) {
        if (first == second) {
            return true;
        }
        return first != null && second != null
                && first.getId() != null
                && first.getId().equals(second.getId());
    }

    public void addEnrollment(Enrollment enrollment) {
        if (enrollment != null && this.enrollments.add(enrollment) && enrollment.getStudent() != this) {
            enrollment.setStudent(this);
        }
    }

    public void removeEnrollment(Enrollment enrollment) {
        this.enrollments.remove(enrollment);
    }
}
