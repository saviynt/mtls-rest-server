package org.example.mtls;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class MtlsApplication extends SpringBootServletInitializer {
    public static void main(String[] args) {
        SpringApplication.run(MtlsApplication.class, args);
    }
}