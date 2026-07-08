package com.huit.da_java.model;

public record WeatherInfo(
        String location,
        double temperatureC,
        double apparentTemperatureC,
        int humidityPercent,
        double windSpeedKmh,
        double precipitationMm,
        int weatherCode,
        String description,
        String updatedAt) {
}
