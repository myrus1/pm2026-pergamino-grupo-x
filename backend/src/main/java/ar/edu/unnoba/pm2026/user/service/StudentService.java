package ar.edu.unnoba.pm2026.user.service;

import ar.edu.unnoba.pm2026.enrollment.model.Enrollment;
import ar.edu.unnoba.pm2026.shared.security.PasswordEncoder;
import ar.edu.unnoba.pm2026.shared.exception.ErrorCode;
import ar.edu.unnoba.pm2026.shared.exception.ResourceNotFoundException;
import ar.edu.unnoba.pm2026.user.dto.StudentDetailResponse;
import ar.edu.unnoba.pm2026.user.model.Student;
import ar.edu.unnoba.pm2026.user.repository.StudentRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StudentService extends UserService<Student> {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository, PasswordEncoder passwordEncoder) {
        super(studentRepository, passwordEncoder);
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
    public List<Student> getAll() {
        return studentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Student getById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.STUDENT_NOT_FOUND, id));
    }

    // TP6: GET /admin/students/{id}. Las inscripciones son LAZY, por eso se arma aca y no en el controller.
    @Transactional(readOnly = true)
    public StudentDetailResponse getDetail(Long id) {
        Student student = getById(id);
        BigDecimal totalPaid = student.getEnrollments().stream()
                .map(Enrollment::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new StudentDetailResponse(student.getId(), student.getFirstName(), student.getLastName(),
                student.getEmail(), student.getDni(), student.getEnrollments().size(), totalPaid);
    }

    @Transactional(readOnly = true)
    public Student findByEmail(String email) {
        return studentRepository.findByEmail(email);
    }

    public void delete(Long id) {
        studentRepository.deleteById(id);
    }
}
