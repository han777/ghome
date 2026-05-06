package com.apartment.service;

import com.apartment.entity.OrderFee;
import com.apartment.entity.Room;
import com.apartment.entity.RoomOccupy;
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

@Service
public class RoomOrderService {

    @Autowired
    private RoomOrderRepository orderRepository;

    @Autowired
    private OrderFeeRepository feeRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private com.apartment.repository.RoomOccupyRepository occupyRepository;

    @Autowired
    private com.apartment.repository.RoomMaintenanceRepository maintenanceRepository;

    public void validateOrder(RoomOrder order) {
        if (order.getRoomOccupies() == null || order.getRoomOccupies().isEmpty()) {
            throw new RuntimeException("房间不能为空");
        }
        if (order.getOrderNo() == null || order.getOrderNo().isEmpty()) {
            order.setOrderNo(generateOrderNo());
        }
        if (order.getStartDate() == null || order.getEndDate() == null) {
            throw new RuntimeException("入住和离开日期不能为空");
        }

        // Use global stay dates for basic validation
        LocalDateTime startDT = LocalDateTime.of(order.getStartDate(), java.time.LocalTime.of(14, 0));
        LocalDateTime endDT = LocalDateTime.of(order.getEndDate(), java.time.LocalTime.of(12, 0));
        
        if (!startDT.isBefore(endDT)) {
            throw new RuntimeException("离开时间必须晚于入住时间");
        }

        java.util.List<Integer> activeStatuses = java.util.Arrays.asList(1, 2);

        for (com.apartment.entity.RoomOccupy occupy : order.getRoomOccupies()) {
            if (occupy.getRoom() == null || occupy.getRoom().getId() == null) {
                throw new RuntimeException("房号不能为空");
            }
            
            // 1. Room Overlap check for each room
            List<RoomOrder> roomOrders = orderRepository.findByRoomIdAndStatusIn(occupy.getRoom().getId(), activeStatuses);
            for (RoomOrder existing : roomOrders) {
                if (order.getId() != null && order.getId().equals(existing.getId())) continue;
                
                LocalDateTime exStartDT = LocalDateTime.of(existing.getStartDate(), java.time.LocalTime.of(14, 0));
                LocalDateTime exEndDT = LocalDateTime.of(existing.getEndDate(), java.time.LocalTime.of(12, 0));
                
                if (startDT.isBefore(exEndDT) && endDT.isAfter(exStartDT)) {
                    throw new RuntimeException("房间 " + occupy.getRoom().getRoomNo() + " 的入住到离开时间与已有的未结订单冲突！");
                }
            }

            // 2. Room Maintenance Overlap
            long maintenanceCount = maintenanceRepository.countOverlappingMaintenances(occupy.getRoom().getId(), startDT, endDT);
            if (maintenanceCount > 0) {
                throw new RuntimeException("房间 " + occupy.getRoom().getRoomNo() + " 处于维修状态！");
            }
        }

    }

    @Transactional
    public RoomOrder sendDoorCode(Long orderId) {
        if (orderId == null) throw new IllegalArgumentException("Order ID cannot be null");
        RoomOrder order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        // Generate a 6-digit code and set to all rooms
        if (order.getRoomOccupies() != null) {
            for (RoomOccupy ro : order.getRoomOccupies()) {
                String code = String.format("%06d", new java.util.Random().nextInt(1000000));
                ro.setDoorCode(code);
            }
        }
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
        if (order.getRoomOccupies() != null) {
            for (com.apartment.entity.RoomOccupy occupy : order.getRoomOccupies()) {
                com.apartment.entity.Room room = occupy.getRoom();
                if (room != null) {
                    room.setStatus(0); // Set room back to free
                    roomRepository.save(room);
                }
            }
        }
        return orderRepository.save(order);
    }

    @Transactional
    public RoomOrder changeRoom(Long occupyId, Long newRoomId) {
        if (occupyId == null || newRoomId == null) throw new IllegalArgumentException("IDs cannot be null");
        
        RoomOccupy oldOccupy = occupyRepository.findById(occupyId)
            .orElseThrow(() -> new RuntimeException("Occupy record not found"));
        RoomOrder order = oldOccupy.getOrder();
        Room oldRoom = oldOccupy.getRoom();
        Room newRoom = roomRepository.findById(newRoomId)
            .orElseThrow(() -> new RuntimeException("Room not found"));

        LocalDateTime now = LocalDateTime.now();

        // 1. Finish Old Occupation
        oldOccupy.setStatus(1); // Finish
        oldOccupy.setCheckOutTime(now);
        if (oldRoom != null) {
            oldRoom.setStatus(0); // Available
            roomRepository.save(oldRoom);
        }

        // 2. Create New Occupation
        RoomOccupy newOccupy = new RoomOccupy();
        newOccupy.setOrder(order);
        newOccupy.setRoom(newRoom);
        newOccupy.setOccupantUser(oldOccupy.getOccupantUser());
        newOccupy.setOccupantCount(oldOccupy.getOccupantCount());
        newOccupy.setCoOccupants(oldOccupy.getCoOccupants());
        newOccupy.setRoomCardNo(oldOccupy.getRoomCardNo());
        newOccupy.setDoorCode(oldOccupy.getDoorCode());
        newOccupy.setCheckInTime(now);
        newOccupy.setCheckOutTime(null); // Will follow order's end date in display
        newOccupy.setStatus(0); // Current
        
        newRoom.setStatus(1); // Occupied
        roomRepository.save(newRoom);
        occupyRepository.save(newOccupy);
        occupyRepository.save(oldOccupy);

        // 3. Update Order Total Amount (Price Difference for remaining days)
        // Calculate remaining days (at least 1 if today is the check-out day)
        long remainingDays = java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(), order.getEndDate());
        if (remainingDays <= 0) remainingDays = 1;

        BigDecimal diff = BigDecimal.ZERO;
        if (oldRoom != null && oldRoom.getRoomType() != null) {
            BigDecimal oldPrice = order.getBizType() == 1 ? oldRoom.getRoomType().getPriceShortRent() : oldRoom.getRoomType().getPriceLongRent();
            BigDecimal newPrice = order.getBizType() == 1 ? newRoom.getRoomType().getPriceShortRent() : newRoom.getRoomType().getPriceLongRent();
            diff = (newPrice.subtract(oldPrice)).multiply(new BigDecimal(remainingDays));

            order.setRoomFee(order.getRoomFee().add(diff));
            order.setTotalAmount(order.getTotalAmount().add(diff));
        }
        
        String log = String.format("\n[Log %s] 换房: %s -> %s, 差价: %s", 
            now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
            oldRoom != null ? oldRoom.getRoomNo() : "Unknown", 
            newRoom.getRoomNo(), diff.toString());
        order.setRemarks((order.getRemarks() == null ? "" : order.getRemarks()) + log);

        return orderRepository.save(order);
    }

    @Transactional
    public RoomOrder addRoom(Long orderId, Long roomId) {
        if (orderId == null || roomId == null) throw new IllegalArgumentException("IDs cannot be null");
        RoomOrder order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        com.apartment.entity.Room room = roomRepository.findById(roomId).orElseThrow(() -> new RuntimeException("Room not found"));
        
        com.apartment.entity.RoomOccupy occupy = new com.apartment.entity.RoomOccupy();
        occupy.setOrder(order);
        occupy.setRoom(room);
        occupy.setCheckInTime(LocalDateTime.now());
        occupy.setOccupantUser(order.getUser());
        occupy.setStatus(0);
        
        room.setStatus(1);
        roomRepository.save(room);
        occupyRepository.save(occupy);
        
        return order;
    }

    public synchronized String generateOrderNo() {
        LocalDateTime now = LocalDateTime.now();
        String prefix = "RO" + now.format(java.time.format.DateTimeFormatter.ofPattern("yyMM"));
        
        return orderRepository.findTopByOrderNoStartingWithOrderByOrderNoDesc(prefix)
            .map(order -> {
                String lastNo = order.getOrderNo();
                int seq = Integer.parseInt(lastNo.substring(6)) + 1;
                return prefix + String.format("%04d", seq);
            })
            .orElse(prefix + "0001");
    }
}
