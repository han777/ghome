package com.apartment.repository;

import com.apartment.entity.SysDepartment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SysDepartmentRepository extends JpaRepository<SysDepartment, Long> {
}
