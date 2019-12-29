package com.ivolasek.gpsync;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
@Slf4j
public class GpsyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(GpsyncApplication.class, args);
        log.info("Go to: http://localhost:8080/");
    }

}
