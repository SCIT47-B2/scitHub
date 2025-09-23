package net.dsa.scitHub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling // 스케줄링 기능을 위한 어노테이션
@EnableJpaAuditing // @createdAt, @updatedAt 사용을 위한JPA Auditing 활성화
@SpringBootApplication
public class ScitHubApplication {

	public static void main(String[] args) {
		SpringApplication.run(ScitHubApplication.class, args);
	}

}
