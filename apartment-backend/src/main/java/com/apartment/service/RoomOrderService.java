package com.apartment.service;

import com.apartment.entity.OrderFee;
import com.apartment.entity.RoomOrder;
import com.apartment.repository.OrderFeeRepository;
import com.apartment.repository.RoomOrderRepository;
import com.apartment.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class RoomOrderService {

    @Autowired
    private RoomOrderRepository orderRepository;

    @Autowired
    private OrderFeeRepository feeRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Transactional
    public RoomOrder sendDoorCode(Long orderId) {
        if (orderId == null) throw new IllegalArgumentException("Order ID cannot be null");
        RoomOrder order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        // Generate a 6-digit code
        String code = String.format("%06d", new Random().nextInt(1000000));
        order.setDoorCode(code);
        // In real world, send SMS/Email here
        return orderRepository.save(order);
    }

    @Transactional
    public OrderFee addExtraFee(Long orderId, String feeType, BigDecimal amount, String remarks) {
        if (orderId == null) throw new IllegalArgumentException("Order ID cannot be null");
        RoomOrder order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        OrderFee fee = new OrderFee();
        fee.setOrder(order);
        fee.setFeeType(feeType);
        fee.setAmount(amount);
        fee.setRemarks(remarks);
        fee.setCreatedAt(LocalDateTime.now());
        
        // Update total amount in order
        if (order.getTotalAmount() == null) order.setTotalAmount(BigDecimal.ZERO);
        order.setTotalAmount(order.getTotalAmount().add(amount));
        orderRepository.save(order);
        
        return feeRepository.save(fee);
    }

    public List<OrderFee> getOrderFees(Long orderId) {
        return feeRepository.findByOrderId(orderId);
    }

    @Transactional
    public RoomOrder cancelOrder(Long orderId) {
        if (orderId == null) throw new IllegalArgumentException("Order ID cannot be null");
        RoomOrder order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(4); // Canceled
        com.apartment.entity.Room room = order.getRoom();
        if (room != null) {
            room.setStatus(0); // Set room back to free
            roomRepository.save(room);
        }
        return orderRepository.save(order);
    }

    @Transactional
    public RoomOrder changeRoom(Long orderId, Long newRoomId) {
        if (orderId == null || newRoomId == null) throw new IllegalArgumentException("IDs cannot be null");
        RoomOrder order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        com.apartment.entity.Room oldRoom = order.getRoom();
        if (oldRoom != null) {
            oldRoom.setStatus(0);
            roomRepository.save(oldRoom);
        }
        com.apartment.entity.Room newRoom = roomRepository.findById(newRoomId).orElseThrow(() -> new RuntimeException("Room not found"));
        order.setRoom(newRoom);
        newRoom.setStatus(1); // Occupied
        roomRepository.save(newRoom);
        return orderRepository.save(order);
    }
}
