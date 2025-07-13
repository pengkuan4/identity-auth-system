package com.example.opservice.service;



import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private final Map<String, String> codeToClient = new HashMap<>();

    public String generateAuthCode(String clientId, String scope) {
        String authCode = UUID.randomUUID().toString();
        codeToClient.put(authCode, clientId);
        return authCode;
    }

    public String generateToken(String code) {
        if (codeToClient.containsKey(code)) {
            codeToClient.remove(code); // 一次性使用
            return UUID.randomUUID().toString(); // 模拟 token
        }
        throw new IllegalArgumentException("无效的授权码");
    }
}