package com.apartment.controller;

import com.apartment.entity.Building;
import com.apartment.entity.Floor;
import com.apartment.repository.BuildingRepository;
import com.apartment.repository.FloorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
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
        // 处理楼层的编辑标记
        List<Floor> floors = building.getFloors();
        if (floors != null && !floors.isEmpty()) {
            List<Floor> processedFloors = new ArrayList<>();
            for (Floor f : floors) {
                String flag = f.get_editFlag();
                if (flag == null || flag.isEmpty() || "unchanged".equals(flag)) {
                    // 无标记或 unchanged：保留不动（合并到持久化上下文）
                    if (f.getId() != null) {
                        Floor existing = floorRepository.findById(f.getId()).orElse(null);
                        if (existing != null) {
                            existing.setName(f.getName());
                            existing.setBuilding(building);
                            processedFloors.add(existing);
                        }
                    }
                } else if ("added".equals(flag)) {
                    // 新增楼层
                    f.setId(null); // 确保 JPA 视为新实体
                    f.setBuilding(building);
                    processedFloors.add(f);
                } else if ("modified".equals(flag)) {
                    // 修改楼层
                    if (f.getId() != null) {
                        Floor existing = floorRepository.findById(f.getId()).orElse(null);
                        if (existing != null) {
                            existing.setName(f.getName());
                            existing.setBuilding(building);
                            processedFloors.add(existing);
                        }
                    }
                } else if ("deleted".equals(flag)) {
                    // 删除楼层
                    if (f.getId() != null) {
                        floorRepository.deleteById(f.getId());
                    }
                    // 不加入 processedFloors，该楼层被移除
                }
            }
            building.setFloors(processedFloors);
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
