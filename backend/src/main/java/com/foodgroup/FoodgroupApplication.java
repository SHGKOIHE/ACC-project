package com.foodgroup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FoodgroupApplication {
    public static void main(String[] args) {
        SpringApplication.run(FoodgroupApplication.class, args);
    }
}
