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

        LocalDateTime startDT = order.getStartDate();
        LocalDateTime endDT = order.getEndDate();
        
        if (!startDT.isBefore(endDT)) {
            throw new RuntimeException("离开时间必须晚于入住时间");
        }

        java.util.List<Integer> activeStatuses = java.util.Arrays.asList(1, 2);
        int currentStatusCount = 0;

        for (com.apartment.entity.RoomOccupy occupy : order.getRoomOccupies()) {
            if (occupy.getRoom() == null || occupy.getRoom().getId() == null) {
                throw new RuntimeException("房号不能为空");
            }
            
            if (occupy.getStatus() != null && occupy.getStatus() == 0) {
                currentStatusCount++;
            }
            
            // 1. Room Overlap check for each room
            List<RoomOrder> roomOrders = orderRepository.findByRoomIdAndStatusIn(occupy.getRoom().getId(), activeStatuses);
            for (RoomOrder existing : roomOrders) {
                if (order.getId() != null && order.getId().equals(existing.getId())) continue;
                
                LocalDateTime exStartDT = existing.getStartDate();
                LocalDateTime exEndDT = existing.getEndDate();
                
                if (startDT.isBefore(exEndDT) && endDT.isAfter(exStartDT)) {
                    throw new RuntimeException("房间 " + occupy.getRoom().getRoomNo() + " 的入住到离开时间与已有的未结订单冲突！");
                }
            }

            // 2. Room Maintenance Overlap
            long maintenanceCount = maintenanceRepository.countOverlappingMaintenances(occupy.getRoom().getId(), startDT, endDT);
            if (maintenanceCount > 0) {
                throw new RuntimeException("房间 " + occupy.getRoom().getRoomNo() + " 处于维修状态！");
            }

            // 3. Occupant User Overlap check (Only if occupant is set)
            if (occupy.getOccupantUser() != null && occupy.getOccupantUser().getId() != null) {
                List<RoomOrder> occupantOrders = orderRepository.findByOccupantUserIdAndStatusIn(occupy.getOccupantUser().getId(), activeStatuses);
                for (RoomOrder existing : occupantOrders) {
                    if (order.getId() != null && order.getId().equals(existing.getId())) continue;

                    LocalDateTime exStartDT = existing.getStartDate();
                    LocalDateTime exEndDT = existing.getEndDate();

                    if (startDT.isBefore(exEndDT) && endDT.isAfter(exStartDT)) {
                        throw new RuntimeException("入住人 " + occupy.getOccupantUser().getRealName() + " 在此期间已有其他房间预约，存在时段冲突！");
                    }
                }
            }
        }

        // 4. Individual guest constraint: only one current occupy allowed
        if (order.getCustomerType() != null && order.getCustomerType() == 1) {
            if (currentStatusCount > 1) {
                throw new RuntimeException("个人客人订单只允许一条记录为当前状态");
            }
        }

        // 5. Calculate Prices if not set
        if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(BigDecimal.ZERO) == 0) {
            calculatePrices(order);
        }
    }

    public void calculatePrices(RoomOrder order) {
        BigDecimal totalRoomFee = BigDecimal.ZERO;

        if (order.getRoomOccupies() != null) {
            for (com.apartment.entity.RoomOccupy occupy : order.getRoomOccupies()) {
                com.apartment.entity.Room room = occupy.getRoom();
                if (room != null && (room.getRoomType() == null)) {
                    room = roomRepository.findById(room.getId()).orElse(room);
                }

                // Initialize default values if not set
                if (occupy.getActualPrice() == null && room != null && room.getRoomType() != null) {
                    BigDecimal price = (order.getBizType() != null && order.getBizType() == 2) ?
                        room.getRoomType().getPriceLongRent() :
                        room.getRoomType().getPriceShortRent();
                    occupy.setActualPrice(price);
                }

                // Calculate days from room-level check-in/check-out times, fall back to order dates
                long days = 0;
                java.time.LocalDate checkInDate = occupy.getCheckInTime() != null ? occupy.getCheckInTime().toLocalDate() : order.getStartDate().toLocalDate();
                java.time.LocalDate checkOutDate = occupy.getCheckOutTime() != null ? occupy.getCheckOutTime().toLocalDate() : order.getEndDate().toLocalDate();
                days = java.time.temporal.ChronoUnit.DAYS.between(checkInDate, checkOutDate);
                if (days <= 0) days = 1;
                occupy.setQuantity((int)days);

                if (occupy.getActualPrice() != null && occupy.getQuantity() != null) {
                    totalRoomFee = totalRoomFee.add(occupy.getActualPrice().multiply(new BigDecimal(occupy.getQuantity())));
                }
            }
        }
        order.setRoomFee(totalRoomFee);
        if (order.getServiceFee() == null) order.setServiceFee(BigDecimal.ZERO);
        order.setTotalAmount(totalRoomFee.add(order.getServiceFee()));
    }

    @Transactional
    public RoomOrder sendRoomCard(Long orderId) {
        if (orderId == null) throw new IllegalArgumentException("Order ID cannot be null");
        RoomOrder order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (order.getRoomOccupies() == null || order.getRoomOccupies().isEmpty()) {
            throw new RuntimeException("该订单没有房间记录");
        }

        for (RoomOccupy ro : order.getRoomOccupies()) {
            // Status 0 is Current
            if (ro.getStatus() != null && ro.getStatus() == 0) {
                if (ro.getRoomCardNo() == null || ro.getRoomCardNo().isBlank()) {
                    throw new RuntimeException("房间 " + ro.getRoom().getRoomNo() + " 的房卡号不能为空");
                }
                if (ro.getDoorCode() == null || ro.getDoorCode().isBlank()) {
                    throw new RuntimeException("房间 " + ro.getRoom().getRoomNo() + " 的门锁密码不能为空");
                }
            }
        }

        order.setStatus(2); // Set to In (已入住)
        // Set actual check-in time if not set
        LocalDateTime now = LocalDateTime.now();
        for (RoomOccupy ro : order.getRoomOccupies()) {
            if (ro.getStatus() != null && ro.getStatus() == 0 && ro.getCheckInTime() == null) {
                ro.setCheckInTime(now);
            }
        }
        
        return orderRepository.save(order);
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
    public RoomOrder checkoutOrder(Long orderId) {
        if (orderId == null) throw new IllegalArgumentException("Order ID cannot be null");
        RoomOrder order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(3); // Out
        LocalDateTime now = LocalDateTime.now();
        order.setEndDate(now);
        if (order.getRoomOccupies() != null) {
            for (RoomOccupy occupy : order.getRoomOccupies()) {
                occupy.setStatus(1); // Finish
                occupy.setCheckOutTime(now);
                Room room = occupy.getRoom();
                if (room != null) {
                    room.setStatus(0); // Available
                    roomRepository.save(room);
                }
                occupyRepository.save(occupy);
            }
        }
        return orderRepository.save(order);
    }

    @Transactional
    public RoomOrder changeRoom(Long occupyId, Long newRoomId, LocalDateTime switchDate) {
        if (occupyId == null || newRoomId == null) throw new IllegalArgumentException("IDs cannot be null");
        
        if (switchDate == null) switchDate = LocalDateTime.now();
        
        RoomOccupy oldOccupy = occupyRepository.findById(occupyId)
            .orElseThrow(() -> new RuntimeException("Occupy record not found"));
        RoomOrder order = oldOccupy.getOrder();
        Room oldRoom = oldOccupy.getRoom();
        Room newRoom = roomRepository.findById(newRoomId)
            .orElseThrow(() -> new RuntimeException("Room not found"));

        // 1. Finish Old Occupation
        oldOccupy.setStatus(1); // Finish
        oldOccupy.setCheckOutTime(switchDate);
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
        newOccupy.setCheckInTime(switchDate);
        newOccupy.setCheckOutTime(null); // Will follow order's end date in display
        newOccupy.setStatus(0); // Current
        
        newRoom.setStatus(1); // Occupied
        roomRepository.save(newRoom);
        // 3. Update Occupations' price and quantity
        long daysStayed = java.time.temporal.ChronoUnit.DAYS.between(order.getStartDate().toLocalDate(), switchDate.toLocalDate());
        if (daysStayed < 0) daysStayed = 0;
        long remainingDays = java.time.temporal.ChronoUnit.DAYS.between(switchDate.toLocalDate(), order.getEndDate().toLocalDate());
        if (remainingDays < 0) remainingDays = 0;

        // Old occupation gets the actual price and the number of days stayed
        if (oldOccupy.getActualPrice() == null && oldRoom != null && oldRoom.getRoomType() != null) {
            BigDecimal oldPrice = order.getBizType() == 1 ? oldRoom.getRoomType().getPriceShortRent() : oldRoom.getRoomType().getPriceLongRent();
            oldOccupy.setActualPrice(oldPrice);
        }
        oldOccupy.setQuantity((int)daysStayed);

        // New occupation gets the actual price and the number of days remaining
        BigDecimal newPrice = BigDecimal.ZERO;
        if (newRoom.getRoomType() != null) {
            newPrice = order.getBizType() == 1 ? newRoom.getRoomType().getPriceShortRent() : newRoom.getRoomType().getPriceLongRent();
        }
        newOccupy.setActualPrice(newPrice);
        newOccupy.setQuantity((int)remainingDays);
        
        occupyRepository.save(newOccupy);
        occupyRepository.save(oldOccupy);

        // 4. Record Log and Recalculate
        String log = String.format("\n[Log %s] 换房: %s -> %s, 生效日期: %s, 剩余天数: %d", 
            LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
            oldRoom != null ? oldRoom.getRoomNo() : "Unknown", 
            newRoom.getRoomNo(), 
            switchDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            remainingDays);
        order.setRemarks((order.getRemarks() == null ? "" : order.getRemarks()) + log);

        calculatePrices(order);
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
        occupy.setOccupantUser(order.getBooker());
        occupy.setStatus(0);
        
        room.setStatus(1);
        roomRepository.save(room);
        occupyRepository.save(occupy);
        
        return order;
    }

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public synchronized String generateOrderNo() {
        LocalDateTime now = LocalDateTime.now();
        String prefix = "RO" + now.format(java.time.format.DateTimeFormatter.ofPattern("yyMM"));
        
        try {
            Long seq = orderRepository.getNextOrderSeq();
            return prefix + String.format("%04d", seq % 10000);
        } catch (Exception e) {
            // If sequence doesn't exist, create it and retry once
            try {
                jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS room_order_no_seq START WITH 1");
                Long seq = orderRepository.getNextOrderSeq();
                return prefix + String.format("%04d", seq % 10000);
            } catch (Exception e2) {
                throw new RuntimeException("Failed to generate order number using sequence", e2);
            }
        }
    }

    @Transactional
    public RoomOrder adjustOccupyTime(Long occupyId, LocalDateTime newStart, LocalDateTime newEnd) {
        RoomOccupy occupy = occupyRepository.findById(occupyId)
            .orElseThrow(() -> new RuntimeException("入住记录未找到"));
        RoomOrder order = occupy.getOrder();
        
        if (newStart.isAfter(newEnd) || newStart.isEqual(newEnd)) {
            throw new RuntimeException("结束时间必须晚于开始时间");
        }

        // Validate conflicts
        java.util.List<Integer> activeStatuses = java.util.Arrays.asList(1, 2);
        
        // 1. Room Conflict (Both Individual and Group)
        List<RoomOrder> roomOrders = orderRepository.findByRoomIdAndStatusIn(occupy.getRoom().getId(), activeStatuses);
        for (RoomOrder existing : roomOrders) {
            if (order.getId().equals(existing.getId())) {
                continue;
            }
            if (newStart.isBefore(existing.getEndDate()) && newEnd.isAfter(existing.getStartDate())) {
                throw new RuntimeException("该时段与房间已有的订单冲突");
            }
        }

        // 2. Room Maintenance Conflict
        long maintenanceCount = maintenanceRepository.countOverlappingMaintenances(occupy.getRoom().getId(), newStart, newEnd);
        if (maintenanceCount > 0) {
            throw new RuntimeException("该时段房间处于维修状态");
        }

        // 3. Occupant User Conflict (Only for Individual Booking)
        if (order.getCustomerType() != null && order.getCustomerType() == 1) { // 1: Individual
            if (occupy.getOccupantUser() != null) {
                List<RoomOrder> occupantOrders = orderRepository.findByOccupantUserIdAndStatusIn(occupy.getOccupantUser().getId(), activeStatuses);
                for (RoomOrder existing : occupantOrders) {
                    if (order.getId().equals(existing.getId())) continue;
                    if (newStart.isBefore(existing.getEndDate()) && newEnd.isAfter(existing.getStartDate())) {
                        throw new RuntimeException("该入住人在所选时段已有其他预约，存在冲突");
                    }
                }
            }
        }

        // Update times
        occupy.setCheckInTime(newStart);
        occupy.setCheckOutTime(newEnd);
        
        // Sync order level times if needed
        if (newStart.isBefore(order.getStartDate())) {
            order.setStartDate(newStart);
        }
        if (newEnd.isAfter(order.getEndDate())) {
            order.setEndDate(newEnd);
        }
        
        occupyRepository.save(occupy);
        calculatePrices(order);
        return orderRepository.save(order);
    }
}
