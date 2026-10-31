package com.example.demo.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordEncoderTest {

    private final PasswordEncoder passwordEncoder = new PasswordEncoder();

    @Test
    void encodeGeneraUnHashBcryptVerificable() {
        String rawPassword = "correct-horse-battery";

        String encodedPassword = passwordEncoder.encode(rawPassword);

        assertThat(encodedPassword).isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.verify(rawPassword, encodedPassword)).isTrue();
        assertThat(passwordEncoder.verify("wrong-password", encodedPassword)).isFalse();
    }
}
