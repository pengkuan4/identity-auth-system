package com.example.rpservice.service;



import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    @Value("${op.endpoint.ctid}")
    private String ctidEndpoint;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean verifyUser(String idCard, String name, String businessLicense, String accessToken) {
        try {
            // 确保 idCard 是 18 位
            if (idCard != null && !idCard.matches("\\d{17}[0-9X]")) {
                System.err.println("Invalid idCard format: " + idCard + ", expected 18 digits with optional X");
                return false;
            }
            System.out.println("Calling CTID endpoint: " + ctidEndpoint + " with idCard: " + idCard + ", name: " + name);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("idCard", idCard);
            requestBody.put("name", name);
            if (businessLicense != null && !businessLicense.trim().isEmpty()) {
                requestBody.put("businessLicense", businessLicense);
            }
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(ctidEndpoint, entity, String.class);
            System.out.println("CTID response: " + response.getBody());

            if (response.getStatusCode().is2xxSuccessful()) {
                String responseBody = response.getBody();
                if (responseBody != null) {
                    responseBody = responseBody.trim().toLowerCase();
                    if ("true".equals(responseBody)) {
                        return true;
                    } else if ("false".equals(responseBody)) {
                        return false;
                    } else {
                        System.err.println("CTID response invalid: " + responseBody);
                        return false;
                    }
                }
                System.err.println("CTID response body is null");
                return false;
            } else {
                System.err.println("CTID response status: " + response.getStatusCode() + ", body: " + response.getBody());
                return false;
            }
        } catch (Exception e) {
            System.err.println("CTID verification failed: " + e.getMessage());
            return false;
        }
    }

    public String getUserInfo(String accessToken) {
        return "Mock user info"; // 占位符
    }

    public void logOperation(String operation, String accessToken) {
        // 实现日志记录逻辑
    }
}