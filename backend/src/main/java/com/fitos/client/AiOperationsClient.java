package com.fitos.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Client gọi service Python `ai-operations` (port 8003). Mọi lỗi mạng đều trả null để
 * tầng service tự quyết định fallback rule-based thay vì làm sập request của người dùng.
 */
@Slf4j
@Component
public class AiOperationsClient {

    private final String serviceUrl;
    private final String serviceToken;
    private final ObjectMapper objectMapper;
    private final RestTemplate http;

    public AiOperationsClient(@Value("${fitos.ai.operations-url:http://localhost:8003}") String serviceUrl,
                              @Value("${fitos.ai.token:fitos-ai-token}") String serviceToken,
                              ObjectMapper objectMapper) {
        this.serviceUrl = serviceUrl;
        this.serviceToken = serviceToken;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(15000);
        this.http = new RestTemplate(factory);
    }

    public boolean isReachable() {
        try {
            http.getForEntity(serviceUrl + "/health", String.class);
            return true;
        } catch (Exception exception) {
            log.debug("ai-operations không phản hồi: {}", exception.getMessage());
            return false;
        }
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
            log.warn("Gọi ai-operations {} thất bại: {}", path, exception.getMessage());
            return null;
        }
    }

    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }
}
