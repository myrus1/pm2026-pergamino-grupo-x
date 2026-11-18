package ar.edu.unnoba.pm2026.enrollment.service;

import ar.edu.unnoba.pm2026.course.dto.CourseDetailResponse;
import ar.edu.unnoba.pm2026.course.dto.SpecialtyResponse;
import ar.edu.unnoba.pm2026.course.model.Course;
import ar.edu.unnoba.pm2026.course.model.CourseStatus;
import ar.edu.unnoba.pm2026.course.service.CourseService;
import ar.edu.unnoba.pm2026.enrollment.dto.StudentCourseResponse;
import ar.edu.unnoba.pm2026.shared.exception.ResourceNotFoundException;
import ar.edu.unnoba.pm2026.user.model.Instructor;
import ar.edu.unnoba.pm2026.user.model.Student;
import ar.edu.unnoba.pm2026.user.service.StudentService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// TP6 (#42): los cursos vistos por el alumno, con el precio que pagaria hoy. Las reglas de "disponible" y
// el precio salen de Student (canApplyTo, priceFor), las mismas que usa la inscripcion.
@Service
@Transactional(readOnly = true)
public class StudentCatalogService {

    private final StudentService studentService;
    private final CourseService courseService;

    public StudentCatalogService(StudentService studentService, CourseService courseService) {
        this.studentService = studentService;
        this.courseService = courseService;
    }

    // Publicados, que no empezaron, con cupo y sin los que ya cursa, del mas proximo al mas lejano
    public List<StudentCourseResponse> getAvailableCourses(Long studentId) {
        return getAvailableCourses(studentId, LocalDate.now());
    }

    List<StudentCourseResponse> getAvailableCourses(Long studentId, LocalDate today) {
        Student student = studentService.getById(studentId);
        return courseService.getPublishedStartingAfter(today).stream()
                .filter(course -> student.canApplyTo(course, today))
                .map(course -> responseOf(course, student, today))
                .toList();
    }

    // 404 si el curso no existe o no esta publicado: para el alumno los borradores no existen
    public StudentCourseResponse getCourse(Long studentId, Long courseId) {
        return getCourse(studentId, courseId, LocalDate.now());
    }

    StudentCourseResponse getCourse(Long studentId, Long courseId, LocalDate today) {
        Course course = courseService.getById(courseId);
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new ResourceNotFoundException("course.not-found", "Curso no encontrado con id " + courseId);
        }
        return responseOf(course, studentService.getById(studentId), today);
    }

    private static StudentCourseResponse responseOf(Course course, Student student, LocalDate today) {
        BigDecimal applicablePrice = student.priceFor(course, today);
        Instructor instructor = course.getInstructor();
        return new StudentCourseResponse(course.getId(), course.getTitle(), course.getDescription(),
                course.getStartDate(), course.getEndDate(), course.getMaxCapacity(), course.availableSeats(),
                course.getBasePrice(), applicablePrice, applicablePrice.compareTo(course.getBasePrice()) < 0,
                student.isEnrolledIn(course),
                new SpecialtyResponse(course.getSpecialty().getId(), course.getSpecialty().getName()),
                new CourseDetailResponse.CourseInstructor(instructor.getId(), instructor.getFirstName(),
                        instructor.getLastName(), instructor.getEmail()));
    }
}
