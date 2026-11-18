package ar.edu.unnoba.pm2026.testutil;

import ar.edu.unnoba.pm2026.course.model.Course;
import ar.edu.unnoba.pm2026.course.model.CourseStatus;
import ar.edu.unnoba.pm2026.course.model.Specialty;
import ar.edu.unnoba.pm2026.user.model.Administrator;
import ar.edu.unnoba.pm2026.user.model.Instructor;
import ar.edu.unnoba.pm2026.user.model.Student;
import java.math.BigDecimal;
import java.time.LocalDate;

// Entidades con id para los tests de los endpoints del TP6, sin repetir los mismos constructores.
public final class Fixtures {

    public static final LocalDate START = LocalDate.of(2026, 6, 1);
    public static final LocalDate END = LocalDate.of(2026, 6, 20);

    private Fixtures() {
    }

    public static Specialty specialty(Long id, String name) {
        Specialty specialty = new Specialty(name);
        TestIds.setId(specialty, id);
        return specialty;
    }

    public static Instructor instructor(Long id) {
        Instructor instructor = new Instructor("Iris", "Ruiz", "iris@example.com", "secret");
        TestIds.setId(instructor, id);
        return instructor;
    }

    public static Student student(Long id) {
        Student student = new Student("Ana", "Diaz", "ana" + id + "@example.com", "secret", "3000000" + id);
        TestIds.setId(student, id);
        return student;
    }

    public static Administrator administrator(Long id) {
        Administrator administrator = new Administrator("Eva", "Sosa", "eva@example.com", "secret");
        TestIds.setId(administrator, id);
        return administrator;
    }

    // Curso de 100.00 con cupo 5; la especialidad se le agrega al instructor para que el curso sea valido
    public static Course course(Long id, Instructor instructor, Specialty specialty) {
        return course(id, instructor, specialty, START);
    }

    public static Course course(Long id, Instructor instructor, Specialty specialty, LocalDate start) {
        instructor.addSpecialty(specialty);
        Course course = new Course("Curso de Java", "Desc", new BigDecimal("100.00"), start, start.plusDays(19), 5,
                specialty, instructor);
        TestIds.setId(course, id);
        return course;
    }

    public static Course course(Long id, CourseStatus status) {
        Course course = course(id, instructor(2L), specialty(1L, "Programacion"));
        course.setStatus(status);
        return course;
    }
}
