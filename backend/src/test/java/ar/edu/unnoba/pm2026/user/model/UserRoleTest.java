package ar.edu.unnoba.pm2026.user.model;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.unnoba.pm2026.shared.security.Role;
import org.junit.jupiter.api.Test;

// El login unico (#27) saca el rol del tipo de usuario, sin guardarlo en una columna
class UserRoleTest {

    @Test
    void cadaTipoDeUsuarioTieneSuRol() {
        assertThat(new Student("Ana", "Perez", "ana@example.com", "x", "1").getRole()).isEqualTo(Role.STUDENT);
        assertThat(new Instructor("Iris", "Ruiz", "iris@example.com", "x").getRole()).isEqualTo(Role.INSTRUCTOR);
        assertThat(new Administrator("Ada", "Lovelace", "ada@example.com", "x").getRole())
                .isEqualTo(Role.ADMINISTRATOR);
    }

    @Test
    void elRolNoDependeDelConstructorQueUsaJpa() {
        assertThat(new Student().getRole()).isEqualTo(Role.STUDENT);
        assertThat(new Instructor().getRole()).isEqualTo(Role.INSTRUCTOR);
        assertThat(new Administrator().getRole()).isEqualTo(Role.ADMINISTRATOR);
    }
}
