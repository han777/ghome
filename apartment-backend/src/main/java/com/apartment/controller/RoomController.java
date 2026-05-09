package com.apartment.controller;

import com.apartment.entity.Room;
import com.apartment.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private com.apartment.repository.RoomMaintenanceRepository maintenanceRepository;
    
    @Autowired
    private com.apartment.repository.RoomOrderRepository orderRepository;

    @GetMapping("/available")
    public List<Room> getAvailableRooms(@RequestParam String startDate, @RequestParam String endDate) {
        java.time.LocalDateTime start = java.time.LocalDateTime.parse(startDate);
        java.time.LocalDateTime end = java.time.LocalDateTime.parse(endDate);
        
        // 1. Get rooms occupied by active orders
        java.util.List<Long> occupiedRoomIds = orderRepository.findOccupiedRoomIds(start, end);
        
        // 2. Get rooms under maintenance
        java.util.List<com.apartment.entity.RoomMaintenance> maintenances = maintenanceRepository.findActiveMaintenancesInPeriod(start, end);
        java.util.Set<Long> unavailableRoomIds = new java.util.HashSet<>(occupiedRoomIds);
        maintenances.forEach(m -> unavailableRoomIds.add(m.getRoom().getId()));
        
        List<Room> allRooms = roomRepository.findAll();
        return allRooms.stream()
                .filter(r -> !unavailableRoomIds.contains(r.getId()))
                .collect(java.util.stream.Collectors.toList());
    }

    @GetMapping("/all")
    public List<Room> getAllRooms() {
        List<Room> rooms = roomRepository.findAll();
        java.time.LocalDateTime startOfDay = java.time.LocalDate.now().atStartOfDay();
        java.time.LocalDateTime endOfDay = java.time.LocalDate.now().atTime(23, 59, 59);
        List<com.apartment.entity.RoomMaintenance> activeMaintenances = maintenanceRepository.findActiveMaintenancesInPeriod(startOfDay, endOfDay);
        java.util.Set<Long> maintenanceRoomIds = activeMaintenances.stream().map(m -> m.getRoom().getId()).collect(java.util.stream.Collectors.toSet());
        for (Room room : rooms) {
            room.setIsMaintenance(maintenanceRoomIds.contains(room.getId()));
        }
        return rooms;
    }

    @PostMapping
    public Room saveRoom(@RequestBody Room room) {
        return roomRepository.save(room);
    }

    @DeleteMapping("/{id}")
    public void deleteRoom(@PathVariable Long id) {
        roomRepository.deleteById(id);
    }
    @GetMapping("/{id}/booked-periods")
    public List<java.util.Map<String, Object>> getRoomBookedPeriods(@PathVariable Long id) {
        java.util.List<java.util.Map<String, Object>> periods = new java.util.ArrayList<>();
        
        // 1. Add maintenance periods
        java.util.List<com.apartment.entity.RoomMaintenance> maintenances = maintenanceRepository.findByRoomId(id);
        for (com.apartment.entity.RoomMaintenance m : maintenances) {
            if (m.getStatus() != 2) { // Not finished/cancelled
                java.util.Map<String, Object> period = new java.util.HashMap<>();
                period.put("start", m.getStartTime());
                period.put("end", m.getEndTime());
                period.put("type", "maintenance");
                periods.add(period);
            }
        }
        
        // 2. Add active order periods
        java.util.List<com.apartment.entity.RoomOrder> orders = orderRepository.findByRoomIdAndStatusIn(id, java.util.Arrays.asList(1, 2));
        for (com.apartment.entity.RoomOrder o : orders) {
            java.util.Map<String, Object> period = new java.util.HashMap<>();
            period.put("start", o.getStartDate());
            period.put("end", o.getEndDate());
            period.put("type", "order");
            period.put("orderId", o.getId());
            periods.add(period);
        }
        
        return periods;
    }
}
