package com.example.demo.common;

import com.password4j.Password;
import java.util.Objects;

public class PasswordEncoder {

    public String encode(String rawPassword) {
        Objects.requireNonNull(rawPassword, "La contraseña no puede ser nula");
        return Password.hash(rawPassword).withBcrypt().getResult();
    }

    public boolean verify(String rawPassword, String encodedPassword) {
        Objects.requireNonNull(rawPassword, "La contraseña no puede ser nula");
        Objects.requireNonNull(encodedPassword, "La contraseña codificada no puede ser nula");
        return Password.check(rawPassword, encodedPassword).withBcrypt();
    }
}
