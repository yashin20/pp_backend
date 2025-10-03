package project.pp_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing // JPA Auditing 기능 활성화
@SpringBootApplication
public class PpBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(PpBackendApplication.class, args);
	}
}
