package com.apartment.controller;

import com.apartment.entity.RoomMaintenance;
import com.apartment.exception.BusinessException;
import com.apartment.exception.ErrorCode;
import com.apartment.repository.RoomMaintenanceRepository;
import com.apartment.repository.RoomOrderRepository;
import com.apartment.service.RoomMaintenanceService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/maintenances")
public class RoomMaintenanceController {

    @Autowired
    private RoomMaintenanceRepository maintenanceRepository;

    @Autowired
    private RoomOrderRepository orderRepository;

    @Autowired
    private RoomMaintenanceService maintenanceService;



    @GetMapping("/all")
    public List<RoomMaintenance> getAllMaintenances() {
        return maintenanceRepository.findAll();
    }

    @GetMapping("/room/{roomId}")
    public List<RoomMaintenance> getByRoomId(@PathVariable Long roomId) {
        return maintenanceRepository.findByRoomId(roomId);
    }

    @PostMapping
    public RoomMaintenance saveMaintenance(@RequestBody RoomMaintenance maintenance) {
        if (maintenance.getRoom() == null || maintenance.getRoom().getId() == null) {
            throw new BusinessException(ErrorCode.GENERAL_ROOM_NOT_FOUND);
        }

        if (maintenance.getStartTime() == null) {
            maintenance.setStartTime(LocalDateTime.now());
        }
        if (maintenance.getEndTime() == null) {
            maintenance.setEndTime(maintenance.getStartTime().plusYears(3));
        }

        if (maintenance.getStartTime().isAfter(maintenance.getEndTime())) {
            throw new BusinessException(ErrorCode.MAINT_START_AFTER_END);
        }

        Long roomId = maintenance.getRoom().getId();

        // Only check overlap if it's not cancelled
        if (maintenance.getStatus() != 2) {
            long orderCount = orderRepository.countOverlappingActiveOrders(roomId, maintenance.getStartTime(), maintenance.getEndTime());
            if (orderCount > 0) {
                throw new BusinessException(ErrorCode.MAINT_ORDER_CONFLICT);
            }

            long maintConflictCount = maintenanceRepository.countOverlappingActiveMaintenances(roomId, maintenance.getStartTime(), maintenance.getEndTime());
            // Exclude self when editing
            if (maintenance.getId() != null) {
                RoomMaintenance self = maintenanceRepository.findById(maintenance.getId()).orElse(null);
                if (self != null && self.getStartTime().isBefore(maintenance.getEndTime()) && self.getEndTime().isAfter(maintenance.getStartTime())) {
                    maintConflictCount--;
                }
            }
            if (maintConflictCount > 0) {
                throw new BusinessException(ErrorCode.MAINT_OTHER_CONFLICT);
            }
        }

        RoomMaintenance existing = null;
        if (maintenance.getId() != null) {
            existing = maintenanceRepository.findById(maintenance.getId()).orElse(null);
            if (existing != null && existing.getStatus() == 1 && maintenance.getStatus() != 1 && maintenance.getStatus() != 2) {
                throw new BusinessException(ErrorCode.MAINT_COMPLETED_NO_REVERT);
            }
        }

        boolean wasNotCompleted = existing == null || existing.getStatus() != 1;

        RoomMaintenance saved = maintenanceRepository.save(maintenance);

        // 维护完成（人工完成）时触发强打扫任务
        if (saved.getStatus() == 1 && wasNotCompleted) {
            maintenanceService.completeMaintenance(saved);
        }

        return saved;
    }

    @DeleteMapping("/{id}")
    public void deleteMaintenance(@PathVariable Long id) {
        RoomMaintenance existing = maintenanceRepository.findById(id).orElse(null);
        if (existing == null) {
            return;
        }
        if (existing.getStatus() == 1) {
            throw new BusinessException(ErrorCode.MAINT_COMPLETED_NO_DELETE);
        }
        if (existing.getStatus() == 0) {
            throw new BusinessException(ErrorCode.MAINT_ACTIVE_NO_DELETE);
        }
        maintenanceRepository.deleteById(id);
    }
}
