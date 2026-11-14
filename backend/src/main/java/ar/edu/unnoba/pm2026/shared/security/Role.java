package ar.edu.unnoba.pm2026.shared.security;

// Roles de usuario que viajan dentro del JWT.
// Vive en common porque lo usan JwtTokenUtil (firma/lectura) y todos los controladores
// al pedir permisos; no pertenece a una sola feature.
public enum Role {

    STUDENT,
    INSTRUCTOR,
    ADMINISTRATOR
}
