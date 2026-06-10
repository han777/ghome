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
            SysMenuRepository menuRepository,
            SysDictRepository dictRepository,
            SysDictItemRepository dictItemRepository,
            PasswordEncoder passwordEncoder,
            org.springframework.jdbc.core.JdbcTemplate jdbcTemplate,
            BookingPurposeRepository bookingPurposeRepository) {
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

            // Initialize ROLE_FINANCE if not exists
            if (roleRepository.findByRoleCode("ROLE_FINANCE").isEmpty()) {
                SysRole financeRole = new SysRole();
                financeRole.setRoleCode("ROLE_FINANCE");
                financeRole.setRoleName("Finance");
                roleRepository.save(financeRole);
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

            // Initialize menus if not exists
            if (menuRepository.count() == 0) {
                // Group 1: 客房业务
                SysMenu g1 = new SysMenu();
                g1.setMenuName("客房业务"); g1.setIcon("🏨"); g1.setSortOrder(0);
                g1 = menuRepository.save(g1);

                menuRepository.save(createMenu("房态仪表盘", "/admin/dashboard", "📊", g1.getId(), 0));
                menuRepository.save(createMenu("入住管理", "/admin/orders", "📝", g1.getId(), 1));
                menuRepository.save(createMenu("线性房态", "/admin/gantt", "📅", g1.getId(), 2));
                menuRepository.save(createMenu("房型预测报表", "/admin/room-type-forecast", "📉", g1.getId(), 3));
                menuRepository.save(createMenu("清扫任务", "/admin/cleaning-tasks", "🧹", g1.getId(), 4));
                menuRepository.save(createMenu("维修管理", "/admin/maintenances", "🔧", g1.getId(), 5));

                // Group 2: 财务报表
                SysMenu g2 = new SysMenu();
                g2.setMenuName("财务报表"); g2.setIcon("📈"); g2.setSortOrder(1);
                g2 = menuRepository.save(g2);

                menuRepository.save(createMenu("财务报表", "/admin/reports", "📈", g2.getId(), 0));
                menuRepository.save(createMenu("房间费结算明细", "/admin/room-fee-detail", "🧾", g2.getId(), 1));
                menuRepository.save(createMenu("商品服务费结算明细", "/admin/service-fee-detail", "📋", g2.getId(), 2));

                // Group 3: 数据管理
                SysMenu g3 = new SysMenu();
                g3.setMenuName("数据管理"); g3.setIcon("📊"); g3.setSortOrder(2);
                g3 = menuRepository.save(g3);

                menuRepository.save(createMenu("楼栋管理", "/admin/buildings", "🏘️", g3.getId(), 0));
                menuRepository.save(createMenu("房型价格", "/admin/room-types", "🛌", g3.getId(), 1));
                menuRepository.save(createMenu("房间列表", "/admin/rooms", "🏠", g3.getId(), 2));
                menuRepository.save(createMenu("商品服务价格", "/admin/product-prices", "🏷️", g3.getId(), 3));
                menuRepository.save(createMenu("订房事由", "/admin/booking-purposes", "📋", g3.getId(), 4));

                // Group 4: 系统管理
                SysMenu g4 = new SysMenu();
                g4.setMenuName("系统管理"); g4.setIcon("⚙️"); g4.setSortOrder(3);
                g4 = menuRepository.save(g4);

                menuRepository.save(createMenu("用户管理", "/admin/accounts", "👤", g4.getId(), 0));
                menuRepository.save(createMenu("角色管理", "/admin/roles", "🛡️", g4.getId(), 1));
                menuRepository.save(createMenu("字典管理", "/admin/dicts", "📖", g4.getId(), 2));
                menuRepository.save(createMenu("部门管理", "/admin/depts", "🏢", g4.getId(), 3));
                menuRepository.save(createMenu("全局设置", "/admin/global-settings", "🔧", g4.getId(), 4));
                menuRepository.save(createMenu("通知记录", "/admin/notification-records", "🔔", g4.getId(), 5));
                menuRepository.save(createMenu("定时任务", "/admin/scheduled-task-logs", "⏰", g4.getId(), 6));
            }

            // Bind roles to menus
            SysRole adminRole = roleRepository.findByRoleCode("ROLE_ADMIN").orElse(null);
            if (adminRole != null) {
                java.util.List<SysMenu> allMenus = menuRepository.findAll();
                for (SysMenu menu : allMenus) {
                    if (menu.getRoles() == null) menu.setRoles(new java.util.HashSet<>());
                    if (!menu.getRoles().contains(adminRole)) {
                        menu.getRoles().add(adminRole);
                        menuRepository.save(menu);
                    }
                }
            }

            SysRole financeRole = roleRepository.findByRoleCode("ROLE_FINANCE").orElse(null);
            if (financeRole != null) {
                java.util.List<SysMenu> allMenus = menuRepository.findAll();
                SysMenu financeGroup = allMenus.stream()
                    .filter(m -> "财务报表".equals(m.getMenuName()) && m.getParentId() == null)
                    .findFirst().orElse(null);
                if (financeGroup != null) {
                    if (financeGroup.getRoles() == null) financeGroup.setRoles(new java.util.HashSet<>());
                    if (!financeGroup.getRoles().contains(financeRole)) {
                        financeGroup.getRoles().add(financeRole);
                        menuRepository.save(financeGroup);
                    }
                    for (SysMenu menu : allMenus) {
                        if (financeGroup.getId().equals(menu.getParentId())) {
                            if (menu.getRoles() == null) menu.setRoles(new java.util.HashSet<>());
                            if (!menu.getRoles().contains(financeRole)) {
                                menu.getRoles().add(financeRole);
                                menuRepository.save(menu);
                            }
                        }
                    }
                }
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

            initEnumDict(dictRepository, dictItemRepository, "OCCUPY_STATUS", "Occupy Status", new String[][]{
                    {"Pending", "0"},
                    {"Checked-in", "1"},
                    {"Checked-out", "2"},
                    {"Canceled", "3"}
            });

            initEnumDict(dictRepository, dictItemRepository, "BIZ_TYPE", "Business Type", new String[][]{
                    {"Short Rent", "1"},
                    {"Long Rent", "2"}
            });

            initEnumDict(dictRepository, dictItemRepository, "USER_SOURCE", "User Source", new String[][]{
                    {"Manual", "0"},
                    {"WeCom", "1"}
            });

            initEnumDict(dictRepository, dictItemRepository, "CLEANING_TASK_TYPE", "Cleaning Task Type", new String[][]{
                    {"日常保洁", "1"},
                    {"强打扫", "2"}
            });

            initEnumDict(dictRepository, dictItemRepository, "CLEANING_TASK_STATUS", "Cleaning Task Status", new String[][]{
                    {"计划", "0"},
                    {"取消", "1"},
                    {"完成", "2"}
            });

            // 4. Initialize Booking Purposes
            if (bookingPurposeRepository.count() == 0) {
                BookingPurpose p1 = new BookingPurpose();
                p1.setName("差旅"); p1.setBizType(1); p1.setIsSystem(true); p1.setSortOrder(0);
                bookingPurposeRepository.save(p1);

                BookingPurpose p2 = new BookingPurpose();
                p2.setName("活动"); p2.setBizType(1); p2.setIsSystem(true); p2.setSortOrder(1);
                bookingPurposeRepository.save(p2);

                BookingPurpose p3 = new BookingPurpose();
                p3.setName("长租"); p3.setBizType(2); p3.setIsSystem(true); p3.setSortOrder(2);
                bookingPurposeRepository.save(p3);
            }

            // 5. Migrate user sources
            try {
                jdbcTemplate.update("UPDATE sys_user SET source = '0' WHERE source = 'system'");
                jdbcTemplate.update("UPDATE sys_user SET source = '1' WHERE source = 'wecom'");
                System.out.println("User source data migration completed successfully.");
            } catch (Exception e) {
                System.err.println("User source data migration failed: " + e.getMessage());
            }
        };
    }

    private SysMenu createMenu(String name, String path, String icon, Long parentId, int sortOrder) {
        SysMenu menu = new SysMenu();
        menu.setMenuName(name);
        menu.setPath(path);
        menu.setIcon(icon);
        menu.setParentId(parentId);
        menu.setSortOrder(sortOrder);
        return menu;
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
