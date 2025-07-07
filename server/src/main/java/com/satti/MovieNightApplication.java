package com.satti;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@Slf4j
@EnableCaching
@EnableScheduling
@EnableKafka
@MapperScan(basePackages = "com.satti.mapper")
public class MovieNightApplication {

    public static void main(String[] args) {
        System.out.println("Hello World!");
        SpringApplication.run(MovieNightApplication.class, args);
    }

}
