package org.benefitmap.backend.BenefitMap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "org.benefitmap.backend")
public class BenefitMapBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BenefitMapBackendApplication.class, args);
	}
}