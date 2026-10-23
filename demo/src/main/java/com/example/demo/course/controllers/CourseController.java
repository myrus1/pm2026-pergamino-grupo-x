package com.example.demo.course.controllers;

import static com.example.demo.controllers.Validaciones.exigir;

import com.example.demo.course.model.Course;
import com.example.demo.course.model.Specialty;
import com.example.demo.course.services.CourseService;
import com.example.demo.course.services.SpecialtyService;
import com.example.demo.course.dto.CourseRequest;
import com.example.demo.course.dto.CourseResponse;
import com.example.demo.user.model.Instructor;
import com.example.demo.user.services.InstructorService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// API REST de cursos: /api/courses
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;
    private final SpecialtyService specialtyService;
    private final InstructorService instructorService;

    public CourseController(CourseService courseService, SpecialtyService specialtyService,
            InstructorService instructorService) {
        this.courseService = courseService;
        this.specialtyService = specialtyService;
        this.instructorService = instructorService;
    }

    @GetMapping
    public List<CourseResponse> getAll() {
        return courseService.getAll().stream().map(this::respuesta).toList();
    }

    @GetMapping("/{id}")
    public CourseResponse getById(@PathVariable Long id) {
        return respuesta(courseService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(@RequestBody CourseRequest solicitud) {
        validar(solicitud);
        Specialty especialidad = specialtyService.getById(solicitud.getSpecialtyId());
        Instructor instructor = instructorService.getById(solicitud.getInstructorId());
        Course curso = new Course(solicitud.getTitle(), solicitud.getDescription(), solicitud.getBasePrice(),
                solicitud.getStartDate(), solicitud.getEndDate(), solicitud.getMaxCapacity(), especialidad, instructor);
        if (solicitud.getStatus() != null) {
            curso.setStatus(solicitud.getStatus());
        }
        return respuesta(courseService.save(curso));
    }

    @PutMapping("/{id}")
    public CourseResponse update(@PathVariable Long id, @RequestBody CourseRequest solicitud) {
        validar(solicitud);
        Course curso = courseService.getById(id);
        curso.setTitle(solicitud.getTitle());
        curso.setDescription(solicitud.getDescription());
        curso.setBasePrice(solicitud.getBasePrice());
        curso.setStartDate(solicitud.getStartDate());
        curso.setEndDate(solicitud.getEndDate());
        curso.setMaxCapacity(solicitud.getMaxCapacity());
        curso.setSpecialty(specialtyService.getById(solicitud.getSpecialtyId()));
        curso.setInstructor(instructorService.getById(solicitud.getInstructorId()));
        if (solicitud.getStatus() != null) {
            curso.setStatus(solicitud.getStatus());
        }
        return respuesta(courseService.save(curso));
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
