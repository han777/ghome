package com.apartment.controller;

import com.apartment.entity.BookingPurpose;
import com.apartment.repository.BookingPurposeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/booking-purposes")
public class BookingPurposeController {

    @Autowired
    private BookingPurposeRepository repository;

    @GetMapping("/all")
    public List<BookingPurpose> getAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparingInt(BookingPurpose::getSortOrder))
                .toList();
    }

    @PostMapping
    public ResponseEntity<?> save(@RequestBody BookingPurpose purpose) {
        // Validate name uniqueness
        List<BookingPurpose> all = repository.findAll();
        boolean nameExists = all.stream()
                .anyMatch(p -> p.getName().equals(purpose.getName())
                        && !p.getId().equals(purpose.getId()));
        if (nameExists) {
            return ResponseEntity.badRequest().body(Map.of("error", "事由名称已存在"));
        }

        if (purpose.getId() != null) {
            // Update existing
            BookingPurpose existing = repository.findById(purpose.getId()).orElse(null);
            if (existing == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "事由不存在"));
            }
            // System purposes: allow name change, prevent bizType/isSystem change
            if (existing.getIsSystem()) {
                existing.setName(purpose.getName());
                existing.setSortOrder(purpose.getSortOrder());
                // bizType and isSystem remain unchanged
            } else {
                existing.setName(purpose.getName());
                existing.setBizType(purpose.getBizType());
                existing.setSortOrder(purpose.getSortOrder());
            }
            return ResponseEntity.ok(repository.save(existing));
        } else {
            // Create new — never allow user to set isSystem=true
            purpose.setIsSystem(false);
            return ResponseEntity.ok(repository.save(purpose));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        BookingPurpose existing = repository.findById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        if (existing.getIsSystem()) {
            return ResponseEntity.badRequest().body(Map.of("error", "系统预设事由不可删除"));
        }
        repository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
