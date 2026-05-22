package com.apartment.service;

import com.apartment.entity.SysConfig;
import com.apartment.entity.SysUser;
import com.apartment.repository.SysConfigRepository;
import com.apartment.repository.SysUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysConfigService {

    @Autowired
    private SysConfigRepository configRepository;

    @Autowired
    private SysUserRepository userRepository;

    /**
     * 获取系统配置（确保始终返回一个配置对象）
     */
    public SysConfig getConfig() {
        return configRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> {
                    SysConfig config = new SysConfig();
                    return configRepository.save(config);
                });
    }

    /**
     * 保存系统配置
     */
    @Transactional
    public SysConfig saveConfig(SysConfig config, Long updatedByUserId) {
        if (config.getId() == null) {
            // 新配置
            SysConfig existing = configRepository.findFirstByOrderByIdAsc().orElse(null);
            if (existing != null) {
                config.setId(existing.getId());
            }
        }

        if (updatedByUserId != null) {
            SysUser user = userRepository.findById(updatedByUserId).orElse(null);
            config.setUpdatedBy(user);
        }

        return configRepository.save(config);
    }

    /**
     * 获取通知邮箱列表
     */
    public List<String> getNotificationEmails() {
        SysConfig config = getConfig();
        if (config.getNotificationEmails() == null || config.getNotificationEmails().trim().isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(config.getNotificationEmails().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
