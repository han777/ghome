package com.apartment.repository;

import com.apartment.entity.SysMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SysMenuRepository extends JpaRepository<SysMenu, Long> {
    List<SysMenu> findByRolesRoleCodeInOrderBySortOrderAsc(Collection<String> roleCodes);
}
