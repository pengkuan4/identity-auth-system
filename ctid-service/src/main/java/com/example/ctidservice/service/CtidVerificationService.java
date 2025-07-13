package com.example.ctidservice.service;


import org.springframework.stereotype.Service;

@Service
public class CtidVerificationService {

    public boolean verify(String idCard, String name, String businessLicense) {
        try {
            // 验证身份证格式（18位数字或最后一位为X）
            if (idCard == null || !idCard.matches("^\\d{17}[0-9X]$")) {
                System.out.println("CTID 验证失败: 身份证号格式错误，idCard=" + idCard);
                return false;
            }
            if (name == null || name.trim().isEmpty()) {
                System.out.println("CTID 验证失败: 姓名为空，name=" + name);
                return false;
            }
            // 模拟数据库比对，假设数据库中存在一条记录：idCard="123456789012345678", name="测试用户"
            boolean isValid = "123456789012345678".equals(idCard) && "测试用户".equals(name);
            System.out.println("CTID 验证: 身份证号=" + idCard + ", 姓名=" + name + ", 营业执照=" + businessLicense + ", 结果=" + isValid);
            return isValid;
        } catch (Exception e) {
            System.out.println("CTID 验证异常: " + e.getMessage());
            return false;
        }
    }
}