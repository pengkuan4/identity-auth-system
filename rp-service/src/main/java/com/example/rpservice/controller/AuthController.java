package com.example.rpservice.controller;


import com.example.rpservice.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpSession;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

@Controller
public class AuthController {

    private final RestTemplate restTemplate;

    @Autowired
    private AuthService authService;

    @Value("${spring.security.oauth2.client.provider.op-service.authorization-uri}")
    private String opAuthorizeEndpoint;

    @Value("${spring.security.oauth2.client.registration.op-service.client-id}")
    private String opClientId;

    @Value("${spring.security.oauth2.client.registration.op-service.redirect-uri}")
    private String opRedirectUri;

    public AuthController(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @GetMapping("/public")
    public String publicEndpoint(@RequestParam(value = "error", required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "认证失败，请检查配置或重试: " + error);
        }
        return "index";
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/verify")
    public String verifyPage(@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient authorizedClient, Model model, HttpSession session) {
        if (authorizedClient != null) {
            model.addAttribute("userName", authorizedClient.getPrincipalName());
        }
        Boolean verified = (Boolean) session.getAttribute("verified");
        model.addAttribute("verified", verified != null && verified);
        System.out.println("Verify page loaded, verified: " + verified);
        return "verify";
    }

    @PostMapping("/verify")
    public String verifyUser(
            @RequestParam("idCard") String idCard,
            @RequestParam("name") String name,
            @RequestParam(value = "businessLicense", required = false) String businessLicense,
            Model model,
            HttpSession session) {

        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("idCard", idCard);
            requestBody.put("name", name);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            String ctidUrl = "http://localhost:8083/ctid/verify";
            String ctidResponse = restTemplate.postForObject(ctidUrl, entity, String.class);

            if ("true".equals(ctidResponse)) {
                session.setAttribute("verified", true);
                model.addAttribute("verified", true);
                System.out.println("Verification successful, set verified: true");
                return "verify";
            } else {
                model.addAttribute("error", "身份验证失败");
                return "verify";
            }
        } catch (Exception e) {
            model.addAttribute("error", "服务器错误: " + e.getMessage());
            return "verify";
        }
    }

    @GetMapping("/start-authorization")
    public String startAuthorization(HttpSession session) {
        Boolean verified = (Boolean) session.getAttribute("verified");
        System.out.println("Start authorization, verified: " + verified);
        if (Boolean.TRUE.equals(verified)) {
            session.removeAttribute("verified");
            try {
                String state = generateState();
                session.setAttribute("oauth2_state", state);
                session.setAttribute("oauth2_registration_id", "op-service");
                String scope = "openid";
                String authorizeUrl = opAuthorizeEndpoint +
                        "?response_type=code" +
                        "&client_id=" + URLEncoder.encode(opClientId, StandardCharsets.UTF_8) +
                        "&redirect_uri=" + URLEncoder.encode(opRedirectUri, StandardCharsets.UTF_8) +
                        "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8) +
                        "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);
                System.out.println("Redirecting to op-service authorize URL: " + authorizeUrl);
                return "redirect:" + authorizeUrl;
            } catch (Exception e) {
                System.err.println("Failed to construct authorize URL: " + e.getMessage());
                return "redirect:/verify";
            }
        }
        System.out.println("No verification, redirecting to /verify");
        return "redirect:/verify";
    }

    private String generateState() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return bytesToHex(bytes);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    @GetMapping("/resource")
    public String resource(@RegisteredOAuth2AuthorizedClient("op-service") OAuth2AuthorizedClient authorizedClient, Model model) {
        String userName = authorizedClient.getPrincipalName();
        model.addAttribute("userName", userName);
        return "resource";
    }

    @GetMapping("/data")
    @ResponseBody
    public ResponseEntity<String> getData(@RegisteredOAuth2AuthorizedClient("op-service") OAuth2AuthorizedClient client) {
        try {
            String userInfo = authService.getUserInfo(client.getAccessToken().getTokenValue());
            authService.logOperation("数据访问", client.getAccessToken().getTokenValue());
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("获取数据失败: " + e.getMessage());
        }
    }
    @PostMapping("/token")
    @ResponseBody
    public ResponseEntity<String> requestToken(
            @RequestParam("code") String code,
            @RequestParam("client_id") String clientId,
            @RequestParam("client_secret") String clientSecret,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("code_verifier") String codeVerifier) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            String body = "code=" + code + "&client_id=" + clientId + "&client_secret=" + clientSecret +
                    "&redirect_uri=" + redirectUri + "&code_verifier=" + codeVerifier + "&grant_type=authorization_code";
            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity("http://localhost:8082/oauth2/token", entity, String.class);
            return response;
        } catch (Exception e) {
            return ResponseEntity.status(500).body("请求令牌失败: " + e.getMessage());
        }
    }
}