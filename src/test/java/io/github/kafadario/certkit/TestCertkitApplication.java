package io.github.kafadario.certkit;

import org.springframework.boot.SpringApplication;

public class TestCertkitApplication {

	public static void main(String[] args) {
		SpringApplication.from(CertkitApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
