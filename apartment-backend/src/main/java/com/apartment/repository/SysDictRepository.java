package com.apartment.repository;

import com.apartment.entity.SysDict;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SysDictRepository extends JpaRepository<SysDict, Long> {
    Optional<SysDict> findByDictCode(String dictCode);
}
