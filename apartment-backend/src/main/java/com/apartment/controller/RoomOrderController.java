package com.apartment.controller;

import com.apartment.entity.RoomOrder;
import com.apartment.repository.RoomOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class RoomOrderController {
    @Autowired
    private RoomOrderRepository orderRepository;

    @GetMapping("/all")
    public List<RoomOrder> getAllOrders() {
        return orderRepository.findAll();
    }

    @PostMapping
    public RoomOrder saveOrder(@RequestBody RoomOrder order) {
        return orderRepository.save(order);
    }

    @DeleteMapping("/{id}")
    public void deleteOrder(@PathVariable Long id) {
        orderRepository.deleteById(id);
    }
}
