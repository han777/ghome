package com.apartment.controller;

import com.apartment.entity.RoomMaintenance;
import com.apartment.repository.RoomMaintenanceRepository;
import com.apartment.repository.RoomOrderRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<?> saveMaintenance(@RequestBody RoomMaintenance maintenance) {
        if (maintenance.getStartTime() == null) {
            maintenance.setStartTime(LocalDateTime.now());
        }
        if (maintenance.getEndTime() == null) {
            maintenance.setEndTime(maintenance.getStartTime().plusYears(3));
        }

        if (maintenance.getStartTime().isAfter(maintenance.getEndTime())) {
            return ResponseEntity.badRequest().body("开始时间不能晚于结束时间");
        }

        // Only check overlap if it's not cancelled
        if (maintenance.getStatus() != 2) {
            long orderCount = orderRepository.countOverlappingActiveOrders(maintenance.getRoom().getId(), maintenance.getStartTime(), maintenance.getEndTime());
            if (orderCount > 0) {
                return ResponseEntity.badRequest().body("维修时间与执行中的订单时间重叠，不允许保存");
            }
        }

        if (maintenance.getId() != null) {
            RoomMaintenance existing = maintenanceRepository.findById(maintenance.getId()).orElse(null);
            if (existing != null && existing.getStatus() == 1) {
                return ResponseEntity.badRequest().body("已完成的维修记录不允许编辑");
            }
        }

        return ResponseEntity.ok(maintenanceRepository.save(maintenance));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMaintenance(@PathVariable Long id) {
        RoomMaintenance existing = maintenanceRepository.findById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        if (existing.getStatus() == 1) {
            return ResponseEntity.badRequest().body("已完成的维修记录不允许删除");
        }
        if (existing.getStatus() == 0) {
            return ResponseEntity.badRequest().body("维修中的记录不允许删除，只能取消");
        }
        maintenanceRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
