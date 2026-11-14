package ar.edu.unnoba.pm2026.enrollment.repository;

import ar.edu.unnoba.pm2026.course.model.Course;
import ar.edu.unnoba.pm2026.enrollment.model.Enrollment;
import ar.edu.unnoba.pm2026.user.model.Student;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByCourseInstructorId(Long instructorId);

    List<Enrollment> findByStudentId(Long studentId);

    @Query("SELECT enrollment.student FROM Enrollment enrollment WHERE enrollment.course.id = :courseId")
    List<Student> findStudentsByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT enrollment.course FROM Enrollment enrollment WHERE enrollment.student.id = :studentId"
            + " ORDER BY enrollment.course.startDate DESC, enrollment.id DESC")
    List<Course> findCoursesByStudentId(@Param("studentId") Long studentId);
}
