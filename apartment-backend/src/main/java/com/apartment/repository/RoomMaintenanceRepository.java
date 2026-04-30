package com.apartment.repository;

import com.apartment.entity.RoomMaintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.time.LocalDateTime;

public interface RoomMaintenanceRepository extends JpaRepository<RoomMaintenance, Long> {
    List<RoomMaintenance> findByRoomId(Long roomId);

    @Query("SELECT COUNT(m) FROM RoomMaintenance m WHERE m.room.id = ?1 AND m.startTime <= ?3 AND m.endTime >= ?2 AND m.status <> 2")
    long countOverlappingMaintenances(Long roomId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT m FROM RoomMaintenance m WHERE m.startTime <= ?2 AND m.endTime >= ?1 AND m.status <> 2")
    List<RoomMaintenance> findActiveMaintenancesInPeriod(LocalDateTime start, LocalDateTime end);
}
