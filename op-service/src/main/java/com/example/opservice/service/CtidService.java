package com.example.opservice.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class CtidService {

    @Value("${ctid.endpoint}")
    private String ctidEndpoint;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean verifyUser(String idCard, String name, String businessLicense) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = "{\"idCard\":\"" + idCard + "\",\"name\":\"" + name + "\",\"businessLicense\":\"" + (businessLicense != null ? businessLicense : "") + "\"}";
            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            Boolean result = restTemplate.postForObject(ctidEndpoint, entity, Boolean.class);
            return result != null && result;
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("调用 ctid-service 失败: 状态码=" + e.getStatusCode() + ", 响应=" + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("调用 ctid-service 异常: " + e.getMessage(), e);
        }
    }
}