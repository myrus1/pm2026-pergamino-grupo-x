package ar.edu.unnoba.pm2026.user.dto;

import ar.edu.unnoba.pm2026.user.model.Instructor;

// TP6: respuesta del registro de instructor (POST /instructor), sin password
public record InstructorAccountResponse(Long id, String name, String lastName, String email) {

    public static InstructorAccountResponse from(Instructor instructor) {
        return new InstructorAccountResponse(instructor.getId(), instructor.getFirstName(), instructor.getLastName(),
                instructor.getEmail());
    }
}
