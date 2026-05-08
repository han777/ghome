package com.apartment.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SmsService {
    private final Map<String, String> codeMap = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public String sendCode(String phone) {
        String code = String.format("%06d", random.nextInt(1000000));
        codeMap.put(phone, code);
        System.out.println("========================================");
        System.out.println("DEBUG: Sending SMS to " + phone);
        System.out.println("DEBUG: Verification Code: " + code);
        System.out.println("========================================");
        return code;
    }

    public boolean verifyCode(String phone, String code) {
        String savedCode = codeMap.get(phone);
        if (savedCode != null && savedCode.equals(code)) {
            codeMap.remove(phone);
            return true;
        }
        return false;
    }
}
