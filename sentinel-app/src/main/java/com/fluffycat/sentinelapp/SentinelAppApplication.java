package com.fluffycat.sentinelapp;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan(basePackages = "com.fluffycat.sentinelapp.**.repo",annotationClass = Mapper.class)
@ConfigurationPropertiesScan("com.fluffycat.sentinelapp")
@SpringBootApplication
public class SentinelAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(SentinelAppApplication.class, args);
    }

}
