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
        java.time.LocalDate start = java.time.LocalDate.parse(startDate);
        java.time.LocalDate end = java.time.LocalDate.parse(endDate);
        
        java.time.LocalDateTime startDT = start.atTime(14, 0);
        java.time.LocalDateTime endDT = end.atTime(12, 0);
        
        // 1. Get rooms occupied by active orders
        java.util.List<Long> occupiedRoomIds = orderRepository.findOccupiedRoomIds(start, end);
        
        // 2. Get rooms under maintenance
        java.util.List<com.apartment.entity.RoomMaintenance> maintenances = maintenanceRepository.findActiveMaintenancesInPeriod(startDT, endDT);
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
}
