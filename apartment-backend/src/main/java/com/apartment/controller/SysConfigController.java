package com.apartment.controller;

import com.apartment.entity.SysConfig;
import com.apartment.service.SysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sys/config")
public class SysConfigController {

    @Autowired
    private SysConfigService configService;

    @GetMapping
    public SysConfig getConfig() {
        return configService.getConfig();
    }

    @PostMapping
    public SysConfig saveConfig(@RequestBody SysConfig config, @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = null;
        if (userDetails != null) {
            // 从 UserDetails 获取用户ID（username 实际存储的是用户ID）
            try {
                userId = Long.parseLong(userDetails.getUsername());
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return configService.saveConfig(config, userId);
    }
}
