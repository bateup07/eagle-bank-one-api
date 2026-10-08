package com.eaglebank;

import com.eaglebank.model.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Provides support for eagle bank application.
 *
 * @author mattbateup
 */
@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class EagleBankApplication {

    public static void main(String[] args) {
        SpringApplication.run(EagleBankApplication.class, args);
    }
}
