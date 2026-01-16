package com.factory.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HellorController {
    @GetMapping("/hello")
    public String hello() {
        return "Backend is running!";
    }
}
