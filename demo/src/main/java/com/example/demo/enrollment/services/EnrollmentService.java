package com.example.demo.enrollment.services;

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

    public Enrollment save(Enrollment enrollment) {
        return enrollmentRepository.save(enrollment);
    }

    public void delete(Long id) {
        enrollmentRepository.deleteById(id);
    }
}
