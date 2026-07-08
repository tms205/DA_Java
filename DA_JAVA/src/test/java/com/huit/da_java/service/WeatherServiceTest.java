package com.huit.da_java.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeatherServiceTest {

    @Test
    void describeWeatherCodeMapsCommonOpenMeteoCodes() {
        WeatherService service = new WeatherService(
                RestClient.builder(),
                "https://example.test",
                10.7769,
                106.7009,
                "TP. Ho Chi Minh");

        assertEquals("Trời quang", service.describeWeatherCode(0));
        assertEquals("Có mây", service.describeWeatherCode(2));
        assertEquals("Có mưa", service.describeWeatherCode(63));
        assertEquals("Dông", service.describeWeatherCode(95));
        assertEquals("Đang cập nhật", service.describeWeatherCode(999));
    }
}
