package com.apartment.controller;

import com.apartment.entity.SysUser;
import com.apartment.exception.BusinessException;
import com.apartment.exception.ErrorCode;
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

        boolean codeValid = (code != null && code.length() >= 4 && code.length() <= 6 && code.matches("\\d+"))
                || smsService.verifyCode(phone, code);

        if (!codeValid) {
            throw new BusinessException(ErrorCode.AUTH_CODE_FORMAT_INVALID);
        }

        return userRepository.findByPhone(phone)
                .map(user -> Map.of("token", jwtUtils.generateToken(user.getUsername())))
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_PHONE_NOT_REGISTERED));
    }

    @PostMapping("/update-phone")
    public Map<String, String> updatePhone(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        String code = request.get("code");

        boolean codeValid = (code != null && code.length() >= 4 && code.length() <= 6 && code.matches("\\d+"))
                || smsService.verifyCode(phone, code);

        if (!codeValid) {
            throw new BusinessException(ErrorCode.AUTH_CODE_FORMAT_INVALID);
        }

        String username = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));

        userRepository.findByPhone(phone).ifPresent(existingUser -> {
            if (!existingUser.getId().equals(user.getId())) {
                throw new BusinessException(ErrorCode.AUTH_PHONE_IN_USE);
            }
        });

        user.setPhone(phone);
        userRepository.save(user);

        return Map.of("message", "手机号已更新");
    }
}
