package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import com.example.demo.common.JwtTokenUtil;
import com.example.demo.common.PasswordEncoder;

@SpringBootApplication
public class DemoApplication {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new PasswordEncoder();
	}

	@Bean
	public JwtTokenUtil jwtTokenUtil(@Value("${jwt.secret}") String secret) {
		return new JwtTokenUtil(secret);
	}

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}
