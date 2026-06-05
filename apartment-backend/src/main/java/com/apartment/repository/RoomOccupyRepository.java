package com.apartment.repository;

import com.apartment.entity.RoomOccupy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RoomOccupyRepository extends JpaRepository<RoomOccupy, Long> {
    List<RoomOccupy> findByOrderId(Long orderId);
    List<RoomOccupy> findByRoomIdAndStatus(Long roomId, Integer status);

    // 查找当前入住的记录（status=1 已入住）
    List<RoomOccupy> findByStatus(Integer status);

    // 查找当日入住的记录（用于生成日常保洁，仅已入住房间）
    @Query("SELECT ro FROM RoomOccupy ro WHERE ro.status = 1 AND ro.checkInTime <= :endOfToday")
    List<RoomOccupy> findCurrentOccupying(@Param("endOfToday") LocalDateTime endOfToday);

    // 查找活跃入住记录（待入住+已入住）
    @Query("SELECT ro FROM RoomOccupy ro WHERE ro.room.id = ?1 AND ro.status IN (0, 1)")
    List<RoomOccupy> findActiveByRoomId(Long roomId);

    // 查找当日退房的记录（用于生成强打扫）
    @Query("SELECT ro FROM RoomOccupy ro WHERE ro.checkOutTime BETWEEN :startOfDay AND :endOfDay")
    List<RoomOccupy> findCheckingOutToday(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
}
