package ar.edu.unnoba.pm2026.course.service;

import ar.edu.unnoba.pm2026.auth.exception.AuthorizationException;
import ar.edu.unnoba.pm2026.shared.security.Role;
import ar.edu.unnoba.pm2026.shared.security.AuthenticatedUser;
import ar.edu.unnoba.pm2026.course.dto.CourseDetailResponse;
import ar.edu.unnoba.pm2026.course.dto.CourseRequest;
import ar.edu.unnoba.pm2026.course.dto.SpecialtyResponse;
import ar.edu.unnoba.pm2026.course.dto.UpdateCourseRequest;
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

    // Crear y editar cursos es solo del instructor, y siempre sobre los suyos: el TP6 no deja que el
    // administrador haga acciones de instructor.
    public Course create(CourseRequest request, AuthenticatedUser actor) {
        requireInstructor(actor);
        validateRequest(request);
        Specialty specialty = specialtyService.getById(request.getSpecialtyId());
        Instructor instructor = instructorService.getById(actor.id());
        Course course = instructor.createCourse(request.getTitle(), request.getDescription(), request.getBasePrice(),
                request.getStartDate(), request.getEndDate(), request.getMaxCapacity(), specialty);
        return courseRepository.save(course);
    }

    public Course update(Long id, CourseRequest request, AuthenticatedUser actor) {
        requireInstructor(actor);
        validateRequest(request);
        Course course = getById(id);
        requireModifiable(course, actor);
        Specialty specialty = specialtyService.getById(request.getSpecialtyId());
        course.getInstructor().validateSpecialty(specialty);
        course.applyChanges(request.getTitle(), request.getDescription(), request.getBasePrice(),
                request.getStartDate(), request.getEndDate(), request.getMaxCapacity());
        // En la edicion completa una descripcion vacia en el body borra la anterior
        course.setDescription(request.getDescription());
        course.setSpecialty(specialty);
        return courseRepository.save(course);
    }

    // TP6: PUT /instructor/courses/{id} con solo los campos a modificar. Mismas reglas que la edicion
    // completa: 403 si es de otro instructor, 409 si ya no esta en borrador.
    public Course updatePartially(Long id, UpdateCourseRequest changes, AuthenticatedUser actor) {
        requireInstructor(actor);
        if (changes == null) {
            throw new ValidationException("La solicitud no puede ser nula");
        }
        Course course = getById(id);
        requireModifiable(course, actor);
        if (changes.getSpecialtyId() != null) {
            Specialty specialty = specialtyService.getById(changes.getSpecialtyId());
            course.getInstructor().validateSpecialty(specialty);
            course.setSpecialty(specialty);
        }
        course.applyChanges(changes.getTitle(), changes.getDescription(), changes.getBasePrice(),
                changes.getStartDate(), changes.getEndDate(), changes.getMaxCapacity());
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

    // TP6: cursos aprobados (publicados), por fecha de inicio descendente
    @Transactional(readOnly = true)
    public List<Course> getPublished() {
        return courseRepository.findByStatusOrderByStartDateDesc(CourseStatus.PUBLISHED);
    }

    // 404 si el instructor no existe, aunque no tenga cursos en ese estado
    @Transactional(readOnly = true)
    public List<Course> getByInstructorAndStatus(Long instructorId, CourseStatus status) {
        instructorService.getById(instructorId);
        return courseRepository.findByInstructorIdAndStatusOrderByStartDateDesc(instructorId, status);
    }

    // TP6: detalle con instructor, inscriptos y cupo. Recorre relaciones LAZY, por eso se arma aca.
    @Transactional(readOnly = true)
    public CourseDetailResponse getDetail(Long id) {
        return detailOf(getById(id));
    }

    // Igual, pero el instructor solo ve el detalle de sus cursos (403 si es de otro)
    @Transactional(readOnly = true)
    public CourseDetailResponse getDetail(Long id, AuthenticatedUser actor) {
        return detailOf(getById(id, actor));
    }

    private static CourseDetailResponse detailOf(Course course) {
        Specialty specialty = course.getSpecialty();
        Instructor instructor = course.getInstructor();
        return new CourseDetailResponse(course.getId(), course.getTitle(), course.getDescription(),
                course.getStatus(), course.getStartDate(), course.getEndDate(), course.getBasePrice(),
                course.getMaxCapacity(), course.enrolledStudents(), course.availableSeats(),
                new SpecialtyResponse(specialty.getId(), specialty.getName()),
                new CourseDetailResponse.CourseInstructor(instructor.getId(), instructor.getFirstName(),
                        instructor.getLastName(), instructor.getEmail()));
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

    private void requireInstructor(AuthenticatedUser actor) {
        if (actor == null) {
            throw new ValidationException("El usuario autenticado no puede ser nulo");
        }
        if (actor.role() != Role.INSTRUCTOR) {
            throw new AuthorizationException("Solo un instructor puede crear o modificar cursos");
        }
    }

    private void validateRequest(CourseRequest request) {
        if (request == null) {
            throw new ValidationException("La solicitud no puede ser nula");
        }
    }

    // Un curso ajeno es 403; uno que ya salio de borrador es 409 (hay que reenviarlo a aprobacion).
    // El administrador solo llega aca para borrar: puede sacar cualquier curso.
    private void requireModifiable(Course course, AuthenticatedUser actor) {
        actor.requireOwnership(course.getInstructor() == null ? null : course.getInstructor().getId(),
                "El curso pertenece a otro instructor");
        if (actor.role() != Role.ADMINISTRATOR && course.getStatus() != CourseStatus.DRAFT) {
            throw new CourseStateException("El curso solo se puede modificar mientras este en borrador");
        }
    }
}
