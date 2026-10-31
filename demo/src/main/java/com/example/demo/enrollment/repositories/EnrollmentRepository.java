package com.example.demo.enrollment.repositories;

import com.example.demo.enrollment.model.Enrollment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByCourseInstructorId(Long instructorId);
}
