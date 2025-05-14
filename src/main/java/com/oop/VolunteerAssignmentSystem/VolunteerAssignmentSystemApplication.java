package com.oop.VolunteerAssignmentSystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // Important for running GA in a separate thread
public class VolunteerAssignmentSystemApplication {
	public static void main(String[] args) {
		SpringApplication.run(VolunteerAssignmentSystemApplication.class, args);
	}
}
