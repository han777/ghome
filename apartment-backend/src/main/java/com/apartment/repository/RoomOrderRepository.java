package com.apartment.repository;

import com.apartment.entity.RoomOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;

public interface RoomOrderRepository extends JpaRepository<RoomOrder, Long> {
    @Query("SELECT COUNT(o) FROM RoomOrder o WHERE o.user.id = ?1 AND o.startDate <= ?2 AND o.endDate >= ?2")
    long countByUserIdAndDate(Long userId, LocalDate date);

    java.util.List<RoomOrder> findByStartDate(LocalDate date);
    java.util.List<RoomOrder> findByEndDate(LocalDate date);
    java.util.List<RoomOrder> findByStartDateBetween(LocalDate start, LocalDate end);

    @Query("SELECT COUNT(o) FROM RoomOrder o WHERE o.room.id = ?1 AND o.status IN (1, 2) AND o.startDate <= ?3 AND o.endDate >= ?2")
    long countOverlappingActiveOrders(Long roomId, LocalDate start, LocalDate end);

    java.util.List<RoomOrder> findByRoomIdAndStatusIn(Long roomId, java.util.Collection<Integer> statuses);
    java.util.List<RoomOrder> findByUserIdAndStatusIn(Long userId, java.util.Collection<Integer> statuses);
}
