package com.example.demo.enrollment.services;

import com.example.demo.common.Rol;
import com.example.demo.common.UsuarioAutenticado;
import com.example.demo.course.model.Course;
import com.example.demo.course.services.CourseService;
import com.example.demo.enrollment.model.Enrollment;
import com.example.demo.enrollment.repositories.EnrollmentRepository;
import com.example.demo.user.model.Student;
import com.example.demo.user.services.InstructorService;
import com.example.demo.user.services.StudentService;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentService studentService;
    private final CourseService courseService;
    private final InstructorService instructorService;

    public EnrollmentService(EnrollmentRepository enrollmentRepository, StudentService studentService,
            CourseService courseService, InstructorService instructorService) {
        this.enrollmentRepository = enrollmentRepository;
        this.studentService = studentService;
        this.courseService = courseService;
        this.instructorService = instructorService;
    }

    @Transactional(readOnly = true)
    public List<Student> getStudentsOfCourse(Long courseId) {
        courseService.getById(courseId);
        return enrollmentRepository.findStudentsByCourseId(courseId);
    }

    @Transactional(readOnly = true)
    public List<Course> getCoursesOfStudent(Long studentId) {
        studentService.getById(studentId);
        return enrollmentRepository.findCoursesByStudentId(studentId);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalRevenue() {
        return enrollmentRepository.findAll().stream()
                .map(Enrollment::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public BigDecimal instructorRevenue(Long instructorId) {
        instructorService.getById(instructorId);
        return enrollmentRepository.findByCourseInstructorId(instructorId).stream()
                .map(Enrollment::instructorRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Enrollment enroll(Long studentId, Long courseId) {
        Student student = studentService.getById(studentId);
        Course course = courseService.getById(courseId);
        Enrollment enrollment = student.applyCourse(course);
        return enrollmentRepository.save(enrollment);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> getAll() {
        return enrollmentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Enrollment getById(Long id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inscripcion no encontrada con id " + id));
    }

    // El alumno solo ve su inscripcion; el instructor solo las de sus cursos; el admin, cualquiera.
    @Transactional(readOnly = true)
    public Enrollment getById(Long id, UsuarioAutenticado actor) {
        Enrollment enrollment = getById(id);
        switch (actor.rol()) {
            case ALUMNO -> actor.exigirPropio(enrollment.getStudent().getId(),
                    "La inscripcion no le pertenece");
            case INSTRUCTOR -> actor.exigirPropio(enrollment.getCourse().getInstructor().getId(),
                    "La inscripcion no le pertenece a sus cursos");
            default -> {
                // El administrador puede consultar cualquier inscripcion.
            }
        }
        return enrollment;
    }

    // Listados filtrados por quien pide: cada rol ve solo lo que le corresponde.
    @Transactional(readOnly = true)
    public List<Enrollment> getAllDeAlumno(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> getAllDeInstructor(Long instructorId) {
        return enrollmentRepository.findByCourseInstructorId(instructorId);
    }
}
