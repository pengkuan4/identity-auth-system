package com.example.opservice.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class BlockchainService {

    @Value("${blockchain.endpoint}")
    private String blockchainEndpoint;

    private final RestTemplate restTemplate = new RestTemplate();

    public void logAuthorization(String authCode, String scope, String purpose) {
        // 调用区块链服务
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{\"authCode\":\"" + authCode + "\",\"scope\":\"" + scope + "\",\"purpose\":\"" + purpose + "\"}", headers);
        restTemplate.postForObject(blockchainEndpoint, entity, String.class);
    }

    public void logToken(String token) {
        // 调用区块链服务
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{\"token\":\"" + token + "\"}", headers);
        restTemplate.postForObject(blockchainEndpoint, entity, String.class);
    }
}