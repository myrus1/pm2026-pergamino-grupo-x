package ar.edu.unnoba.pm2026.shared.security;

import java.util.Arrays;

// Roles de usuario que viajan dentro del JWT (claim "rol").
// En el codigo van en ingles, pero en el token se mantienen los valores de siempre
// (ALUMNO, INSTRUCTOR, ADMINISTRADOR) porque el front los lee asi.
public enum Role {

    STUDENT("ALUMNO"),
    INSTRUCTOR("INSTRUCTOR"),
    ADMINISTRATOR("ADMINISTRADOR");

    private final String claimValue;

    Role(String claimValue) {
        this.claimValue = claimValue;
    }

    public String claimValue() {
        return claimValue;
    }

    // Lanza IllegalArgumentException si el valor no corresponde a ningun rol
    public static Role fromClaimValue(String claimValue) {
        return Arrays.stream(values())
                .filter(role -> role.claimValue.equals(claimValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown role in token: " + claimValue));
    }
}
