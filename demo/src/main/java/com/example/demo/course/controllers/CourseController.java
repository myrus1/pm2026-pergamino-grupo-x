package com.example.demo.course.controllers;

import static com.example.demo.common.Validaciones.exigir;

import com.example.demo.course.model.Course;
import com.example.demo.course.services.CourseService;
import com.example.demo.course.dto.CourseRequest;
import com.example.demo.course.dto.CourseResponse;
import com.example.demo.enrollment.services.EnrollmentService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// API REST de cursos: /api/courses
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;
    private final EnrollmentService enrollmentService;

    public CourseController(CourseService courseService, EnrollmentService enrollmentService) {
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
    }

    @GetMapping
    public List<CourseResponse> getAll(@RequestParam(required = false) Long studentId) {
        List<Course> cursos = studentId == null
                ? courseService.getAll()
                : enrollmentService.getCoursesOfStudent(studentId);
        return cursos.stream().map(this::respuesta).toList();
    }

    @GetMapping("/{id}")
    public CourseResponse getById(@PathVariable Long id) {
        return respuesta(courseService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(@RequestBody CourseRequest solicitud) {
        validar(solicitud);
        return respuesta(courseService.create(solicitud));
    }

    @PutMapping("/{id}")
    public CourseResponse update(@PathVariable Long id, @RequestBody CourseRequest solicitud) {
        validar(solicitud);
        return respuesta(courseService.update(id, solicitud));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        courseService.getById(id);
        courseService.delete(id);
    }

    private void validar(CourseRequest solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        exigir(solicitud.getTitle(), "title");
        exigir(solicitud.getBasePrice(), "basePrice");
        exigir(solicitud.getStartDate(), "startDate");
        exigir(solicitud.getEndDate(), "endDate");
        exigir(solicitud.getMaxCapacity(), "maxCapacity");
        exigir(solicitud.getSpecialtyId(), "specialtyId");
        exigir(solicitud.getInstructorId(), "instructorId");
    }

    private CourseResponse respuesta(Course curso) {
        return new CourseResponse(curso.getId(), curso.getTitle(), curso.getDescription(), curso.getBasePrice(),
                curso.getStartDate(), curso.getEndDate(), curso.getMaxCapacity(), curso.getStatus(),
                curso.getSpecialty() == null ? null : curso.getSpecialty().getId(),
                curso.getInstructor() == null ? null : curso.getInstructor().getId());
    }
}
