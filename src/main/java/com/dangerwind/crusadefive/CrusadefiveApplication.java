package com.dangerwind.crusadefive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class CrusadefiveApplication {

	public static void main(String[] args) {
		SpringApplication.run(CrusadefiveApplication.class, args);
	}

}
