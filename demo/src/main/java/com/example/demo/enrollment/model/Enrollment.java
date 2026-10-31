package com.example.demo.enrollment.model;

import com.example.demo.course.model.Course;
import com.example.demo.user.model.Student;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "enrollments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_enrollment_student_course", columnNames = {"student_id", "course_id"})
})
@Getter
public class Enrollment {

    private static final BigDecimal INSTRUCTOR_SHARE = new BigDecimal("0.65");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false)
    private LocalDateTime enrollmentDate = LocalDateTime.now();

    @Setter
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal finalAmount;

    // Relacion con el estudiante que realiza la inscripcion
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // Relacion con el curso al que se inscribe un estudiante
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    protected Enrollment() {
    }

    public Enrollment(Student student, Course course, BigDecimal finalAmount) {
        this(student, course, finalAmount, LocalDateTime.now());
    }

    public Enrollment(Student student, Course course, BigDecimal finalAmount, LocalDateTime enrollmentDate) {
        setStudent(student);
        setCourse(course);
        this.finalAmount = finalAmount;
        this.enrollmentDate = enrollmentDate;
    }

    public BigDecimal instructorRevenue() {
        return course.getBasePrice().multiply(INSTRUCTOR_SHARE).setScale(2, RoundingMode.HALF_UP);
    }

    public void setStudent(Student student) {
        if (this.student == student) {
            return;
        }
        Objects.requireNonNull(student, "Estudiante no puede ser nulo");
        Student previousStudent = this.student;
        this.student = student;
        if (previousStudent != null) {
            previousStudent.removeEnrollment(this);
        }
        student.addEnrollment(this);
    }

    public void setCourse(Course course) {
        if (this.course == course) {
            return;
        }
        Objects.requireNonNull(course, "Curso no puede ser nulo");
        Course previousCourse = this.course;
        this.course = course;
        if (previousCourse != null) {
            previousCourse.removeEnrollment(this);
        }
        course.addEnrollment(this);
    }

}
