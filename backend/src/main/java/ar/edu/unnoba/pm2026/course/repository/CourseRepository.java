package ar.edu.unnoba.pm2026.course.repository;

import ar.edu.unnoba.pm2026.course.model.Course;
import ar.edu.unnoba.pm2026.course.model.CourseStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByInstructorId(Long instructorId);

    // TP6: los listados de cursos por estado van de la fecha de inicio mas nueva a la mas vieja
    List<Course> findByStatusOrderByStartDateDesc(CourseStatus status);

    List<Course> findByInstructorIdAndStatusOrderByStartDateDesc(Long instructorId, CourseStatus status);
}
