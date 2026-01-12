package com.Team1_Back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class Team1BackApplication {

	public static void main(String[] args) {
		SpringApplication.run(Team1BackApplication.class, args);
	}

}
