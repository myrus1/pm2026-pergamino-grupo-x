package com.example.demo.enrollment.repositories;

import com.example.demo.course.model.Course;
import com.example.demo.enrollment.model.Enrollment;
import com.example.demo.user.model.Student;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByCourseInstructorId(Long instructorId);

    @Query("SELECT enrollment.student FROM Enrollment enrollment WHERE enrollment.course.id = :courseId")
    List<Student> findStudentsByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT enrollment.course FROM Enrollment enrollment WHERE enrollment.student.id = :studentId"
            + " ORDER BY enrollment.course.startDate DESC, enrollment.id DESC")
    List<Course> findCoursesByStudentId(@Param("studentId") Long studentId);
}
