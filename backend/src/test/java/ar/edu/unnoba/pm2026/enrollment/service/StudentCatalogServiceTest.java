package ar.edu.unnoba.pm2026.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import ar.edu.unnoba.pm2026.course.model.Course;
import ar.edu.unnoba.pm2026.course.model.CourseStatus;
import ar.edu.unnoba.pm2026.course.model.Specialty;
import ar.edu.unnoba.pm2026.course.service.CourseService;
import ar.edu.unnoba.pm2026.enrollment.dto.StudentCourseResponse;
import ar.edu.unnoba.pm2026.enrollment.model.Enrollment;
import ar.edu.unnoba.pm2026.shared.exception.ResourceNotFoundException;
import ar.edu.unnoba.pm2026.testutil.Fixtures;
import ar.edu.unnoba.pm2026.user.model.Instructor;
import ar.edu.unnoba.pm2026.user.model.Student;
import ar.edu.unnoba.pm2026.user.service.StudentService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudentCatalogServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 5, 10);

    @Mock
    private StudentService studentService;

    @Mock
    private CourseService courseService;

    private StudentCatalogService studentCatalogService;

    private final Instructor iris = Fixtures.instructor(2L);
    private final Specialty programacion = Fixtures.specialty(1L, "Programacion");
    private Student ana;

    @BeforeEach
    void setUp() {
        studentCatalogService = new StudentCatalogService(studentService, courseService);
        ana = Fixtures.student(1L);
        lenient().when(studentService.getById(1L)).thenReturn(ana);
    }

    private Course published(Long id, LocalDate start, int capacity) {
        Course course = Fixtures.course(id, iris, programacion, start);
        course.setStatus(CourseStatus.PUBLISHED);
        course.setMaxCapacity(capacity);
        return course;
    }

    @Test
    void availableDejaAfueraLosLlenosYLosQueYaCursa() {
        Course libre = published(7L, LocalDate.of(2026, 6, 1), 5);
        Course lleno = published(8L, LocalDate.of(2026, 6, 1), 1);
        new Enrollment(Fixtures.student(2L), lleno, new BigDecimal("100.00"));
        Course suyo = published(9L, LocalDate.of(2026, 6, 1), 5);
        new Enrollment(ana, suyo, new BigDecimal("100.00"), LocalDateTime.of(2026, 4, 1, 0, 0));
        when(courseService.getPublishedStartingAfter(HOY)).thenReturn(List.of(libre, lleno, suyo));

        List<StudentCourseResponse> available = studentCatalogService.getAvailableCourses(1L, HOY);

        assertThat(available).extracting(StudentCourseResponse::id).containsExactly(7L);
        assertThat(available.get(0).availableSeats()).isEqualTo(5);
        assertThat(available.get(0).enrolled()).isFalse();
    }

    @Test
    void availableMuestraElPrecioConDescuentoDeFidelidad() {
        Course cursado = published(9L, LocalDate.of(2026, 6, 1), 5);
        new Enrollment(ana, cursado, new BigDecimal("100.00"), LocalDateTime.of(2026, 5, 3, 0, 0));
        Course otro = published(7L, LocalDate.of(2026, 6, 1), 5);
        when(courseService.getPublishedStartingAfter(HOY)).thenReturn(List.of(otro));

        StudentCourseResponse course = studentCatalogService.getAvailableCourses(1L, HOY).get(0);

        assertThat(course.basePrice()).isEqualByComparingTo("100.00");
        assertThat(course.applicablePrice()).isEqualByComparingTo("75.00");
        assertThat(course.loyaltyDiscount()).isTrue();
    }

    @Test
    void getCourseIndicaSiYaEstaInscripto() {
        Course suyo = published(9L, LocalDate.of(2026, 6, 1), 5);
        new Enrollment(ana, suyo, new BigDecimal("100.00"), LocalDateTime.of(2026, 4, 1, 0, 0));
        when(courseService.getById(9L)).thenReturn(suyo);

        StudentCourseResponse detail = studentCatalogService.getCourse(1L, 9L, HOY);

        assertThat(detail.enrolled()).isTrue();
        assertThat(detail.availableSeats()).isEqualTo(4);
        assertThat(detail.instructor().name()).isEqualTo("Iris");
    }

    @Test
    void getCourseDeUnCursoSinPublicarEsNotFound() {
        Course borrador = Fixtures.course(7L, iris, programacion, LocalDate.of(2026, 6, 1));
        when(courseService.getById(7L)).thenReturn(borrador);

        assertThatThrownBy(() -> studentCatalogService.getCourse(1L, 7L, HOY))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Curso no encontrado con id 7");
    }
}
