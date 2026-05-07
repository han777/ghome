package com.apartment.controller;

import com.apartment.entity.*;
import com.apartment.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/sys")
public class SysManagementController {
    @Autowired private SysUserRepository userRepository;
    @Autowired private SysRoleRepository roleRepository;
    @Autowired private SysMenuRepository menuRepository;
    @Autowired private SysDepartmentRepository deptRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // --- Users ---
    @GetMapping("/users") public List<SysUser> getUsers() { return userRepository.findAll(); }
    @GetMapping("/profile")
    public SysUser getProfile() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }
    @PostMapping("/users")
    public SysUser saveUser(@RequestBody SysUser user) {
        if (user.getId() == null) {
            user.setPassword(passwordEncoder.encode("123456")); // Default password for new users
        } else {
            SysUser existing = userRepository.findById(user.getId()).orElse(null);
            if (existing != null && (user.getPassword() == null || user.getPassword().isEmpty())) {
                user.setPassword(existing.getPassword()); // Keep existing password if not changed
            }
        }
        return userRepository.save(user);
    }
    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable Long id) { userRepository.deleteById(id); }

    // --- Roles ---
    @GetMapping("/roles") public List<SysRole> getRoles() { return roleRepository.findAll(); }
    @PostMapping("/roles")
    public SysRole saveRole(@RequestBody SysRole role) { return roleRepository.save(role); }
    @DeleteMapping("/roles/{id}")
    public void deleteRole(@PathVariable Long id) { roleRepository.deleteById(id); }

    // --- Depts ---
    @GetMapping("/depts") public List<SysDepartment> getDepts() { return deptRepository.findAll(); }
    @PostMapping("/depts")
    public SysDepartment saveDept(@RequestBody SysDepartment dept) { return deptRepository.save(dept); }
    @DeleteMapping("/depts/{id}")
    public void deleteDept(@PathVariable Long id) { deptRepository.deleteById(id); }

    // --- Menus ---
    @GetMapping("/menus") public List<SysMenu> getMenus() { return menuRepository.findAll(); }
}
