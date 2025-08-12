package com.example.bankcards.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * For a secure endpoint test
 */
@RestController
public class TestSecureController {

    @GetMapping("/api/secure/hello")
    public String helloSecure() {
        return "\n" +
                "This is a secure endpoint that is only accessible with a JWT";
    }
}
