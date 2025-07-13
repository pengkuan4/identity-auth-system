package com.example.opservice.controller;


import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserInfoController {

    @GetMapping("/userinfo")
    public String userInfo(@AuthenticationPrincipal Jwt jwt) {
        try {
            if (jwt == null) {
                return "{\"error\": \"未提供有效 JWT，当前为公开访问\"}";
            }
            return "{\"sub\": \"" + jwt.getSubject() + "\", \"name\": \"测试用户\", \"preferred_username\": \"" + jwt.getClaimAsString("preferred_username") + "\"}";
        } catch (Exception e) {
            return "{\"error\": \"处理用户信息失败: " + e.getMessage() + "\"}";
        }
    }
}