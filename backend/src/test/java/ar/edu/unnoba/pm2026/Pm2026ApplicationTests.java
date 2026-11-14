package ar.edu.unnoba.pm2026;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// "test" aplica src/test/resources/application-test.properties (pool de conexiones chico:
// la BD compartida del curso solo acepta 20 conexiones).
@SpringBootTest
@ActiveProfiles("test")
class Pm2026ApplicationTests {

	@Test
	void contextLoads() {
	}

}
