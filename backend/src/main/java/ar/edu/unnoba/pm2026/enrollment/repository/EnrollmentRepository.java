package ar.edu.unnoba.pm2026.enrollment.repository;

import ar.edu.unnoba.pm2026.course.model.Course;
import ar.edu.unnoba.pm2026.enrollment.model.Enrollment;
import ar.edu.unnoba.pm2026.user.model.Student;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByCourseInstructorId(Long instructorId);

    List<Enrollment> findByStudentId(Long studentId);

    List<Enrollment> findByCourseIdOrderByEnrollmentDateAsc(Long courseId);

    // Reportes del TP6: inscripciones con fecha en [start, end), de la mas vieja a la mas nueva
    @Query("SELECT enrollment FROM Enrollment enrollment WHERE enrollment.enrollmentDate >= :start"
            + " AND enrollment.enrollmentDate < :end ORDER BY enrollment.enrollmentDate ASC, enrollment.id ASC")
    List<Enrollment> findByEnrollmentDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT enrollment.student FROM Enrollment enrollment WHERE enrollment.course.id = :courseId")
    List<Student> findStudentsByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT enrollment.course FROM Enrollment enrollment WHERE enrollment.student.id = :studentId"
            + " ORDER BY enrollment.course.startDate DESC, enrollment.id DESC")
    List<Course> findCoursesByStudentId(@Param("studentId") Long studentId);
}
