package com.blog.blogsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BlogsystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(BlogsystemApplication.class, args);
	}

}
