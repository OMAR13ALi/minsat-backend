package com.minsat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.minsat.config.AirConfig;

@SpringBootApplication
@EnableConfigurationProperties(AirConfig.class)
public class MinsatApplication {

    public static void main(String[] args) {
        SpringApplication.run(MinsatApplication.class, args);
    }
}
