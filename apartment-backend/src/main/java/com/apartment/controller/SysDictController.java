package com.apartment.controller;

import com.apartment.entity.SysDict;
import com.apartment.repository.SysDictRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/sys/dict")
public class SysDictController {
    @Autowired
    private SysDictRepository dictRepository;

    @GetMapping("/all")
    public List<SysDict> getAllDicts() {
        return dictRepository.findAll();
    }

    @PostMapping
    public SysDict saveDict(@RequestBody SysDict dict) {
        if (dict.getItems() != null) {
            dict.getItems().forEach(item -> item.setDict(dict));
        }
        return dictRepository.save(dict);
    }

    @DeleteMapping("/{id}")
    public void deleteDict(@PathVariable Long id) {
        dictRepository.deleteById(id);
    }
}
