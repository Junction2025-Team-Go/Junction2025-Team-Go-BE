package com.example.junction2025;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;


@SpringBootApplication
public class Junction2025Application {
    public static void main(String[] args) {
        SpringApplication.run(Junction2025Application.class, args);
    }
}
