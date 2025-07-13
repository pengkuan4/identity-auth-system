package com.example.blockchainservice.controller;



import com.example.blockchainservice.service.BlockchainLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/blockchain")
public class BlockchainController {

    @Autowired
    private BlockchainLogService logService;

    @PostMapping("/log")
    public ResponseEntity<String> log(@RequestBody String logData) {
        logService.log(logData);
        return ResponseEntity.ok("日志已记录");
    }
}