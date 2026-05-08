package com.apartment.controller;

import com.apartment.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private com.apartment.service.SmsService smsService;
    @Autowired
    private com.apartment.repository.SysUserRepository userRepository;

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.get("username"), loginRequest.get("password")));
        String jwt = jwtUtils.generateToken(authentication.getName());
        return Map.of("token", jwt);
    }

    @PostMapping("/send-code")
    public Map<String, String> sendCode(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        smsService.sendCode(phone);
        return Map.of("message", "验证码已发送");
    }

    @PostMapping("/mobile-login")
    public Map<String, String> mobileLogin(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        String code = request.get("code");
        
        if (smsService.verifyCode(phone, code)) {
            return userRepository.findByPhone(phone)
                    .map(user -> Map.of("token", jwtUtils.generateToken(user.getUsername())))
                    .orElseThrow(() -> new RuntimeException("手机号未注册"));
        } else {
            throw new RuntimeException("验证码错误");
        }
    }
}
