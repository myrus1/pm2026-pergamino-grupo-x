package ar.edu.unnoba.pm2026.user.service;

import ar.edu.unnoba.pm2026.user.exception.InstructorEmailAlreadyExistsException;
import ar.edu.unnoba.pm2026.shared.security.PasswordEncoder;
import ar.edu.unnoba.pm2026.course.dto.CourseResponse;
import ar.edu.unnoba.pm2026.course.dto.SpecialtyResponse;
import ar.edu.unnoba.pm2026.course.model.Course;
import ar.edu.unnoba.pm2026.course.model.CourseStatus;
import ar.edu.unnoba.pm2026.course.model.Specialty;
import ar.edu.unnoba.pm2026.course.service.SpecialtyService;
import ar.edu.unnoba.pm2026.user.dto.InstructorDetailResponse;
import ar.edu.unnoba.pm2026.user.dto.InstructorSummaryResponse;
import ar.edu.unnoba.pm2026.user.model.Instructor;
import ar.edu.unnoba.pm2026.user.repository.InstructorRepository;
import ar.edu.unnoba.pm2026.shared.exception.ResourceNotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InstructorService extends UserService<Instructor> {

    private final InstructorRepository instructorRepository;
    private final SpecialtyService specialtyService;

    public InstructorService(InstructorRepository instructorRepository, PasswordEncoder passwordEncoder,
            SpecialtyService specialtyService) {
        super(instructorRepository, passwordEncoder);
        this.instructorRepository = instructorRepository;
        this.specialtyService = specialtyService;
    }

    @Transactional(readOnly = true)
    public List<Instructor> getAll() {
        return instructorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Instructor getById(Long id) {
        return instructorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("instructor.not-found",
                        "Instructor no encontrado con id " + id));
    }

    @Transactional(readOnly = true)
    public List<SpecialtyResponse> getSpecialties(Long instructorId) {
        return specialtiesOf(getById(instructorId));
    }

    // TP6: GET /admin/instructors. Las especialidades son LAZY, por eso se arma aca y no en el controller.
    @Transactional(readOnly = true)
    public List<InstructorSummaryResponse> getSummaries() {
        return instructorRepository.findAll().stream()
                .map(instructor -> new InstructorSummaryResponse(instructor.getId(), instructor.getFirstName(),
                        instructor.getLastName(), instructor.getEmail(), specialtiesOf(instructor)))
                .toList();
    }

    // TP6: GET /admin/instructors/{id}: datos, especialidades y resumen de cursos (por fecha de inicio descendente)
    @Transactional(readOnly = true)
    public InstructorDetailResponse getDetail(Long id) {
        Instructor instructor = getById(id);
        List<Course> courses = instructor.getCourses().stream()
                .sorted(Comparator.comparing(Course::getStartDate).reversed())
                .toList();
        InstructorDetailResponse.CourseSummary summary = new InstructorDetailResponse.CourseSummary(courses.size(),
                countByStatus(courses, CourseStatus.DRAFT), countByStatus(courses, CourseStatus.PENDING_APPROVAL),
                countByStatus(courses, CourseStatus.PUBLISHED));
        return new InstructorDetailResponse(instructor.getId(), instructor.getFirstName(), instructor.getLastName(),
                instructor.getEmail(), specialtiesOf(instructor), summary,
                courses.stream().map(CourseResponse::from).toList());
    }

    private static List<SpecialtyResponse> specialtiesOf(Instructor instructor) {
        return instructor.getSpecialties().stream()
                .sorted(Comparator.comparing(Specialty::getName))
                .map(specialty -> new SpecialtyResponse(specialty.getId(), specialty.getName()))
                .toList();
    }

    private static int countByStatus(List<Course> courses, CourseStatus status) {
        return (int) courses.stream().filter(course -> course.getStatus() == status).count();
    }

    @Transactional(readOnly = true)
    public Instructor findByEmail(String email) {
        return instructorRepository.findByEmail(email);
    }

    public void delete(Long id) {
        instructorRepository.deleteById(id);
    }

    public void addSpecialty(Long instructorId, Long specialtyId) {
        Instructor instructor = getById(instructorId);
        Specialty specialty = specialtyService.getById(specialtyId);
        instructor.addSpecialty(specialty);
        instructorRepository.save(instructor);
    }

    public void removeSpecialty(Long instructorId, Long specialtyId) {
        Instructor instructor = getById(instructorId);
        Specialty specialty = specialtyService.getById(specialtyId);
        instructor.removeSpecialty(specialty);
        instructorRepository.save(instructor);
    }

    @Override
    protected void validateEmailAvailable(String email, Long instructorId) {
        Instructor existente = instructorRepository.findByEmail(email);
        if (existente != null && (instructorId == null || !Objects.equals(existente.getId(), instructorId))) {
            throw new InstructorEmailAlreadyExistsException(email);
        }
    }
}
