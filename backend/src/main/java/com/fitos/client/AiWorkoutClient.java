package com.fitos.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/** Client gọi service Python `ai-workout` (port 8004) cho phần gợi ý giáo án. */
@Slf4j
@Component
public class AiWorkoutClient {

    private final String serviceUrl;
    private final String serviceToken;
    private final RestTemplate http;

    public AiWorkoutClient(@Value("${fitos.ai.workout-url:http://localhost:8004}") String serviceUrl,
                           @Value("${fitos.ai.token:fitos-ai-token}") String serviceToken) {
        this.serviceUrl = serviceUrl;
        this.serviceToken = serviceToken;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(25000);
        this.http = new RestTemplate(factory);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> post(String path, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(serviceToken);
        try {
            Map<String, Object> response = http.postForObject(serviceUrl + path,
                    new HttpEntity<>(body, headers), Map.class);
            return response == null ? Map.of() : response;
        } catch (Exception exception) {
            log.warn("Gọi ai-workout {} thất bại: {}", path, exception.getMessage());
            return null;
        }
    }
}
