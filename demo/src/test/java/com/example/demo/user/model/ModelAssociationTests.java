package com.example.demo.user.model;

import com.example.demo.course.model.Course;
import com.example.demo.course.model.CourseStatus;
import com.example.demo.course.model.Specialty;
import com.example.demo.enrollment.model.CourseEnrollmentException;
import com.example.demo.enrollment.model.Enrollment;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/* Test creado con IA que cubre los siguientes casos:
        - Inscripción válida en un curso publicado antes de su fecha de inicio.
        - Rechazo de cursos no publicados o que ya comenzaron.
        - Control del cupo y prevención de inscripciones duplicadas.
        - Descuento del 25 % por otra inscripción de la misma especialidad en el mismo mes y año; verifica también que no se aplique en el mismo mes de otro año.
        - Creación de cursos por el instructor solo para una especialidad que posee; comprueba que se creen como `DRAFT` y queden asociados al instructor y a la especialidad.
        - Uso de la fecha actual en la operación pública `Student.applyCourse(Course)`.
        - Sincronización de asociaciones cuando se reasignan cursos, estudiantes o especialidades, y protección de las colecciones contra modificaciones directas.
        - Declaración de la precisión monetaria (`BigDecimal`, precisión 12 y escala 2) y de la restricción única por estudiante y curso. */

class ModelAssociationTests {

    @Test
    void studentEnrollsInPublishedCourseBeforeItsStartDate() {
        LocalDate enrollmentDate = LocalDate.of(2026, 10, 1);
        Student student = new Student();
        Course course = publishedCourse("Java", new Specialty("Programming"), new BigDecimal("100.00"),
                enrollmentDate.plusDays(1), 2);

        Enrollment enrollment = student.applyCourse(course, enrollmentDate);

        assertEquals(new BigDecimal("100.00"), enrollment.getFinalAmount());
        assertEquals(enrollmentDate.atStartOfDay(), enrollment.getEnrollmentDate());
        assertTrue(student.getEnrollments().contains(enrollment));
        assertTrue(course.getEnrollments().contains(enrollment));
        assertThrows(UnsupportedOperationException.class, () -> course.getEnrollments().clear());
    }

    @Test
    void studentGetsLoyaltyDiscountForSameSpecialtyInSameMonthAndYear() {
        LocalDate firstEnrollmentDate = LocalDate.of(2026, 10, 1);
        LocalDate secondEnrollmentDate = LocalDate.of(2026, 10, 10);
        Specialty specialty = new Specialty("Programming");
        Course firstCourse = publishedCourse("Java", specialty, new BigDecimal("100.00"),
                firstEnrollmentDate.plusDays(1), 2);
        Course secondCourse = publishedCourse("Cypress", specialty, new BigDecimal("100.00"),
                secondEnrollmentDate.plusDays(1), 2);
        Student student = new Student();

        student.applyCourse(firstCourse, firstEnrollmentDate);
        Enrollment discountedEnrollment = student.applyCourse(secondCourse, secondEnrollmentDate);

        assertEquals(new BigDecimal("75.00"), discountedEnrollment.getFinalAmount());
    }

    @Test
    void loyaltyDiscountDoesNotCarryOverToTheSameMonthInAnotherYear() {
        Specialty specialty = new Specialty("Programming");
        Course previousYearCourse = publishedCourse("Java", specialty, new BigDecimal("100.00"),
                LocalDate.of(2025, 10, 2), 2);
        Course currentYearCourse = publishedCourse("Cypress", specialty, new BigDecimal("100.00"),
                LocalDate.of(2026, 10, 2), 2);
        Student student = new Student();

        student.applyCourse(previousYearCourse, LocalDate.of(2025, 10, 1));
        Enrollment enrollment = student.applyCourse(currentYearCourse, LocalDate.of(2026, 10, 1));

        assertEquals(new BigDecimal("100.00"), enrollment.getFinalAmount());
    }

    @Test
    void studentCannotEnrollWhenCourseIsNotPublishedOrAlreadyStarted() {
        Student student = new Student();
        Specialty specialty = new Specialty("Programming");
        Course draftCourse = new Course("Java", "Course description", new BigDecimal("100.00"),
                LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 20), 2, specialty, new Instructor());
        Course startedCourse = publishedCourse("Cypress", specialty, new BigDecimal("100.00"),
                LocalDate.of(2026, 10, 1), 2);

        assertThrows(CourseEnrollmentException.class,
                () -> student.applyCourse(draftCourse, LocalDate.of(2026, 10, 1)));
        assertThrows(CourseEnrollmentException.class,
                () -> student.applyCourse(startedCourse, LocalDate.of(2026, 10, 1)));
        assertTrue(student.getEnrollments().isEmpty());
    }

    @Test
    void studentCannotExceedCourseCapacityOrEnrollTwice() {
        Student firstStudent = new Student();
        Student secondStudent = new Student();
        LocalDate enrollmentDate = LocalDate.of(2026, 10, 1);
        Course oneSeatCourse = publishedCourse("Java", new Specialty("Programming"),
                new BigDecimal("100.00"), enrollmentDate.plusDays(1), 1);
        firstStudent.applyCourse(oneSeatCourse, enrollmentDate);

        assertThrows(CourseEnrollmentException.class,
                () -> secondStudent.applyCourse(oneSeatCourse, enrollmentDate));

        Course twoSeatCourse = publishedCourse("Cypress", new Specialty("Testing"),
                new BigDecimal("100.00"), enrollmentDate.plusDays(1), 2);
        firstStudent.applyCourse(twoSeatCourse, enrollmentDate);

        assertThrows(CourseEnrollmentException.class,
                () -> firstStudent.applyCourse(twoSeatCourse, enrollmentDate));
        assertEquals(1, twoSeatCourse.getEnrollments().size());
    }

    @Test
    void instructorCreatesDraftCourseAndOwnsIt() {
        Instructor instructor = new Instructor();
        Specialty specialty = new Specialty("Programming");
        instructor.addSpecialty(specialty);

        Course createdCourse = instructor.createCourse("Java", "Course description", new BigDecimal("120.00"),
                LocalDate.now(), LocalDate.now().plusDays(30), 20, specialty);

        assertEquals(1, instructor.getCourses().size());
        Course course = instructor.getCourses().iterator().next();
        assertSame(createdCourse, course);
        assertEquals("Java", course.getTitle());
        assertEquals(CourseStatus.DRAFT, course.getStatus());
        assertSame(instructor, course.getInstructor());
        assertSame(specialty, course.getSpecialty());
        assertTrue(specialty.getCourses().contains(course));
    }

    @Test
    void publicApplyCourseUsesTheCurrentSystemDate() {
        Student student = new Student();
        Course course = publishedCourse("Java", new Specialty("Programming"), new BigDecimal("100.00"),
                LocalDate.now().plusDays(1), 2);

        Enrollment enrollment = student.applyCourse(course);

        assertEquals(LocalDate.now().atStartOfDay(), enrollment.getEnrollmentDate());
    }

    @Test
    void instructorCannotCreateCourseWithUnownedSpecialty() {
        Instructor instructor = new Instructor();
        Specialty specialty = new Specialty("Design");

        assertThrows(IllegalArgumentException.class, () ->
                instructor.createCourse("Design", "Course description", new BigDecimal("50.00"),
                        LocalDate.now(), LocalDate.now().plusDays(30), 10, specialty));
        assertTrue(instructor.getCourses().isEmpty());
    }

    @Test
    void courseConstructorAndSettersKeepAssociationsInSync() {
        Instructor firstInstructor = new Instructor();
        Instructor secondInstructor = new Instructor();
        Specialty firstSpecialty = new Specialty("Programming");
        Specialty secondSpecialty = new Specialty("Design");
        Course course = new Course("Java", "Course description", new BigDecimal("120.00"),
                LocalDate.now(), LocalDate.now().plusDays(30), 20, firstSpecialty, firstInstructor);

        assertTrue(firstInstructor.getCourses().contains(course));
        assertTrue(firstSpecialty.getCourses().contains(course));

        course.setInstructor(secondInstructor);
        course.setSpecialty(secondSpecialty);

        assertFalse(firstInstructor.getCourses().contains(course));
        assertFalse(firstSpecialty.getCourses().contains(course));
        assertTrue(secondInstructor.getCourses().contains(course));
        assertTrue(secondSpecialty.getCourses().contains(course));
        assertThrows(UnsupportedOperationException.class, () -> secondInstructor.getCourses().clear());
        assertThrows(UnsupportedOperationException.class, () -> secondSpecialty.getCourses().clear());

        firstSpecialty.addInstructor(firstInstructor);
        assertTrue(firstInstructor.getSpecialties().contains(firstSpecialty));
        assertTrue(firstSpecialty.getInstructors().contains(firstInstructor));
        assertThrows(UnsupportedOperationException.class, () -> firstInstructor.getSpecialties().clear());
        assertThrows(UnsupportedOperationException.class, () -> firstSpecialty.getInstructors().clear());
    }

    @Test
    void enrollmentKeepsStudentCollectionInSyncWhenReassigned() {
        Student firstStudent = new Student();
        Student secondStudent = new Student();
        Course course = new Course("Java", "Course description", new BigDecimal("99.95"),
                LocalDate.now(), LocalDate.now().plusDays(30), 1,
                new Specialty("Programming"), new Instructor());
        Enrollment enrollment = new Enrollment(firstStudent, course, new BigDecimal("99.95"));

        assertTrue(firstStudent.getEnrollments().contains(enrollment));
        enrollment.setStudent(secondStudent);

        assertFalse(firstStudent.getEnrollments().contains(enrollment));
        assertTrue(secondStudent.getEnrollments().contains(enrollment));
        assertThrows(UnsupportedOperationException.class, () -> secondStudent.getEnrollments().clear());
        assertThrows(NullPointerException.class, () -> enrollment.setStudent(null));
        assertTrue(secondStudent.getEnrollments().contains(enrollment));
        enrollment.setStudent(firstStudent);
        assertTrue(secondStudent.getEnrollments().isEmpty());
        assertTrue(firstStudent.getEnrollments().contains(enrollment));
    }

    @Test
    void monetaryColumnsAndEnrollmentPairConstraintAreDeclared() throws ReflectiveOperationException {
        assertMoneyColumn(Course.class, "basePrice");
        assertMoneyColumn(Enrollment.class, "finalAmount");

        Table table = Enrollment.class.getAnnotation(Table.class);
        assertTrue(Arrays.stream(table.uniqueConstraints())
                .map(UniqueConstraint::columnNames)
                .anyMatch(columns -> Arrays.equals(columns, new String[]{"student_id", "course_id"})));
    }

    private void assertMoneyColumn(Class<?> entityType, String fieldName) throws ReflectiveOperationException {
        Field field = entityType.getDeclaredField(fieldName);
        Column column = field.getAnnotation(Column.class);

        assertEquals(BigDecimal.class, field.getType());
        assertEquals(12, column.precision());
        assertEquals(2, column.scale());
    }

    private Course publishedCourse(String title, Specialty specialty, BigDecimal price,
                                   LocalDate startDate, int capacity) {
        Instructor instructor = new Instructor();
        instructor.addSpecialty(specialty);
        Course course = new Course(title, "Course description", price, startDate, startDate.plusDays(30),
                capacity, specialty, instructor);
        course.setStatus(CourseStatus.PUBLISHED);
        return course;
    }
}
