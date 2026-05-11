package com.apartment.repository;

import com.apartment.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SysUserRepository extends JpaRepository<SysUser, Long> {
    Optional<SysUser> findByUsername(String username);
    Optional<SysUser> findByEmail(String email);
    Optional<SysUser> findByPhone(String phone);
    Optional<SysUser> findByWecomId(String wecomId);
}
