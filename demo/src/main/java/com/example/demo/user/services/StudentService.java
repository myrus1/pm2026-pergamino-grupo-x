package com.example.demo.user.services;

import com.example.demo.common.PasswordEncoder;
import com.example.demo.user.model.Student;
import com.example.demo.user.repositories.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
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
                .orElseThrow(() -> new EntityNotFoundException("Estudiante no encontrado con id " + id));
    }

    public void delete(Long id) {
        studentRepository.deleteById(id);
    }
}
