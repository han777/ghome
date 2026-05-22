package com.apartment.repository;

import com.apartment.entity.SysConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SysConfigRepository extends JpaRepository<SysConfig, Long> {

    Optional<SysConfig> findFirstByOrderByIdAsc();
}
