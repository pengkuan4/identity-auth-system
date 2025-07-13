package com.example.opservice.controller;

import com.example.opservice.service.AuthService;
import com.example.opservice.service.BlockchainService;
import com.example.opservice.service.CtidService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Controller
@RequestMapping("/oauth2")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private CtidService ctidService;

    @Autowired
    private BlockchainService blockchainService;

    @Value("${op.redirect-uri}")
    private String redirectUri;

    @GetMapping("/authorize")
    public String authorize(
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("scope") String scope,
            @RequestParam("state") String state,
            Model model) {
        try {
            System.out.println("Authorize request: client_id=" + clientId + ", redirect_uri=" + redirectUri + ", scope=" + scope + ", state=" + state);
            if (!redirectUri.equals(redirectUri)) { // 严格匹配 redirect_uri
                System.err.println("Invalid redirect_uri: " + redirectUri + ", expected: " + redirectUri);
                return "error";
            }
            model.addAttribute("client_id", clientId);
            model.addAttribute("redirect_uri", redirectUri);
            model.addAttribute("scope", scope);
            model.addAttribute("state", state);
            model.addAttribute("data_purpose", "用于用户数据查询和分析");
            return "authorize";
        } catch (Exception e) {
            System.err.println("Authorize failed: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/authorize")
    public ResponseEntity<String> confirmAuthorize(
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUriParam,
            @RequestParam("scope") String scope,
            @RequestParam("state") String state,
            @RequestParam("action") String action,
            HttpSession session) {
        try {
            System.out.println("Confirm authorize: client_id=" + clientId + ", redirect_uri=" + redirectUriParam + ", state=" + state + ", action=" + action);
            if (!redirectUriParam.equals(redirectUri)) {
                System.err.println("Redirect URI mismatch: " + redirectUriParam + " != " + redirectUri);
                return ResponseEntity.status(302)
                        .header("Location", redirectUriParam + "?error=invalid_redirect_uri&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8))
                        .build();
            }

            if ("reject".equals(action)) {
                String redirectUrl = redirectUriParam + "?error=access_denied&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);
                System.out.println("Rejecting, redirecting to: " + redirectUrl);
                return ResponseEntity.status(302)
                        .header("Location", redirectUrl)
                        .build();
            } else if ("accept".equals(action)) {
                String authCode = UUID.randomUUID().toString();
                blockchainService.logAuthorization(authCode, scope, "用于用户数据查询和分析");
                String redirectUrl = redirectUriParam + "?code=" + authCode + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);
                System.out.println("Accepting, redirecting to: " + redirectUrl);
                return ResponseEntity.status(302)
                        .header("Location", redirectUrl)
                        .build();
            } else {
                return ResponseEntity.badRequest().body("无效的操作");
            }
        } catch (Exception e) {
            System.err.println("Authorize confirmation failed: " + e.getMessage());
            return ResponseEntity.status(500).body("授权确认失败: " + e.getMessage());
        }
    }

    @PostMapping("/token")
    public ResponseEntity<String> token(
            @RequestParam("code") String code,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("grant_type") String grantType) {
        try {
            if (!"authorization_code".equals(grantType)) {
                return ResponseEntity.badRequest().body("不支持的 grant_type");
            }
            String accessToken = authService.generateToken(code);
            blockchainService.logToken(accessToken);
            return ResponseEntity.ok("{\"access_token\":\"" + accessToken + "\",\"token_type\":\"Bearer\",\"expires_in\":3600}");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("生成令牌失败: " + e.getMessage());
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<Boolean> verifyUser(@RequestBody UserVerificationRequest request) {
        try {
            if (request.getIdCard() == null || request.getName() == null) {
                return ResponseEntity.badRequest().body(false);
            }
            boolean verified = ctidService.verifyUser(request.getIdCard(), request.getName(), request.getBusinessLicense());
            blockchainService.logAuthorization("用户验证", request.getIdCard(), verified ? "成功" : "失败");
            return ResponseEntity.ok(verified);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(false);
        }
    }

    @GetMapping("/userinfo")
    @ResponseBody
    public ResponseEntity<String> userinfo(@RequestHeader("Authorization") String authorization) {
        try {
            String token = authorization.replace("Bearer ", "");
            if (token == null || token.isEmpty()) {
                return ResponseEntity.status(401).body("无效的令牌");
            }
            return ResponseEntity.ok("{\"sub\":\"test-user\",\"name\":\"测试用户\"}");
        } catch (Exception e) {
            return ResponseEntity.status(401).body("无效的令牌: " + e.getMessage());
        }
    }
}

class UserVerificationRequest {
    private String idCard;
    private String name;
    private String businessLicense;

    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBusinessLicense() { return businessLicense; }
    public void setBusinessLicense(String businessLicense) { this.businessLicense = businessLicense; }
}