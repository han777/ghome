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

    @Autowired
    private com.apartment.service.RoomOrderService orderService;

    @GetMapping("/all")
    public List<RoomOrder> getAllOrders() {
        return orderRepository.findAll();
    }

    @PostMapping
    public RoomOrder saveOrder(@RequestBody RoomOrder order) {
        return orderRepository.save(order);
    }

    @PostMapping("/{id}/send-code")
    public RoomOrder sendCode(@PathVariable Long id) {
        return orderService.sendDoorCode(id);
    }

    @PostMapping("/{id}/add-fee")
    public com.apartment.entity.OrderFee addFee(@PathVariable Long id, @RequestBody com.apartment.entity.OrderFee fee) {
        return orderService.addExtraFee(id, fee.getFeeType(), fee.getAmount(), fee.getRemarks());
    }

    @GetMapping("/{id}/fees")
    public List<com.apartment.entity.OrderFee> getFees(@PathVariable Long id) {
        return orderService.getOrderFees(id);
    }

    @PostMapping("/{id}/cancel")
    public RoomOrder cancel(@PathVariable Long id) {
        return orderService.cancelOrder(id);
    }

    @PostMapping("/{id}/change-room")
    public RoomOrder changeRoom(@PathVariable Long id, @RequestParam Long roomId) {
        return orderService.changeRoom(id, roomId);
    }

    @DeleteMapping("/{id}")
    public void deleteOrder(@PathVariable Long id) {
        orderRepository.deleteById(id);
    }
}
