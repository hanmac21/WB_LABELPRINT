package com.example.wb_labelprint;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({
    "com.example.wb_labelprint.mapper.usa",
    "com.example.wb_labelprint.mapper.mex"
})
public class WbLabelprintApplication {

    public static void main(String[] args) {
        SpringApplication.run(WbLabelprintApplication.class, args);
    }

}
