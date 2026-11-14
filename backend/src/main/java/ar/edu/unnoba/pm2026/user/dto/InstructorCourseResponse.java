package ar.edu.unnoba.pm2026.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class InstructorCourseResponse {

    private Long id;
    private String name;
    private String description;
}
