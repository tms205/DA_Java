package com.huit.da_java.service;

import com.huit.da_java.model.WeatherInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class WeatherService {
    private final RestClient restClient;
    private final String apiUrl;
    private final double latitude;
    private final double longitude;
    private final String locationName;

    public WeatherService(RestClient.Builder restClientBuilder,
                          @Value("${app.weather.api-url:https://api.open-meteo.com/v1/forecast}") String apiUrl,
                          @Value("${app.weather.latitude:10.7769}") double latitude,
                          @Value("${app.weather.longitude:106.7009}") double longitude,
                          @Value("${app.weather.location-name:TP. Ho Chi Minh}") String locationName) {
        this.restClient = restClientBuilder.build();
        this.apiUrl = apiUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationName = locationName;
    }

    public WeatherInfo getCurrentWeather() {
        Map<String, Object> response = restClient.get()
                .uri(apiUrl + "?latitude={latitude}&longitude={longitude}"
                                + "&current=temperature_2m,relative_humidity_2m,apparent_temperature,"
                                + "precipitation,weather_code,wind_speed_10m"
                                + "&timezone=auto",
                        latitude,
                        longitude)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (response == null || !(response.get("current") instanceof Map<?, ?> current)) {
            throw new IllegalStateException("Khong nhan duoc du lieu thoi tiet hien tai.");
        }

        int weatherCode = number(current.get("weather_code")).intValue();
        return new WeatherInfo(
                locationName,
                number(current.get("temperature_2m")).doubleValue(),
                number(current.get("apparent_temperature")).doubleValue(),
                number(current.get("relative_humidity_2m")).intValue(),
                number(current.get("wind_speed_10m")).doubleValue(),
                number(current.get("precipitation")).doubleValue(),
                weatherCode,
                describeWeatherCode(weatherCode),
                stringValue(current.get("time")));
    }

    String describeWeatherCode(int code) {
        return switch (code) {
            case 0 -> "Trời quang";
            case 1, 2, 3 -> "Có mây";
            case 45, 48 -> "Sương mù";
            case 51, 53, 55, 56, 57 -> "Mưa phùn";
            case 61, 63, 65, 66, 67 -> "Có mưa";
            case 71, 73, 75, 77 -> "Có tuyết";
            case 80, 81, 82 -> "Mưa rào";
            case 85, 86 -> "Mưa tuyết";
            case 95, 96, 99 -> "Dông";
            default -> "Đang cập nhật";
        };
    }

    private Number number(Object value) {
        if (value instanceof Number number) {
            return number;
        }
        throw new IllegalStateException("Du lieu thoi tiet khong hop le.");
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
