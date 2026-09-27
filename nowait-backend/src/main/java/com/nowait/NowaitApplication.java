package com.nowait;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class NowaitApplication {

    public static void main(String[] args) {
        SpringApplication.run(NowaitApplication.class, args);
    }
}
