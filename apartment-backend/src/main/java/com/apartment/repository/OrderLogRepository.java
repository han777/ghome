package com.apartment.repository;

import com.apartment.entity.OrderLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderLogRepository extends JpaRepository<OrderLog, Long> {
    List<OrderLog> findByOrderIdOrderByOperationTimeDesc(Long orderId);
}
