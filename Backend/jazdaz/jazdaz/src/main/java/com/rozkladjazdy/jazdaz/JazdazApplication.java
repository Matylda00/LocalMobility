package com.rozkladjazdy.jazdaz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class JazdazApplication {

	public static void main(String[] args) {
		SpringApplication.run(JazdazApplication.class, args);
	}

}
