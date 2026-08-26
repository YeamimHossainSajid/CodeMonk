package com.codemonk.analysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class CodeAnalysisApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeAnalysisApplication.class, args);
    }
}
