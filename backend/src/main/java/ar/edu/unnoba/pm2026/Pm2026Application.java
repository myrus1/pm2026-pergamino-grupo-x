package ar.edu.unnoba.pm2026;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import ar.edu.unnoba.pm2026.shared.security.JwtTokenUtil;
import ar.edu.unnoba.pm2026.shared.security.PasswordEncoder;
import java.time.Duration;

@SpringBootApplication
public class Pm2026Application {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new PasswordEncoder();
	}

	@Bean
	public JwtTokenUtil jwtTokenUtil(@Value("${jwt.secret}") String secret,
			@Value("${jwt.expiration}") long expirationSeconds) {
		return new JwtTokenUtil(secret, Duration.ofSeconds(expirationSeconds));
	}

	public static void main(String[] args) {
		SpringApplication.run(Pm2026Application.class, args);
	}

}
