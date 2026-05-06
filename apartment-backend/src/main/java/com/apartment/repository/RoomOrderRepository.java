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

    @Query("SELECT COUNT(o) FROM RoomOrder o JOIN o.roomOccupies ro WHERE ro.room.id = ?1 AND o.status IN (1, 2) AND o.startDate <= ?3 AND o.endDate >= ?2")
    long countOverlappingActiveOrders(Long roomId, LocalDate start, LocalDate end);

    @Query("SELECT o FROM RoomOrder o JOIN o.roomOccupies ro WHERE ro.room.id = ?1 AND o.status IN ?2")
    java.util.List<RoomOrder> findByRoomIdAndStatusIn(Long roomId, java.util.Collection<Integer> statuses);
    
    java.util.List<RoomOrder> findByUserIdAndStatusIn(Long userId, java.util.Collection<Integer> statuses);
    
    @Query("SELECT o FROM RoomOrder o JOIN o.roomOccupies ro WHERE ro.occupantUser.id = ?1 AND o.status IN ?2")
    java.util.List<RoomOrder> findByOccupantUserIdAndStatusIn(Long occupantUserId, java.util.Collection<Integer> statuses);
    
    @Query("SELECT DISTINCT ro.room.id FROM RoomOrder o JOIN o.roomOccupies ro WHERE o.status IN (1, 2) AND o.startDate < ?2 AND o.endDate > ?1")
    java.util.List<Long> findOccupiedRoomIds(LocalDate start, LocalDate end);

    java.util.Optional<RoomOrder> findTopByOrderNoStartingWithOrderByOrderNoDesc(String prefix);
    
    @Query(value = "SELECT nextval('room_order_no_seq')", nativeQuery = true)
    Long getNextOrderSeq();
}
