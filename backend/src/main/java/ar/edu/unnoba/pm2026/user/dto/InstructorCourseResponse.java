package ar.edu.unnoba.pm2026.user.dto;

import ar.edu.unnoba.pm2026.course.model.Course;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class InstructorCourseResponse {

    private Long id;
    private String name;
    private String description;

    // TP5/TP6: GET /instructor/courses devuelve id, name (el titulo) y description
    public static InstructorCourseResponse from(Course course) {
        return new InstructorCourseResponse(course.getId(), course.getTitle(), course.getDescription());
    }
}
