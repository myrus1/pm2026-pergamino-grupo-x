package com.example.demo.course.model;

import com.example.demo.enrollment.model.Enrollment;
import com.example.demo.user.model.Instructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "courses")
@Getter
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false)
    private String title;

    @Setter
    private String description;

    @Setter
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Setter
    @Column(nullable = false)
    private LocalDate startDate;

    @Setter
    @Column(nullable = false)
    private LocalDate endDate;

    @Setter
    @Column(nullable = false)
    private Integer maxCapacity;

    // Estado del curso guardado como String en la base de datos
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseStatus status = CourseStatus.DRAFT;

    // Especialidad del curso
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialty_id", nullable = false)
    private Specialty specialty;

    // Relacion en la que muchos cursos pertenecen a un instructor
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private Instructor instructor;

    // Lado inverso de la relacion: lo ignoramos para evitar ciclos al serializar a JSON
    @JsonIgnore
    @OneToMany(mappedBy = "course")
    private Set<Enrollment> enrollments = new HashSet<>();

    protected Course() {
    }

    public Course(String title, String description, BigDecimal basePrice,
                  LocalDate startDate, LocalDate endDate, Integer maxCapacity,
                  Specialty specialty, Instructor instructor) {
        this.title = Objects.requireNonNull(title, "El titulo no puede ser nulo");
        this.description = description;
        this.basePrice = Objects.requireNonNull(basePrice, "El precio base no puede ser nulo");
        if (basePrice.signum() < 0) {
            throw new IllegalArgumentException("El precio base no puede ser negativo");
        }
        this.startDate = Objects.requireNonNull(startDate, "La fecha de inicio no puede ser nula");
        this.endDate = Objects.requireNonNull(endDate, "La fecha de fin no puede ser nula");
        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("La fecha de fin debe ser posterior a la de inicio");
        }
        this.maxCapacity = Objects.requireNonNull(maxCapacity, "La capacidad no puede ser nula");
        if (maxCapacity <= 0) {
            throw new IllegalArgumentException("La capacidad debe ser mayor a cero");
        }
        setSpecialty(specialty);
        setInstructor(instructor);
    }

    public void setSpecialty(Specialty specialty) {
        if (this.specialty == specialty) {
            return;
        }
        Objects.requireNonNull(specialty, "La especialidad no puede ser nula");
        Specialty previousSpecialty = this.specialty;
        this.specialty = specialty;
        if (previousSpecialty != null) {
            previousSpecialty.removeCourse(this);
        }
        specialty.addCourse(this);
    }

    public void setInstructor(Instructor instructor) {
        if (this.instructor == instructor) {
            return;
        }
        Objects.requireNonNull(instructor, "Instructor no puede ser nulo");
        Instructor previousInstructor = this.instructor;
        this.instructor = instructor;
        if (previousInstructor != null) {
            previousInstructor.removeCourse(this);
        }
        instructor.addCourse(this);
    }

    public void setStatus(CourseStatus status) {
        this.status = Objects.requireNonNull(status, "Estado no puede ser nulo");
    }

    public Set<Enrollment> getEnrollments() {
        return Collections.unmodifiableSet(enrollments);
    }

    public boolean hasAvailableCapacity() {
        return maxCapacity != null && enrollments.size() < maxCapacity;
    }

    public void addEnrollment(Enrollment enrollment) {
        if (enrollment != null && enrollments.add(enrollment) && enrollment.getCourse() != this) {
            enrollment.setCourse(this);
        }
    }

    public void removeEnrollment(Enrollment enrollment) {
        enrollments.remove(enrollment);
    }
}
