package com.apartment.repository;

import com.apartment.entity.OrderFee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderFeeRepository extends JpaRepository<OrderFee, Long> {
    List<OrderFee> findByOrderId(Long orderId);
}
