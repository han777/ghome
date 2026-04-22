package com.apartment.controller;

import com.apartment.entity.RoomType;
import com.apartment.repository.RoomTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/room-types")
public class RoomTypeController {
    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @GetMapping("/all")
    public List<RoomType> getAll() {
        return roomTypeRepository.findAll();
    }

    @PostMapping
    public RoomType save(@RequestBody RoomType roomType) {
        if (roomType == null) throw new IllegalArgumentException("RoomType cannot be null");
        return roomTypeRepository.save(roomType);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        if (id == null) throw new IllegalArgumentException("ID cannot be null");
        roomTypeRepository.deleteById(id);
    }
}
