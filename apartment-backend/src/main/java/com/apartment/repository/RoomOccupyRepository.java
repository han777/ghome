package com.apartment.repository;

import com.apartment.entity.RoomOccupy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RoomOccupyRepository extends JpaRepository<RoomOccupy, Long> {
    List<RoomOccupy> findByOrderId(Long orderId);
    List<RoomOccupy> findByRoomIdAndStatus(Long roomId, Integer status);
}
