package com.apartment.controller;

import com.apartment.entity.Building;
import com.apartment.entity.Floor;
import com.apartment.repository.BuildingRepository;
import com.apartment.repository.FloorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/buildings")
public class BuildingController {
    @Autowired private BuildingRepository buildingRepository;
    @Autowired private FloorRepository floorRepository;

    @GetMapping("/all")
    public List<Building> getAll() {
        return buildingRepository.findAll();
    }

    @PostMapping
    public Building save(@RequestBody Building building) {
        if (building.getFloors() != null) {
            building.getFloors().forEach(f -> f.setBuilding(building));
        }
        return buildingRepository.save(building);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        buildingRepository.deleteById(id);
    }

    @PostMapping("/{buildingId}/floors")
    public Floor addFloor(@PathVariable Long buildingId, @RequestBody Floor floor) {
        Building b = buildingRepository.findById(buildingId).orElseThrow();
        floor.setBuilding(b);
        return floorRepository.save(floor);
    }
}
