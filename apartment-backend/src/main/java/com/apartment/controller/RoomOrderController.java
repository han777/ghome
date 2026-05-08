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

    @Autowired
    private com.apartment.repository.SysUserRepository userRepository;

    @GetMapping("/all")
    public List<RoomOrder> getAllOrders() {
        return orderRepository.findAll();
    }
    
    @GetMapping("/mine")
    public List<RoomOrder> getMyOrders() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .map(u -> orderRepository.findByBookerIdOrderByCreatedAtDesc(u.getId()))
                .orElse(new java.util.ArrayList<>());
    }

    @PostMapping
    public RoomOrder saveOrder(@RequestBody RoomOrder order) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        userRepository.findByUsername(username).ifPresent(u -> {
            // Set last update user
            order.setLastUpdateUser(u);

            // If creating a new order in status 0 (Cooling-off), delete existing ones for this user
            if (order.getId() == null && order.getStatus() != null && order.getStatus() == 0) {
                java.util.List<RoomOrder> existing = orderRepository.findByBookerIdAndStatus(u.getId(), 0);
                if (!existing.isEmpty()) {
                    orderRepository.deleteAll(existing);
                }
            }
            
            if (order.getId() == null) {
                // NEW ORDER
                if (order.getCreateUser() == null) {
                    order.setCreateUser(u);
                }
                if (order.getBooker() == null) {
                    order.setBooker(u);
                }
            } else {
                // UPDATE ORDER
                orderRepository.findById(order.getId()).ifPresent(existing -> {
                    if (order.getCreateUser() == null) {
                        order.setCreateUser(existing.getCreateUser());
                    }
                    if (order.getCreatedAt() == null) {
                        order.setCreatedAt(existing.getCreatedAt());
                    }
                });
            }

            if (order.getBookPhone() == null || order.getBookPhone().isBlank()) {
                order.setBookPhone(u.getPhone());
            }
            if (order.getRoomOccupies() != null) {
                order.getRoomOccupies().forEach(occupy -> {
                    occupy.setOrder(order);
                    if (occupy.getOccupantUser() == null && order.getId() == null) {
                        occupy.setOccupantUser(u);
                    }
                });
            }
        });
        orderService.validateOrder(order);
        if (order.getProductDetails() != null) {
            order.getProductDetails().forEach(detail -> detail.setOrder(order));
        }
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

    @PostMapping("/{id}/checkout")
    public RoomOrder checkout(@PathVariable Long id) {
        return orderService.checkoutOrder(id);
    }

    @PostMapping("/occupy/{occupyId}/change-room")
    public RoomOrder changeRoom(@PathVariable Long occupyId, @RequestParam Long roomId) {
        return orderService.changeRoom(occupyId, roomId);
    }

    @PostMapping("/{id}/add-room")
    public RoomOrder addRoom(@PathVariable Long id, @RequestParam Long roomId) {
        return orderService.addRoom(id, roomId);
    }

    @DeleteMapping("/{id}")
    public void deleteOrder(@PathVariable Long id) {
        orderRepository.deleteById(id);
    }
}
