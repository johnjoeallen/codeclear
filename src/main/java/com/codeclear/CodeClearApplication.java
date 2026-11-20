package com.codeclear;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CodeClearApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeClearApplication.class, args);
    }
}
