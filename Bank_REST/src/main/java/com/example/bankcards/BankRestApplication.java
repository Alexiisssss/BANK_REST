package com.example.bankcards;

import com.example.bankcards.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.example.bankcards.config.CryptoProperties;

@EnableConfigurationProperties({CryptoProperties.class, JwtProperties.class})
@SpringBootApplication
public class BankRestApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankRestApplication.class, args);
    }
}
