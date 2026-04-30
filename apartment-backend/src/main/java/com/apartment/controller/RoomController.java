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
