package com.example.demo.course.services;

import com.example.demo.course.dto.CourseRequest;
import com.example.demo.course.model.Course;
import com.example.demo.course.model.Specialty;
import com.example.demo.course.repositories.CourseRepository;
import com.example.demo.user.model.Instructor;
import com.example.demo.user.services.InstructorService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;
    private final SpecialtyService specialtyService;
    private final InstructorService instructorService;

    public CourseService(CourseRepository courseRepository, SpecialtyService specialtyService,
            InstructorService instructorService) {
        this.courseRepository = courseRepository;
        this.specialtyService = specialtyService;
        this.instructorService = instructorService;
    }

    public Course create(CourseRequest request) {
        Specialty specialty = specialtyService.getById(request.getSpecialtyId());
        Instructor instructor = instructorService.getById(request.getInstructorId());
        Course course = new Course(request.getTitle(), request.getDescription(), request.getBasePrice(),
                request.getStartDate(), request.getEndDate(), request.getMaxCapacity(), specialty, instructor);
        return courseRepository.save(course);
    }

    public Course update(Long id, CourseRequest request) {
        Course course = getById(id);
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setBasePrice(request.getBasePrice());
        course.setStartDate(request.getStartDate());
        course.setEndDate(request.getEndDate());
        course.setMaxCapacity(request.getMaxCapacity());
        course.setSpecialty(specialtyService.getById(request.getSpecialtyId()));
        course.setInstructor(instructorService.getById(request.getInstructorId()));
        return courseRepository.save(course);
    }

    @Transactional(readOnly = true)
    public List<Course> getAll() {
        return courseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Course getById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Curso no encontrado con id " + id));
    }

    public Course save(Course course) {
        return courseRepository.save(course);
    }

    public void delete(Long id) {
        courseRepository.deleteById(id);
    }
}
