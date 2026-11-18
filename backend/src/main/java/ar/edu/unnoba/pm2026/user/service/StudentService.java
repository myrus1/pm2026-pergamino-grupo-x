package ar.edu.unnoba.pm2026.user.service;

import ar.edu.unnoba.pm2026.shared.security.PasswordEncoder;
import ar.edu.unnoba.pm2026.shared.exception.ResourceNotFoundException;
import ar.edu.unnoba.pm2026.user.model.Student;
import ar.edu.unnoba.pm2026.user.repository.StudentRepository;
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
                .orElseThrow(() -> new ResourceNotFoundException("student.not-found",
                        "Estudiante no encontrado con id " + id));
    }

    @Transactional(readOnly = true)
    public Student findByEmail(String email) {
        return studentRepository.findByEmail(email);
    }

    public void delete(Long id) {
        studentRepository.deleteById(id);
    }
}
