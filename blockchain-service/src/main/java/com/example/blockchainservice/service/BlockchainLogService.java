package com.example.blockchainservice.service;


import org.springframework.stereotype.Service;

@Service
public class BlockchainLogService {

    public void log(String logData) {
        // 模拟区块链日志记录
        System.out.println("区块链日志: " + logData);
    }
}