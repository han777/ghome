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

    @Autowired
    private com.apartment.repository.RoomMaintenanceRepository maintenanceRepository;

    public void validateOrder(RoomOrder order) {
        if (order.getRoom() == null || order.getRoom().getId() == null) {
            throw new RuntimeException("房间不能为空");
        }
        if (order.getUser() == null || order.getUser().getId() == null) {
            throw new RuntimeException("客人不能为空");
        }
        if (order.getStartDate() == null || order.getEndDate() == null) {
            throw new RuntimeException("入住和离开日期不能为空");
        }

        java.time.LocalTime checkInTime = order.getCheckInTime() != null ? java.time.LocalTime.parse(order.getCheckInTime()) : java.time.LocalTime.of(14, 0);
        java.time.LocalTime checkOutTime = order.getCheckOutTime() != null ? java.time.LocalTime.parse(order.getCheckOutTime()) : java.time.LocalTime.of(12, 0);
        
        LocalDateTime startDT = LocalDateTime.of(order.getStartDate(), checkInTime);
        LocalDateTime endDT = LocalDateTime.of(order.getEndDate(), checkOutTime);
        
        if (!startDT.isBefore(endDT)) {
            throw new RuntimeException("离开时间必须晚于入住时间");
        }

        java.util.List<Integer> activeStatuses = java.util.Arrays.asList(1, 2);

        // 1. Room Overlap
        List<RoomOrder> roomOrders = orderRepository.findByRoomIdAndStatusIn(order.getRoom().getId(), activeStatuses);
        for (RoomOrder existing : roomOrders) {
            if (order.getId() != null && order.getId().equals(existing.getId())) continue;
            
            java.time.LocalTime exCheckIn = existing.getCheckInTime() != null ? java.time.LocalTime.parse(existing.getCheckInTime()) : java.time.LocalTime.of(14, 0);
            java.time.LocalTime exCheckOut = existing.getCheckOutTime() != null ? java.time.LocalTime.parse(existing.getCheckOutTime()) : java.time.LocalTime.of(12, 0);
            LocalDateTime exStartDT = LocalDateTime.of(existing.getStartDate(), exCheckIn);
            LocalDateTime exEndDT = LocalDateTime.of(existing.getEndDate(), exCheckOut);
            
            if (startDT.isBefore(exEndDT) && endDT.isAfter(exStartDT)) {
                throw new RuntimeException("房间的入住到离开时间与已有的未结订单的入住时间到离开时间冲突！");
            }
        }

        // 2. Room Maintenance Overlap
        long maintenanceCount = maintenanceRepository.countOverlappingMaintenances(order.getRoom().getId(), startDT, endDT);
        if (maintenanceCount > 0) {
            throw new RuntimeException("房间的入住到离开时间与维修的开始时间到结束时间冲突！");
        }

        // 3. Guest Overlap
        List<RoomOrder> guestOrders = orderRepository.findByUserIdAndStatusIn(order.getUser().getId(), activeStatuses);
        for (RoomOrder existing : guestOrders) {
            if (order.getId() != null && order.getId().equals(existing.getId())) continue;
            
            java.time.LocalTime exCheckIn = existing.getCheckInTime() != null ? java.time.LocalTime.parse(existing.getCheckInTime()) : java.time.LocalTime.of(14, 0);
            java.time.LocalTime exCheckOut = existing.getCheckOutTime() != null ? java.time.LocalTime.parse(existing.getCheckOutTime()) : java.time.LocalTime.of(12, 0);
            LocalDateTime exStartDT = LocalDateTime.of(existing.getStartDate(), exCheckIn);
            LocalDateTime exEndDT = LocalDateTime.of(existing.getEndDate(), exCheckOut);
            
            if (startDT.isBefore(exEndDT) && endDT.isAfter(exStartDT)) {
                throw new RuntimeException("客人的入住到离开时间与已有的未结订单的入住时间到离开时间冲突！");
            }
        }
    }

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
