package ar.edu.unnoba.pm2026.shared.security;

import com.password4j.Password;
import java.util.Objects;

public class PasswordEncoder {

    public String encode(String rawPassword) {
        Objects.requireNonNull(rawPassword, "Password cannot be null");
        return Password.hash(rawPassword).withBcrypt().getResult();
    }

    public boolean verify(String rawPassword, String encodedPassword) {
        Objects.requireNonNull(rawPassword, "Password cannot be null");
        Objects.requireNonNull(encodedPassword, "Encoded password cannot be null");
        return Password.check(rawPassword, encodedPassword).withBcrypt();
    }
}
