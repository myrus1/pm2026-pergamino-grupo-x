package com.example.demo.course.services;

import com.example.demo.common.Rol;
import com.example.demo.common.UsuarioAutenticado;
import com.example.demo.course.dto.CourseRequest;
import com.example.demo.course.model.Course;
import com.example.demo.course.model.CourseStateException;
import com.example.demo.course.model.CourseStatus;
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

    // El instructor siempre crea cursos suyos; el administrador crea el que indica el body.
    public Course create(CourseRequest request, UsuarioAutenticado actor) {
        Specialty specialty = specialtyService.getById(request.getSpecialtyId());
        Instructor instructor = instructorService.getById(instructorDelActor(request, actor));
        Course course = instructor.createCourse(request.getTitle(), request.getDescription(), request.getBasePrice(),
                request.getStartDate(), request.getEndDate(), request.getMaxCapacity(), specialty);
        return courseRepository.save(course);
    }

    public Course update(Long id, CourseRequest request, UsuarioAutenticado actor) {
        Course course = getById(id);
        exigirQuePuedaModificar(course, actor);
        Specialty specialty = specialtyService.getById(request.getSpecialtyId());
        Instructor instructor = instructorService.getById(instructorDelActor(request, actor));
        instructor.validateSpecialty(specialty);
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setBasePrice(request.getBasePrice());
        course.setStartDate(request.getStartDate());
        course.setEndDate(request.getEndDate());
        course.setMaxCapacity(request.getMaxCapacity());
        course.setSpecialty(specialty);
        course.setInstructor(instructor);
        return courseRepository.save(course);
    }

    @Transactional(readOnly = true)
    public List<Course> getAll() {
        return courseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Course> getCoursesByInstructorId(Long instructorId) {
        return courseRepository.findByInstructorId(instructorId);
    }

    @Transactional(readOnly = true)
    public Course getById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Curso no encontrado con id " + id));
    }

    // Igual que getById pero ademas exige que el curso le pertenezca al instructor del token.
    @Transactional(readOnly = true)
    public Course getById(Long id, UsuarioAutenticado actor) {
        Course course = getById(id);
        actor.exigirPropio(course.getInstructor() == null ? null : course.getInstructor().getId(),
                "El curso pertenece a otro instructor");
        return course;
    }

    public Course save(Course course) {
        return courseRepository.save(course);
    }

    public void delete(Long id, UsuarioAutenticado actor) {
        Course course = getById(id);
        exigirQuePuedaModificar(course, actor);
        courseRepository.deleteById(id);
    }

    private Long instructorDelActor(CourseRequest request, UsuarioAutenticado actor) {
        return actor.rol() == Rol.INSTRUCTOR ? actor.id() : request.getInstructorId();
    }

    // Un curso ajeno es 403; uno que ya salio de borrador es 409 (hay que reenviarlo a aprobacion).
    private void exigirQuePuedaModificar(Course course, UsuarioAutenticado actor) {
        actor.exigirPropio(course.getInstructor() == null ? null : course.getInstructor().getId(),
                "El curso pertenece a otro instructor");
        if (actor.rol() != Rol.ADMINISTRADOR && course.getStatus() != CourseStatus.DRAFT) {
            throw new CourseStateException("El curso solo se puede modificar mientras este en borrador");
        }
    }
}
