package com.yieldstreet;

import org.springframework.boot.SpringApplication;

public class TestAssignmentApplication {

	public static void main(String[] args) {
		SpringApplication.from(AssignmentApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
