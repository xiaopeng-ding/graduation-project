package com.peng.ding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan("com.peng.ding.dao")
public class DingApplication {
    public static void main(String[] args) {
        SpringApplication.run(DingApplication.class, args);
    }
}
