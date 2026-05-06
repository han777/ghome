package com.apartment.controller;

import com.apartment.entity.RoomOccupy;
import com.apartment.repository.RoomOccupyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/occupies")
public class RoomOccupyController {
    @Autowired
    private RoomOccupyRepository occupyRepository;

    @GetMapping("/all")
    public List<RoomOccupy> getAll() {
        return occupyRepository.findAll();
    }
}
