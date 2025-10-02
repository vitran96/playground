package tech.kingoyster.spring_1;

import org.springframework.boot.SpringApplication;

public class TestDemoApplication {

	public static void main(String[] args) {
		SpringApplication.from(Spring1Application::main).with(TestcontainersConfiguration.class).run(args);
	}

}
