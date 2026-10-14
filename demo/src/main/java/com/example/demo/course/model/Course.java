package com.example.demo.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;

@Entity
@Table(name = "courses")
@Getter
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

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

    // Relación en la qe muchos cursos pertenecen a un instructor
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private Instructor instructor;

    @OneToMany(mappedBy = "course")
    private Set<Enrollment> enrollments = new HashSet<>();

    protected Course() {
    }

    Course(String title, String description, BigDecimal basePrice,
           LocalDate startDate, LocalDate endDate, Integer maxCapacity,
           Specialty specialty, Instructor instructor) {
        this.title = title;
        this.description = description;
        this.basePrice = basePrice;
        this.startDate = startDate;
        this.endDate = endDate;
        this.maxCapacity = maxCapacity;
        setSpecialty(specialty);
        setInstructor(instructor);
    }

    void setSpecialty(Specialty specialty) {
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

    void setInstructor(Instructor instructor) {
        if (this.instructor == instructor) {
            return;
        }
        Objects.requireNonNull(instructor, "Instructor n puede ser nulo");
        Instructor previousInstructor = this.instructor;
        this.instructor = instructor;
        if (previousInstructor != null) {
            previousInstructor.removeCourse(this);
        }
        instructor.addCourse(this);
    }

    public Set<Enrollment> getEnrollments() {
        return Collections.unmodifiableSet(enrollments);
    }

    public boolean hasAvailableCapacity() {
        return maxCapacity != null && enrollments.size() < maxCapacity;
    }

    void setStatus(CourseStatus status) {
        this.status = Objects.requireNonNull(status, "Estado no pede ser nulo");
    }

    void addEnrollment(Enrollment enrollment) {
        if (enrollment != null && enrollments.add(enrollment) && enrollment.getCourse() != this) {
            enrollment.setCourse(this);
        }
    }

    void removeEnrollment(Enrollment enrollment) {
        enrollments.remove(enrollment);
    }
}