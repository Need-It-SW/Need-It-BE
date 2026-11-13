package com.needitbe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class NeedItBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(NeedItBeApplication.class, args);
    }

}
