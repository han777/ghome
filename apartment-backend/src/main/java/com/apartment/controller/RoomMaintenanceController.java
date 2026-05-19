package com.apartment.controller;

import com.apartment.entity.RoomMaintenance;
import com.apartment.exception.BusinessException;
import com.apartment.exception.ErrorCode;
import com.apartment.repository.RoomMaintenanceRepository;
import com.apartment.repository.RoomOrderRepository;

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
        if (maintenance.getStartTime() == null) {
            maintenance.setStartTime(LocalDateTime.now());
        }
        if (maintenance.getEndTime() == null) {
            maintenance.setEndTime(maintenance.getStartTime().plusYears(3));
        }

        if (maintenance.getStartTime().isAfter(maintenance.getEndTime())) {
            throw new BusinessException(ErrorCode.MAINT_START_AFTER_END);
        }

        // Only check overlap if it's not cancelled
        if (maintenance.getStatus() != 2) {
            long orderCount = orderRepository.countOverlappingActiveOrders(maintenance.getRoom().getId(), maintenance.getStartTime(), maintenance.getEndTime());
            if (orderCount > 0) {
                throw new BusinessException(ErrorCode.MAINT_ORDER_CONFLICT);
            }

            java.util.List<RoomMaintenance> others = maintenanceRepository.findByRoomId(maintenance.getRoom().getId());
            for (RoomMaintenance other : others) {
                if (other.getStatus() == 2) continue;
                if (maintenance.getId() != null && maintenance.getId().equals(other.getId())) continue;

                if (maintenance.getStartTime().isBefore(other.getEndTime()) && maintenance.getEndTime().isAfter(other.getStartTime())) {
                    throw new BusinessException(ErrorCode.MAINT_OTHER_CONFLICT);
                }
            }
        }

        if (maintenance.getId() != null) {
            RoomMaintenance existing = maintenanceRepository.findById(maintenance.getId()).orElse(null);
            if (existing != null && existing.getStatus() == 1 && maintenance.getStatus() != 1 && maintenance.getStatus() != 2) {
                throw new BusinessException(ErrorCode.MAINT_COMPLETED_NO_REVERT);
            }
        }

        return maintenanceRepository.save(maintenance);
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
