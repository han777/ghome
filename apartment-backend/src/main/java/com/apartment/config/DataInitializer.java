package com.apartment.config;

import com.apartment.entity.*;
import com.apartment.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;


@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(
            SysUserRepository userRepository,
            SysRoleRepository roleRepository,
            SysDictRepository dictRepository,
            SysDictItemRepository dictItemRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            // 1. Initialize Roles if not exists
            if (roleRepository.count() == 0) {
                SysRole adminRole = new SysRole();
                adminRole.setRoleCode("ROLE_ADMIN");
                adminRole.setRoleName("Administrator");
                roleRepository.save(adminRole);

                SysRole userRole = new SysRole();
                userRole.setRoleCode("ROLE_USER");
                userRole.setRoleName("Standard User");
                roleRepository.save(userRole);
            }

            // 2. Initialize Admin User if not exists
            if (userRepository.findByUsername("admin").isEmpty()) {
                SysUser admin = new SysUser();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin"));
                admin.setRealName("System Admin");
                admin.setPhone("18888888888");
                admin.setStatus(1);
                
                SysRole adminRole = roleRepository.findAll().stream()
                        .filter(r -> r.getRoleCode().equals("ROLE_ADMIN"))
                        .findFirst().orElse(null);
                
                if (adminRole != null) {
                    java.util.Set<SysRole> roles = new java.util.HashSet<>();
                    roles.add(adminRole);
                    admin.setRoles(roles);
                }
                userRepository.save(admin);
                System.out.println("Default Admin account created: admin / admin (Phone: 18888888888)");
            }

            // Add a test user for mobile login
            if (userRepository.findByUsername("testuser").isEmpty()) {
                SysUser testUser = new SysUser();
                testUser.setUsername("testuser");
                testUser.setPassword(passwordEncoder.encode("password"));
                testUser.setRealName("Test Mobile User");
                testUser.setPhone("13800138000");
                testUser.setStatus(1);
                
                SysRole userRole = roleRepository.findAll().stream()
                        .filter(r -> r.getRoleCode().equals("ROLE_USER"))
                        .findFirst().orElse(null);
                
                if (userRole != null) {
                    java.util.Set<SysRole> roles = new java.util.HashSet<>();
                    roles.add(userRole);
                    testUser.setRoles(roles);
                }
                userRepository.save(testUser);
                System.out.println("Test Mobile User created: 13800138000");
            }

            // 3. Initialize Dictionaries from Enums
            initEnumDict(dictRepository, dictItemRepository, "ROOM_STATUS", "Room Status", new String[][]{
                    {"Available", "0"},
                    {"Locked", "1"}
            });

            initEnumDict(dictRepository, dictItemRepository, "ORDER_STATUS", "Order Status", new String[][]{
                    {"Cooling-off", "0"},
                    {"Pending", "1"},
                    {"In", "2"},
                    {"Out", "3"},
                    {"Canceled", "4"}
            });

            initEnumDict(dictRepository, dictItemRepository, "BIZ_TYPE", "Business Type", new String[][]{
                    {"Short Rent", "1"},
                    {"Long Rent", "2"}
            });
        };
    }

    private void initEnumDict(SysDictRepository dictRepository, SysDictItemRepository dictItemRepository, 
                               String code, String name, String[][] items) {
        if (dictRepository.findByDictCode(code).isEmpty()) {
            SysDict dict = new SysDict();
            dict.setDictCode(code);
            dict.setDictName(name);
            SysDict savedDict = dictRepository.save(dict);

            for (int i = 0; i < items.length; i++) {
                SysDictItem item = new SysDictItem();
                item.setDict(savedDict);
                item.setItemLabel(items[i][0]);
                item.setItemValue(items[i][1]);
                item.setSortOrder(i);
                dictItemRepository.save(item);
            }
        }
    }
}
