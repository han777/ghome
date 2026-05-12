package com.apartment.controller;

import com.apartment.entity.SysUser;
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
        
        // 模拟验证: 任意4-6位数字验证码均可通过（开发阶段）
        boolean codeValid = (code != null && code.length() >= 4 && code.length() <= 6 && code.matches("\\d+"))
                || smsService.verifyCode(phone, code);

        if (!codeValid) {
            throw new RuntimeException("验证码格式错误，请输入4-6位数字");
        }

        // 仅登录已有用户（不自动创建，用户应由企微OAuth创建）
        return userRepository.findByPhone(phone)
                .map(user -> Map.of("token", jwtUtils.generateToken(user.getUsername())))
                .orElseThrow(() -> new RuntimeException("手机号未注册，请通过企业微信登录"));
    }

    /**
     * 补充手机号：已登录用户（企微OAuth创建）更新phone字段。
     * 需JWT认证，仅补充phone，不创建新用户。
     */
    @PostMapping("/update-phone")
    public Map<String, String> updatePhone(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        String code = request.get("code");

        // 模拟验证: 任意4-6位数字验证码均可通过（开发阶段）
        boolean codeValid = (code != null && code.length() >= 4 && code.length() <= 6 && code.matches("\\d+"))
                || smsService.verifyCode(phone, code);

        if (!codeValid) {
            throw new RuntimeException("验证码格式错误，请输入4-6位数字");
        }

        // 获取当前登录用户
        String username = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查手机号是否已被**其他**用户使用（自己已有的phone可以重复设置）
        userRepository.findByPhone(phone).ifPresent(existingUser -> {
            if (!existingUser.getId().equals(user.getId())) {
                throw new RuntimeException("该手机号已被其他用户使用");
            }
        });

        user.setPhone(phone);
        userRepository.save(user);

        return Map.of("message", "手机号已更新");
    }
}