package com.nefentus.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@AutoConfiguration
public class SecureWebApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecureWebApiApplication.class, args);
    }

}
