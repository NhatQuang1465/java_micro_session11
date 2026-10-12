package com.example.demoredisspringboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class DemoRedisSpringBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoRedisSpringBootApplication.class, args);
    }

}
