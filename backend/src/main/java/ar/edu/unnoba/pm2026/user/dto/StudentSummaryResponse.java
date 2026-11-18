package ar.edu.unnoba.pm2026.user.dto;

import ar.edu.unnoba.pm2026.user.model.Student;

// TP6: un alumno en GET /admin/students (name y lastName como en CreateStudentRequestDTO), sin password
public record StudentSummaryResponse(Long id, String name, String lastName, String email, String dni) {

    public static StudentSummaryResponse from(Student student) {
        return new StudentSummaryResponse(student.getId(), student.getFirstName(), student.getLastName(),
                student.getEmail(), student.getDni());
    }
}
