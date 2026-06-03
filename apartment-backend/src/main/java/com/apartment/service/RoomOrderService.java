package com.apartment.service;

import com.apartment.entity.OrderFee;
import com.apartment.entity.Room;
import com.apartment.entity.RoomOccupy;
import com.apartment.entity.RoomOrder;
import com.apartment.entity.NotificationRecord;
import com.apartment.entity.SysUser;
import com.apartment.exception.BusinessException;
import com.apartment.exception.ErrorCode;
import com.apartment.repository.OrderFeeRepository;
import com.apartment.repository.RoomOrderRepository;
import com.apartment.repository.RoomRepository;
import com.apartment.repository.NotificationRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

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

    @Autowired
    private NotificationRecordRepository notificationRecordRepository;

    @Autowired
    private com.apartment.repository.CleaningTaskRepository cleaningTaskRepository;

    @Autowired
    private MessageTemplateService messageTemplateService;

    @Autowired
    private SysConfigService sysConfigService;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public void validateOrder(RoomOrder order) {
        if (order.getRoomOccupies() == null || order.getRoomOccupies().isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_ROOM_EMPTY);
        }
        if (order.getOrderNo() == null || order.getOrderNo().isEmpty()) {
            order.setOrderNo(generateOrderNo());
        }
        if (order.getStartDate() == null || order.getEndDate() == null) {
            throw new BusinessException(ErrorCode.ORDER_DATES_EMPTY);
        }

        LocalDateTime startDT = order.getStartDate();
        LocalDateTime endDT = order.getEndDate();

        if (!startDT.isBefore(endDT)) {
            throw new BusinessException(ErrorCode.ORDER_END_BEFORE_START);
        }

        // Group order validation & auto-fill
        boolean isGroup = order.getCustomerType() != null && order.getCustomerType() == 2;
        if (isGroup) {
            if (isBlank(order.getGroupName())) {
                throw new BusinessException(ErrorCode.ORDER_GROUP_NAME_EMPTY);
            }
            if (isBlank(order.getContactName())) {
                throw new BusinessException(ErrorCode.ORDER_CONTACT_NAME_EMPTY);
            }
            if (isBlank(order.getContactPhone())) {
                throw new BusinessException(ErrorCode.ORDER_CONTACT_PHONE_EMPTY);
            }
            if (isBlank(order.getCompany())) {
                throw new BusinessException(ErrorCode.ORDER_COMPANY_EMPTY);
            }
            if (isBlank(order.getCostCenter())) {
                throw new BusinessException(ErrorCode.ORDER_COST_CENTER_EMPTY);
            }
            if (isBlank(order.getActivityCode())) {
                throw new BusinessException(ErrorCode.ORDER_ACTIVITY_CODE_EMPTY);
            }
            // Auto-fill occupantName for group orders
            for (com.apartment.entity.RoomOccupy occupy : order.getRoomOccupies()) {
                occupy.setOccupantUser(null);
                occupy.setOccupantName(order.getGroupName());
            }
        }

        java.util.List<Integer> activeStatuses = java.util.Arrays.asList(1, 2);
        int currentStatusCount = 0;

        for (com.apartment.entity.RoomOccupy occupy : order.getRoomOccupies()) {
            if (occupy.getRoom() == null || occupy.getRoom().getId() == null) {
                throw new BusinessException(ErrorCode.ORDER_ROOM_NO_EMPTY);
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
                    throw new BusinessException(ErrorCode.ORDER_ROOM_TIME_CONFLICT, occupy.getRoom().getRoomNo());
                }
            }

            // 2. Room Maintenance Overlap
            long maintenanceCount = maintenanceRepository.countOverlappingMaintenances(occupy.getRoom().getId(), startDT, endDT);
            if (maintenanceCount > 0) {
                throw new BusinessException(ErrorCode.ORDER_ROOM_MAINTENANCE, occupy.getRoom().getRoomNo());
            }

            // 3. Occupant User Overlap check (Only if occupant is set)
            if (occupy.getOccupantUser() != null && occupy.getOccupantUser().getId() != null) {
                List<RoomOrder> occupantOrders = orderRepository.findByOccupantUserIdAndStatusIn(occupy.getOccupantUser().getId(), activeStatuses);
                for (RoomOrder existing : occupantOrders) {
                    if (order.getId() != null && order.getId().equals(existing.getId())) continue;

                    LocalDateTime exStartDT = existing.getStartDate();
                    LocalDateTime exEndDT = existing.getEndDate();

                    if (startDT.isBefore(exEndDT) && endDT.isAfter(exStartDT)) {
                        throw new BusinessException(ErrorCode.ORDER_OCCUPANT_CONFLICT, occupy.getOccupantUser().getRealName());
                    }
                }
            }
        }

        // 4. Individual guest constraint: only one current occupy allowed
        if (!isGroup) {
            if (currentStatusCount > 1) {
                throw new BusinessException(ErrorCode.ORDER_INDIVIDUAL_ONE_CURRENT);
            }
        }

        // 5. Calculate Prices if not set
        if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(BigDecimal.ZERO) == 0) {
            calculatePrices(order);
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
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
    public RoomOrder sendRoomCard(Long orderId, String keyBoxNo, String boxPassword) {
        if (orderId == null) throw new IllegalArgumentException("Order ID cannot be null");
        RoomOrder order = orderRepository.findById(orderId).orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getRoomOccupies() == null || order.getRoomOccupies().isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_NO_ROOM_RECORDS);
        }

        if (keyBoxNo == null || keyBoxNo.isBlank()) {
            throw new BusinessException(ErrorCode.ORDER_KEY_BOX_NO_EMPTY);
        }
        if (boxPassword == null || boxPassword.isBlank()) {
            throw new BusinessException(ErrorCode.ORDER_BOX_PASSWORD_EMPTY);
        }

        // Check if any room has an uncompleted cleaning task (脏房拦截)
        for (RoomOccupy ro : order.getRoomOccupies()) {
            if (ro.getRoom() != null && ro.getRoom().getId() != null) {
                List<com.apartment.entity.CleaningTask> pendingTasks = cleaningTaskRepository
                    .findByRoomIdAndStatus(ro.getRoom().getId(), 0);
                if (!pendingTasks.isEmpty()) {
                    throw new BusinessException(ErrorCode.ORDER_ROOM_DIRTY);
                }
            }
        }

        // Write key box info to order
        order.setKeyBoxNo(keyBoxNo);
        order.setBoxPassword(boxPassword);

        order.setStatus(2); // Set to In (已入住)
        // Set actual check-in time if not set
        LocalDateTime now = LocalDateTime.now();
        for (RoomOccupy ro : order.getRoomOccupies()) {
            if (ro.getStatus() != null && ro.getStatus() == 0 && ro.getCheckInTime() == null) {
                ro.setCheckInTime(now);
            }
        }

        // Check if room card notification already sent for this order (one per order)
        if (notificationRecordRepository.existsByOrderIdAndKeyBoxNoIsNotNull(order.getId())) {
            return orderRepository.save(order);
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        // Collect all room numbers (comma-separated)
        StringBuilder roomNosBuilder = new StringBuilder();
        LocalDateTime earliestCheckIn = null;
        if (order.getRoomOccupies() != null) {
            for (RoomOccupy ro : order.getRoomOccupies()) {
                if (ro.getStatus() != null && ro.getStatus() == 0 && ro.getRoom() != null) {
                    if (roomNosBuilder.length() > 0) roomNosBuilder.append(", ");
                    roomNosBuilder.append(ro.getRoom().getRoomNo());
                    LocalDateTime ciTime = ro.getCheckInTime() != null ? ro.getCheckInTime() : order.getStartDate();
                    if (earliestCheckIn == null || ciTime.isBefore(earliestCheckIn)) {
                        earliestCheckIn = ciTime;
                    }
                }
            }
        }

        // Determine recipient (booker)
        SysUser recipient = order.getBooker();
        if (recipient != null) {
            String channel = "1".equals(recipient.getSource()) ? "wecom" : "email";
            String roomNosVal = roomNosBuilder.toString();
            LocalDateTime ciTime = earliestCheckIn != null ? earliestCheckIn : order.getStartDate();
            LocalDateTime coTime = order.getEndDate();
            String recipientNameVal = recipient.getRealName() != null ? recipient.getRealName() : recipient.getUsername();
            String userLocale = recipient.getLocale() != null ? recipient.getLocale() : "zh";

            String content = messageTemplateService.buildCheckInNotification(
                userLocale,
                order.getOrderNo(),
                roomNosVal,
                keyBoxNo,
                boxPassword,
                ciTime != null ? ciTime.format(fmt) : "",
                coTime != null ? coTime.format(fmt) : ""
            );

            NotificationRecord nr = new NotificationRecord();
            nr.setOrder(order);
            nr.setRecipientUser(recipient);
            nr.setChannel(channel);
            nr.setOrderNo(order.getOrderNo());
            nr.setRoomNo(roomNosVal);
            nr.setKeyBoxNo(keyBoxNo);
            nr.setBoxPassword(boxPassword);
            nr.setLocale(userLocale);
            nr.setCheckInTime(ciTime);
            nr.setCheckOutTime(coTime);
            nr.setRecipientName(recipientNameVal);
            nr.setContent(content);

            notificationRecordRepository.save(nr);
        }

        return orderRepository.save(order);
    }

    @Transactional
    public void sendOrderNotification(Long orderId) {
        if (orderId == null) return;

        // Check if notification already sent for this order (one notification per order)
        if (notificationRecordRepository.existsByOrderId(orderId)) {
            return;
        }

        // Flush and clear session cache to ensure we read fresh data from DB.
        // When called right after save() in the same HTTP request (open-in-view enabled),
        // findById would return the cached entity whose associations (room, occupantUser)
        // may only contain the id from the frontend JSON payload, not the full DB record.
        // Flushing ensures pending writes are committed; clearing forces subsequent
        // findById to load a fresh entity graph from the database.
        entityManager.flush();
        entityManager.clear();

        RoomOrder order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return;

        java.util.List<String> notificationEmails = sysConfigService.getNotificationEmails();
        if (notificationEmails.isEmpty()) return;

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        // Build room info list
        StringBuilder roomListBuilder = new StringBuilder();
        if (order.getRoomOccupies() != null) {
            for (RoomOccupy ro : order.getRoomOccupies()) {
                if (roomListBuilder.length() > 0) roomListBuilder.append("\n");
                String roomNo = ro.getRoom() != null ? ro.getRoom().getRoomNo() : "";
                String occupant = ro.getOccupantName();
                if (occupant == null && ro.getOccupantUser() != null) {
                    occupant = ro.getOccupantUser().getRealName() != null ? ro.getOccupantUser().getRealName() : ro.getOccupantUser().getUsername();
                }
                roomListBuilder.append("  - ").append(roomNo);
                if (occupant != null && !occupant.isBlank()) {
                    roomListBuilder.append(" (").append(occupant).append(")");
                }
            }
        }

        String bookerName = "-";
        if (order.getBooker() != null) {
            bookerName = order.getBooker().getRealName() != null ? order.getBooker().getRealName() : order.getBooker().getUsername();
        }
        String bookerPhone = order.getBookPhone() != null ? order.getBookPhone() : "-";

        LocalDateTime ciTime = order.getStartDate();
        LocalDateTime coTime = order.getEndDate();

        String emailContent = "新订单通知\n"
            + "\n订单号: " + order.getOrderNo()
            + "\n订房人: " + bookerName
            + "\n联系电话: " + bookerPhone
            + "\n入住时间: " + (ciTime != null ? ciTime.format(fmt) : "")
            + "\n离店时间: " + (coTime != null ? coTime.format(fmt) : "")
            + "\n房间信息:\n" + roomListBuilder.toString()
            + "\n\n订单金额: " + (order.getTotalAmount() != null ? order.getTotalAmount().toPlainString() : "0")
            + "\n备注: " + (order.getRemarks() != null ? order.getRemarks() : "-");

        String roomNoSummary = "";
        if (order.getRoomOccupies() != null && !order.getRoomOccupies().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (RoomOccupy ro : order.getRoomOccupies()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(ro.getRoom() != null ? ro.getRoom().getRoomNo() : "");
            }
            roomNoSummary = sb.toString();
        }

        for (String email : notificationEmails) {
            NotificationRecord nr = new NotificationRecord();
            nr.setOrder(order);
            nr.setChannel("email");
            nr.setRecipientEmail(email);
            nr.setOrderNo(order.getOrderNo());
            nr.setRoomNo(roomNoSummary);
            nr.setLocale("zh");
            nr.setCheckInTime(ciTime);
            nr.setCheckOutTime(coTime);
            nr.setRecipientName(email);
            nr.setContent(emailContent);
            notificationRecordRepository.save(nr);
        }
    }

    @Transactional
    public RoomOrder cancelOrder(Long orderId) {
        if (orderId == null) throw new IllegalArgumentException("Order ID cannot be null");
        RoomOrder order = orderRepository.findById(orderId).orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // Cancel deadline: must be no later than 24:00 of the day before check-in
        LocalDateTime cancelDeadline = order.getStartDate().toLocalDate().atStartOfDay();
        if (LocalDateTime.now().isAfter(cancelDeadline) || LocalDateTime.now().isEqual(cancelDeadline)) {
            throw new BusinessException(ErrorCode.ORDER_CANCEL_DEADLINE_PASSED);
        }

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

    // Admin cancel: no time window restriction
    @Transactional
    public RoomOrder adminCancelOrder(Long orderId) {
        if (orderId == null) throw new IllegalArgumentException("Order ID cannot be null");
        RoomOrder order = orderRepository.findById(orderId).orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

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
        RoomOrder order = orderRepository.findById(orderId).orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
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
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_OCCUPY_NOT_FOUND));
        RoomOrder order = oldOccupy.getOrder();
        Room oldRoom = oldOccupy.getRoom();
        Room newRoom = roomRepository.findById(newRoomId)
            .orElseThrow(() -> new BusinessException(ErrorCode.GENERAL_ROOM_NOT_FOUND));

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
        newOccupy.setOccupantName(oldOccupy.getOccupantName());
        newOccupy.setOccupantCount(oldOccupy.getOccupantCount());
        newOccupy.setCoOccupants(oldOccupy.getCoOccupants());
        newOccupy.setCheckInTime(switchDate);
        newOccupy.setCheckOutTime(order.getEndDate());
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

        // Recalculate order date range from all active occupies
        recalcOrderDateRange(order);

        return orderRepository.save(order);
    }

    @Transactional
    public RoomOrder addRoom(Long orderId, Long roomId) {
        if (orderId == null || roomId == null) throw new IllegalArgumentException("IDs cannot be null");
        RoomOrder order = orderRepository.findById(orderId).orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        com.apartment.entity.Room room = roomRepository.findById(roomId).orElseThrow(() -> new BusinessException(ErrorCode.GENERAL_ROOM_NOT_FOUND));

        com.apartment.entity.RoomOccupy occupy = new com.apartment.entity.RoomOccupy();
        occupy.setOrder(order);
        occupy.setRoom(room);
        occupy.setCheckInTime(LocalDateTime.now());
        boolean isGroup = order.getCustomerType() != null && order.getCustomerType() == 2;
        if (isGroup) {
            occupy.setOccupantName(order.getGroupName());
        } else {
            occupy.setOccupantUser(order.getBooker());
        }
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
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_OCCUPY_NOT_FOUND));
        RoomOrder order = occupy.getOrder();

        if (newStart.isAfter(newEnd) || newStart.isEqual(newEnd)) {
            throw new BusinessException(ErrorCode.ORDER_END_BEFORE_START);
        }

        // Validate conflicts
        java.util.List<Integer> activeStatuses = java.util.Arrays.asList(1, 2);

        // 1. Room Conflict
        List<RoomOrder> roomOrders = orderRepository.findByRoomIdAndStatusIn(occupy.getRoom().getId(), activeStatuses);
        for (RoomOrder existing : roomOrders) {
            if (order.getId().equals(existing.getId())) {
                continue;
            }
            if (newStart.isBefore(existing.getEndDate()) && newEnd.isAfter(existing.getStartDate())) {
                throw new BusinessException(ErrorCode.ORDER_TIME_CONFLICT);
            }
        }

        // 2. Room Maintenance Conflict
        long maintenanceCount = maintenanceRepository.countOverlappingMaintenances(occupy.getRoom().getId(), newStart, newEnd);
        if (maintenanceCount > 0) {
            throw new BusinessException(ErrorCode.ORDER_MAINTENANCE_CONFLICT);
        }

        // 3. Occupant User Conflict
        if (order.getCustomerType() != null && order.getCustomerType() == 1) {
            if (occupy.getOccupantUser() != null) {
                List<RoomOrder> occupantOrders = orderRepository.findByOccupantUserIdAndStatusIn(occupy.getOccupantUser().getId(), activeStatuses);
                for (RoomOrder existing : occupantOrders) {
                    if (order.getId().equals(existing.getId())) continue;
                    if (newStart.isBefore(existing.getEndDate()) && newEnd.isAfter(existing.getStartDate())) {
                        throw new BusinessException(ErrorCode.ORDER_OCCUPANT_TIME_CONFLICT);
                    }
                }
            }
        }

        // Update times
        occupy.setCheckInTime(newStart);
        occupy.setCheckOutTime(newEnd);

        occupyRepository.save(occupy);
        calculatePrices(order);

        // Recalculate order date range from all active occupies
        recalcOrderDateRange(order);

        return orderRepository.save(order);
    }

    /**
     * Recalculate order-level start/end dates to cover all active (status=0) occupy date ranges.
     */
    private void recalcOrderDateRange(RoomOrder order) {
        List<RoomOccupy> allOccupies = occupyRepository.findByOrderId(order.getId());
        LocalDateTime minStart = null;
        LocalDateTime maxEnd = null;
        for (RoomOccupy o : allOccupies) {
            if (o.getStatus() != null && o.getStatus() == 0) {
                LocalDateTime ci = o.getCheckInTime();
                LocalDateTime co = o.getCheckOutTime();
                if (ci != null) {
                    if (minStart == null || ci.isBefore(minStart)) minStart = ci;
                }
                if (co != null) {
                    if (maxEnd == null || co.isAfter(maxEnd)) maxEnd = co;
                }
            }
        }
        // For occupies with null checkOutTime, use order's endDate as fallback
        for (RoomOccupy o : allOccupies) {
            if (o.getStatus() != null && o.getStatus() == 0 && o.getCheckOutTime() == null) {
                if (maxEnd == null || order.getEndDate().isAfter(maxEnd)) {
                    maxEnd = order.getEndDate();
                }
            }
        }
        if (minStart != null) order.setStartDate(minStart);
        if (maxEnd != null) order.setEndDate(maxEnd);
    }

    @Transactional
    public RoomOrder sendDoorCode(Long orderId) {
        if (orderId == null) throw new IllegalArgumentException("Order ID cannot be null");
        RoomOrder order = orderRepository.findById(orderId).orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        // Generate a 6-digit code and set to all rooms
        if (order.getRoomOccupies() != null) {
            for (RoomOccupy ro : order.getRoomOccupies()) {
                String code = String.format("%06d", new java.util.Random().nextInt(1000000));
                // Note: roomCardNo/doorCode removed from RoomOccupy; this method is no longer used
                // Keeping the method signature for API compatibility
            }
        }
        return orderRepository.save(order);
    }

    @Transactional
    public OrderFee addExtraFee(Long orderId, String feeType, BigDecimal amount, String remarks) {
        if (orderId == null) throw new IllegalArgumentException("Order ID cannot be null");
        RoomOrder order = orderRepository.findById(orderId).orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
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
}
