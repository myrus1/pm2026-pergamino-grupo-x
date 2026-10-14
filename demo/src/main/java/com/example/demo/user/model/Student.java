package com.example.demo.user.model;

import com.example.demo.course.model.Course;
import com.example.demo.course.model.CourseStatus;
import com.example.demo.course.model.Specialty;
import com.example.demo.enrollment.model.CourseEnrollmentException;
import com.example.demo.enrollment.model.Enrollment;
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
import java.util.Objects;
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

    public Set<Enrollment> getEnrollments() {
        return Collections.unmodifiableSet(enrollments);
    }

    public Enrollment applyCourse(Course course) {
        return applyCourse(course, LocalDate.now());
    }

    Enrollment applyCourse(Course course, LocalDate enrollmentDate) {
        Objects.requireNonNull(course, "curso no puede ser nulo");
        Objects.requireNonNull(enrollmentDate, "La fecha de inscripcion no puede ser nula");

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new CourseEnrollmentException("El curso no esta publicado");
        }
        if (course.getStartDate() == null || !enrollmentDate.isBefore(course.getStartDate())) {
            throw new CourseEnrollmentException("La inscripcion debe ser antes de que comience el curso");
        }
        if (!course.hasAvailableCapacity()) {
            throw new CourseEnrollmentException("El curso esta lleno");
        }
        if (course.getSpecialty() == null) {
            throw new CourseEnrollmentException("Especialidad del curso no definida");
        }
        if (enrollments.stream().anyMatch(enrollment -> enrollment.getCourse() == course
                || (enrollment.getCourse().getId() != null
                && enrollment.getCourse().getId().equals(course.getId())))) {
            throw new CourseEnrollmentException("Estudiante inscripto en el curso");
        }

        BigDecimal finalAmount = course.getBasePrice();
        if (finalAmount == null) {
            throw new CourseEnrollmentException("No se establecio el precio base del curso");
        }
        if (hasPreviousEnrollmentForSpecialtyInMonth(course, enrollmentDate)) {
            finalAmount = finalAmount.multiply(FULL_PRICE_FACTOR).setScale(2, RoundingMode.HALF_UP);
        }

        return new Enrollment(this, course, finalAmount, enrollmentDate.atStartOfDay());
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
