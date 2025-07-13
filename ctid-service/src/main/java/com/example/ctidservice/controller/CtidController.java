package com.example.ctidservice.controller;



import com.example.ctidservice.service.CtidVerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ctid")
public class CtidController {

    @Autowired
    private CtidVerificationService ctidVerificationService;

    @PostMapping("/verify")
    public ResponseEntity<Boolean> verifyUser(@RequestBody VerificationRequest request) {
        try {
            boolean verified = ctidVerificationService.verify(
                    request.getIdCard(),
                    request.getName(),
                    request.getBusinessLicense()
            );
            return ResponseEntity.ok(verified);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(false);
        }
    }
}

class VerificationRequest {
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