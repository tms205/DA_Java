package com.huit.da_java.controller;

import com.huit.da_java.model.WeatherInfo;
import com.huit.da_java.service.WeatherService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomerWeatherController {
    private final WeatherService weatherService;

    public CustomerWeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/customer/api/weather")
    public ResponseEntity<WeatherInfo> currentWeather(HttpSession session) {
        if (!"customer".equals(session.getAttribute("role"))) {
            return ResponseEntity.status(401).build();
        }
        try {
            return ResponseEntity.ok(weatherService.getCurrentWeather());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(502).build();
        }
    }
}
