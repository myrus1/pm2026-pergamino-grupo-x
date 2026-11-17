package ar.edu.unnoba.pm2026.course.service;

import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.course.dto.CourseRequest;
import ar.edu.unnoba.pm2026.course.model.Course;
import ar.edu.unnoba.pm2026.course.exception.CourseStateException;
import ar.edu.unnoba.pm2026.course.model.CourseStatus;
import ar.edu.unnoba.pm2026.course.model.Specialty;
import ar.edu.unnoba.pm2026.course.repository.CourseRepository;
import ar.edu.unnoba.pm2026.shared.exception.ResourceNotFoundException;
import ar.edu.unnoba.pm2026.shared.exception.ValidationException;
import ar.edu.unnoba.pm2026.user.model.Instructor;
import ar.edu.unnoba.pm2026.user.service.InstructorService;
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
    public Course create(CourseRequest request, AuthenticatedUser actor) {
        validateRequest(request, actor);
        Specialty specialty = specialtyService.getById(request.getSpecialtyId());
        Instructor instructor = instructorService.getById(resolveInstructorId(request, actor));
        Course course = instructor.createCourse(request.getTitle(), request.getDescription(), request.getBasePrice(),
                request.getStartDate(), request.getEndDate(), request.getMaxCapacity(), specialty);
        return courseRepository.save(course);
    }

    public Course update(Long id, CourseRequest request, AuthenticatedUser actor) {
        validateRequest(request, actor);
        Course course = getById(id);
        requireModifiable(course, actor);
        Specialty specialty = specialtyService.getById(request.getSpecialtyId());
        Instructor instructor = instructorService.getById(resolveInstructorId(request, actor));
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
                .orElseThrow(() -> new ResourceNotFoundException("course.not-found",
                        "Curso no encontrado con id " + id));
    }

    // Igual que getById pero ademas exige que el curso le pertenezca al instructor del token.
    @Transactional(readOnly = true)
    public Course getById(Long id, AuthenticatedUser actor) {
        Course course = getById(id);
        actor.requireOwnership(course.getInstructor() == null ? null : course.getInstructor().getId(),
                "El curso pertenece a otro instructor");
        return course;
    }

    public Course save(Course course) {
        return courseRepository.save(course);
    }

    public void delete(Long id, AuthenticatedUser actor) {
        Course course = getById(id);
        requireModifiable(course, actor);
        courseRepository.deleteById(id);
    }

    private Long resolveInstructorId(CourseRequest request, AuthenticatedUser actor) {
        return actor.role() == Role.INSTRUCTOR ? actor.id() : request.getInstructorId();
    }

    private void validateRequest(CourseRequest request, AuthenticatedUser actor) {
        if (request == null) {
            throw new ValidationException("La solicitud no puede ser nula");
        }
        if (actor == null) {
            throw new ValidationException("El usuario autenticado no puede ser nulo");
        }
        if (actor.role() == Role.ADMINISTRATOR && request.getInstructorId() == null) {
            throw new ValidationException("El campo 'instructorId' es obligatorio");
        }
    }

    // Un curso ajeno es 403; uno que ya salio de borrador es 409 (hay que reenviarlo a aprobacion).
    private void requireModifiable(Course course, AuthenticatedUser actor) {
        actor.requireOwnership(course.getInstructor() == null ? null : course.getInstructor().getId(),
                "El curso pertenece a otro instructor");
        if (actor.role() != Role.ADMINISTRATOR && course.getStatus() != CourseStatus.DRAFT) {
            throw new CourseStateException("El curso solo se puede modificar mientras este en borrador");
        }
    }
}
