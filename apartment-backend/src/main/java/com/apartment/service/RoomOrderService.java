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
    private com.apartment.repository.SysUserRepository userRepository;

    @Autowired
    private com.apartment.repository.ProductPriceRepository productPriceRepository;

    @Autowired
    private MessageTemplateService messageTemplateService;

    @Autowired
    private SysConfigService sysConfigService;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Get display name for order notifications: group name for team orders, booker name for individual.
     */
    private String getOrderDisplayName(RoomOrder order) {
        if (order.getCustomerType() != null && order.getCustomerType() == 2) {
            return order.getGroupName() != null ? order.getGroupName() : "-";
        }
        if (order.getBooker() != null) {
            return order.getBooker().getRealName() != null ? order.getBooker().getRealName() : order.getBooker().getUsername();
        }
        return "-";
    }

    /**
     * Send notification to the booker for individual orders (customerType=1).
     * Skips group orders (customerType=2) as they have no single booker to notify.
     */
    private void notifyBooker(RoomOrder order, String subject, String content, String messageType) {
        if (order.getCustomerType() != null && order.getCustomerType() == 2) return; // Skip group orders
        SysUser booker = order.getBooker();
        if (booker == null) return;

        String channel = "1".equals(booker.getSource()) ? "wecom" : "email";
        String userLocale = booker.getLocale() != null ? booker.getLocale() : "zh";

        NotificationRecord nr = new NotificationRecord();
        nr.setOrder(order);
        nr.setRecipientUser(booker);
        nr.setChannel(channel);
        nr.setMessageType(messageType);
        nr.setOrderNo(order.getOrderNo());
        nr.setLocale(userLocale);
        nr.setRecipientName(booker.getRealName() != null ? booker.getRealName() : booker.getUsername());
        nr.setSubject(subject);
        nr.setContent(content);
        notificationRecordRepository.save(nr);
    }

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

            // Resolve roomNo from DB (frontend only sends room.id)
            String roomNo = roomRepository.findById(occupy.getRoom().getId())
                    .map(Room::getRoomNo).orElse(null);

            // 1. Room Overlap check for each room
            List<RoomOrder> roomOrders = orderRepository.findByRoomIdAndStatusIn(occupy.getRoom().getId(), activeStatuses);
            for (RoomOrder existing : roomOrders) {
                if (order.getId() != null && order.getId().equals(existing.getId())) continue;

                LocalDateTime exStartDT = existing.getStartDate();
                LocalDateTime exEndDT = existing.getEndDate();

                if (startDT.isBefore(exEndDT) && endDT.isAfter(exStartDT)) {
                    throw new BusinessException(ErrorCode.ORDER_ROOM_TIME_CONFLICT, roomNo);
                }
            }

            // 2. Room Maintenance Overlap
            long maintenanceCount = maintenanceRepository.countOverlappingMaintenances(occupy.getRoom().getId(), startDT, endDT);
            if (maintenanceCount > 0) {
                throw new BusinessException(ErrorCode.ORDER_ROOM_MAINTENANCE, roomNo);
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

        // Recalculate serviceFee from product details (source of truth, not frontend value)
        BigDecimal totalServiceFee = BigDecimal.ZERO;
        if (order.getProductDetails() != null) {
            for (com.apartment.entity.OrderProductDetail detail : order.getProductDetails()) {
                if (detail.getActualPrice() != null && detail.getQuantity() != null) {
                    totalServiceFee = totalServiceFee.add(detail.getActualPrice().multiply(new BigDecimal(detail.getQuantity())));
                }
            }
        }
        order.setServiceFee(totalServiceFee);

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

        boolean isGroup = order.getCustomerType() != null && order.getCustomerType() == 2;
        LocalDateTime now = LocalDateTime.now();

        if (!isGroup) {
            // Individual order: set all Pending rooms to Checked-in
            for (RoomOccupy ro : order.getRoomOccupies()) {
                if (ro.getStatus() != null && ro.getStatus() == RoomOccupy.STATUS_PENDING) {
                    ro.setStatus(RoomOccupy.STATUS_CHECKED_IN);
                    if (ro.getCheckInTime() == null) {
                        ro.setCheckInTime(now);
                    }
                }
            }
        }
        // Group order: do NOT change room status — rooms stay Pending until individually checked in

        // Check if room card notification already sent for this order (one per order)
        if (notificationRecordRepository.existsByOrderIdAndKeyBoxNoIsNotNull(order.getId())) {
            return orderRepository.save(order);
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        // Collect all room numbers (comma-separated) from active occupies (Pending or Checked-in)
        StringBuilder roomNosBuilder = new StringBuilder();
        LocalDateTime earliestCheckIn = null;
        if (order.getRoomOccupies() != null) {
            for (RoomOccupy ro : order.getRoomOccupies()) {
                if (ro.getStatus() != null && (ro.getStatus() == RoomOccupy.STATUS_PENDING || ro.getStatus() == RoomOccupy.STATUS_CHECKED_IN) && ro.getRoom() != null) {
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

            String displayName = getOrderDisplayName(order);
            String subject = messageTemplateService.buildEmailSubject(displayName,
                ciTime != null ? ciTime.format(DATE_ONLY) : "",
                coTime != null ? coTime.format(DATE_ONLY) : "");

            NotificationRecord nr = new NotificationRecord();
            nr.setOrder(order);
            nr.setRecipientUser(recipient);
            nr.setChannel(channel);
            nr.setMessageType("key_box");
            nr.setOrderNo(order.getOrderNo());
            nr.setRoomNo(roomNosVal);
            nr.setKeyBoxNo(keyBoxNo);
            nr.setBoxPassword(boxPassword);
            nr.setLocale(userLocale);
            nr.setCheckInTime(ciTime);
            nr.setCheckOutTime(coTime);
            nr.setRecipientName(recipientNameVal);
            nr.setSubject(subject);
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

        String displayName = getOrderDisplayName(order);
        String subject = messageTemplateService.buildEmailSubject(displayName,
            ciTime != null ? ciTime.format(DATE_ONLY) : "",
            coTime != null ? coTime.format(DATE_ONLY) : "");

        for (String email : notificationEmails) {
            NotificationRecord nr = new NotificationRecord();
            nr.setOrder(order);
            nr.setChannel("email");
            nr.setMessageType("order_created");
            nr.setRecipientEmail(email);
            nr.setOrderNo(order.getOrderNo());
            nr.setRoomNo(roomNoSummary);
            nr.setLocale("zh");
            nr.setCheckInTime(ciTime);
            nr.setCheckOutTime(coTime);
            nr.setRecipientName(email);
            nr.setSubject(subject);
            nr.setContent(emailContent);
            notificationRecordRepository.save(nr);
        }
    }

    @Transactional
    public RoomOrder cancelOrder(Long orderId) {
        if (orderId == null) throw new IllegalArgumentException("Order ID cannot be null");
        RoomOrder order = orderRepository.findById(orderId).orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // Reject if any room is already checked in
        if (order.getRoomOccupies() != null) {
            for (com.apartment.entity.RoomOccupy occupy : order.getRoomOccupies()) {
                if (occupy.getStatus() != null && occupy.getStatus() == RoomOccupy.STATUS_CHECKED_IN) {
                    throw new BusinessException(ErrorCode.ORDER_HAS_CHECKED_IN_ROOM);
                }
            }
        }

        // Cancel deadline: must be no later than 24:00 of the day before check-in
        LocalDateTime cancelDeadline = order.getStartDate().toLocalDate().atStartOfDay();
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai"));
        if (now.isAfter(cancelDeadline) || now.isEqual(cancelDeadline)) {
            throw new BusinessException(ErrorCode.ORDER_CANCEL_DEADLINE_PASSED);
        }

        order.setStatus(4); // Canceled
        if (order.getRoomOccupies() != null) {
            for (com.apartment.entity.RoomOccupy occupy : order.getRoomOccupies()) {
                occupy.setStatus(RoomOccupy.STATUS_CANCELED);
                com.apartment.entity.Room room = occupy.getRoom();
                if (room != null) {
                    room.setStatus(0); // Set room back to free
                    roomRepository.save(room);
                }
            }
        }

        // Notify booker of cancellation (individual orders only)
        String displayName = getOrderDisplayName(order);
        String startDate = order.getStartDate() != null ? order.getStartDate().format(DATE_ONLY) : "";
        String endDate = order.getEndDate() != null ? order.getEndDate().format(DATE_ONLY) : "";
        SysUser booker = order.getBooker();
        String locale = booker != null && booker.getLocale() != null ? booker.getLocale() : "zh";
        String cancelSubject = messageTemplateService.buildCancelSubject(locale, displayName);
        String cancelContent = messageTemplateService.buildCancelContent(locale, order.getOrderNo(), displayName, startDate, endDate);
        notifyBooker(order, cancelSubject, cancelContent, "order_cancel");

        return orderRepository.save(order);
    }

    // Admin cancel: no time window restriction
    @Transactional
    public RoomOrder adminCancelOrder(Long orderId) {
        if (orderId == null) throw new IllegalArgumentException("Order ID cannot be null");
        RoomOrder order = orderRepository.findById(orderId).orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // Reject if any room is already checked in
        if (order.getRoomOccupies() != null) {
            for (com.apartment.entity.RoomOccupy occupy : order.getRoomOccupies()) {
                if (occupy.getStatus() != null && occupy.getStatus() == RoomOccupy.STATUS_CHECKED_IN) {
                    throw new BusinessException(ErrorCode.ORDER_HAS_CHECKED_IN_ROOM);
                }
            }
        }

        order.setStatus(4); // Canceled
        if (order.getRoomOccupies() != null) {
            for (com.apartment.entity.RoomOccupy occupy : order.getRoomOccupies()) {
                occupy.setStatus(RoomOccupy.STATUS_CANCELED);
                com.apartment.entity.Room room = occupy.getRoom();
                if (room != null) {
                    room.setStatus(0); // Set room back to free
                    roomRepository.save(room);
                }
            }
        }

        // Notify booker of cancellation (individual orders only)
        String displayName = getOrderDisplayName(order);
        String startDate = order.getStartDate() != null ? order.getStartDate().format(DATE_ONLY) : "";
        String endDate = order.getEndDate() != null ? order.getEndDate().format(DATE_ONLY) : "";
        SysUser booker = order.getBooker();
        String locale = booker != null && booker.getLocale() != null ? booker.getLocale() : "zh";
        String cancelSubject = messageTemplateService.buildCancelSubject(locale, displayName);
        String cancelContent = messageTemplateService.buildCancelContent(locale, order.getOrderNo(), displayName, startDate, endDate);
        notifyBooker(order, cancelSubject, cancelContent, "order_cancel");

        return orderRepository.save(order);
    }

    @Transactional
    public RoomOrder checkoutOrder(Long orderId) {
        if (orderId == null) throw new IllegalArgumentException("Order ID cannot be null");
        RoomOrder order = orderRepository.findById(orderId).orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        boolean isGroup = order.getCustomerType() != null && order.getCustomerType() == 2;
        if (isGroup) {
            throw new BusinessException(ErrorCode.ORDER_GROUP_CHECKOUT_PER_ROOM);
        }

        order.setStatus(3); // Out
        LocalDateTime now = LocalDateTime.now();
        order.setEndDate(now);
        if (order.getRoomOccupies() != null) {
            for (RoomOccupy occupy : order.getRoomOccupies()) {
                occupy.setStatus(RoomOccupy.STATUS_CHECKED_OUT);
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
        oldOccupy.setStatus(RoomOccupy.STATUS_CHECKED_OUT); // Checked-out
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
        newOccupy.setStatus(RoomOccupy.STATUS_PENDING); // Pending

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

        // Notify booker of room change (individual orders only)
        String displayName = getOrderDisplayName(order);
        String oldRoomNo = oldRoom != null ? oldRoom.getRoomNo() : "-";
        String newRoomNo = newRoom.getRoomNo();
        String switchDateStr = switchDate.format(DATE_ONLY);
        SysUser booker = order.getBooker();
        String locale = booker != null && booker.getLocale() != null ? booker.getLocale() : "zh";
        String changeSubject = messageTemplateService.buildRoomChangeSubject(locale, displayName, oldRoomNo, newRoomNo);
        String changeContent = messageTemplateService.buildRoomChangeContent(locale, order.getOrderNo(), displayName, oldRoomNo, newRoomNo, switchDateStr);
        notifyBooker(order, changeSubject, changeContent, "room_change");

        return orderRepository.save(order);
    }

    @Transactional
    public RoomOrder addRoom(Long orderId, Long roomId) {
        if (orderId == null || roomId == null) throw new IllegalArgumentException("IDs cannot be null");
        RoomOrder order = orderRepository.findById(orderId).orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        com.apartment.entity.Room room = roomRepository.findById(roomId).orElseThrow(() -> new BusinessException(ErrorCode.GENERAL_ROOM_NOT_FOUND));

        if (order.getStatus() != null && order.getStatus() == 3) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_CHECKED_OUT);
        }

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
        occupy.setStatus(RoomOccupy.STATUS_PENDING);

        // For individual orders, mark room as occupied immediately;
        // for group orders, room stays available until actual check-in
        if (!isGroup) {
            room.setStatus(1);
            roomRepository.save(room);
        }
        occupyRepository.save(occupy);

        return order;
    }

    @Transactional
    public RoomOrder checkInRoom(Long occupyId) {
        RoomOccupy occupy = occupyRepository.findById(occupyId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_OCCUPY_NOT_FOUND));

        if (occupy.getStatus() == null || occupy.getStatus() != RoomOccupy.STATUS_PENDING) {
            throw new BusinessException(ErrorCode.ORDER_OCCUPY_NOT_PENDING);
        }

        // Dirty room check
        if (occupy.getRoom() != null && occupy.getRoom().getId() != null) {
            List<com.apartment.entity.CleaningTask> pendingTasks = cleaningTaskRepository
                .findByRoomIdAndStatus(occupy.getRoom().getId(), 0);
            if (!pendingTasks.isEmpty()) {
                throw new BusinessException(ErrorCode.ORDER_ROOM_DIRTY);
            }
        }

        occupy.setStatus(RoomOccupy.STATUS_CHECKED_IN);
        occupy.setCheckInTime(LocalDateTime.now());
        occupyRepository.save(occupy);

        Room room = occupy.getRoom();
        if (room != null) {
            room.setStatus(1); // Occupied
            roomRepository.save(room);
        }

        RoomOrder order = occupy.getOrder();
        if (order != null && order.getStatus() != null && order.getStatus() == 1) {
            order.setStatus(2); // In
            orderRepository.save(order);
        }

        return order;
    }

    @Transactional
    public RoomOrder checkoutRoom(Long occupyId) {
        RoomOccupy occupy = occupyRepository.findById(occupyId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_OCCUPY_NOT_FOUND));

        if (occupy.getStatus() == null || occupy.getStatus() != RoomOccupy.STATUS_CHECKED_IN) {
            throw new BusinessException(ErrorCode.ORDER_OCCUPY_NOT_CHECKED_IN);
        }

        occupy.setStatus(RoomOccupy.STATUS_CHECKED_OUT);
        occupyRepository.save(occupy);

        Room room = occupy.getRoom();
        if (room != null) {
            room.setStatus(0); // Available
            roomRepository.save(room);
        }

        RoomOrder order = occupy.getOrder();
        if (order != null) {
            // Reload occupies to get latest status
            List<RoomOccupy> allOccupies = occupyRepository.findByOrderId(order.getId());
            boolean allFinished = allOccupies.stream()
                .allMatch(o -> o.getStatus() != null && (o.getStatus() == RoomOccupy.STATUS_CHECKED_OUT || o.getStatus() == RoomOccupy.STATUS_CANCELED));

            if (allFinished) {
                order.setStatus(3); // Out
                orderRepository.save(order);
            }
        }

        return order;
    }

    @Transactional
    public RoomOrder deleteRoomOccupy(Long occupyId) {
        RoomOccupy occupy = occupyRepository.findById(occupyId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_OCCUPY_NOT_FOUND));

        if (occupy.getStatus() == null || occupy.getStatus() != RoomOccupy.STATUS_PENDING) {
            throw new BusinessException(ErrorCode.ORDER_OCCUPY_NOT_PENDING);
        }

        RoomOrder order = occupy.getOrder();
        Room room = occupy.getRoom();

        // Remove from order's collection first to avoid cascade/orphanRemoval conflict.
        // RoomOrder.roomOccupies has cascade=ALL + orphanRemoval=true; if we only call
        // occupyRepository.delete(), the occupy stays in order.getRoomOccupies(), and
        // the subsequent orderRepository.save(order) cascade-merges it back — conflicting
        // with the explicit delete and potentially re-persisting the row.
        if (order != null && order.getRoomOccupies() != null) {
            order.getRoomOccupies().remove(occupy);
        }
        occupy.setOrder(null);

        occupyRepository.delete(occupy);

        if (room != null) {
            room.setStatus(0); // Available
            roomRepository.save(room);
        }

        if (order != null) {
            recalcOrderDateRange(order);
            calculatePrices(order);
            orderRepository.save(order);
        }

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

        if (occupy.getStatus() != null && (occupy.getStatus() == RoomOccupy.STATUS_CHECKED_OUT || occupy.getStatus() == RoomOccupy.STATUS_CANCELED)) {
            throw new BusinessException(ErrorCode.ORDER_OCCUPY_FINISHED);
        }

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
     * Recalculate order-level start/end dates to cover all active (Pending or Checked-in) occupy date ranges.
     */
    private void recalcOrderDateRange(RoomOrder order) {
        List<RoomOccupy> allOccupies = occupyRepository.findByOrderId(order.getId());
        LocalDateTime minStart = null;
        LocalDateTime maxEnd = null;
        for (RoomOccupy o : allOccupies) {
            if (o.getStatus() != null && (o.getStatus() == RoomOccupy.STATUS_PENDING || o.getStatus() == RoomOccupy.STATUS_CHECKED_IN)) {
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
            if (o.getStatus() != null && (o.getStatus() == RoomOccupy.STATUS_PENDING || o.getStatus() == RoomOccupy.STATUS_CHECKED_IN) && o.getCheckOutTime() == null) {
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

    // ===== Incremental save for order updates =====

    /**
     * Incrementally update an existing order based on incoming partial data.
     * @return JSON string of changed fields for logging, or null if no changes
     */
    @Transactional
    public String saveOrderIncremental(RoomOrder existing, RoomOrder incoming) {
        java.util.List<java.util.Map<String, Object>> changeLog = new java.util.ArrayList<>();

        // Capture old bizType before applyOrderFieldChanges overwrites it
        Integer oldBizType = existing.getBizType();

        // 1. Apply order-level partial update (only non-null fields from incoming)
        applyOrderFieldChanges(existing, incoming, changeLog);

        // 1.5 If bizType changed, reset actualPrice on all active occupies so calculatePrices
        //     will recalculate from the new bizType (short-term vs long-term price)
        if (incoming.getBizType() != null && !incoming.getBizType().equals(oldBizType)) {
            if (existing.getRoomOccupies() != null) {
                for (RoomOccupy occupy : existing.getRoomOccupies()) {
                    occupy.setActualPrice(null);
                }
            }
        }

        // 2. Process room occupies incrementally
        java.util.List<RoomChangeInfo> roomChanges = processRoomOccupiesIncremental(existing, incoming, changeLog);

        // 3. Process product details incrementally
        processProductDetailsIncremental(existing, incoming, changeLog);

        // 3.5 Recalculate date range and prices after room changes
        recalcOrderDateRange(existing);
        calculatePrices(existing);

        // 4. Validate and save
        validateOrder(existing);
        RoomOrder saved = orderRepository.save(existing);

        // 5. Trigger room-change notifications
        for (RoomChangeInfo info : roomChanges) {
            notifyRoomChange(saved, info);
        }

        // 6. Return change log as JSON (controller handles OrderLog creation with user context)
        if (changeLog.isEmpty()) return null;
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(changeLog);
        } catch (Exception e) {
            return changeLog.toString();
        }
    }

    /** Apply order-level partial update: only override non-null fields from incoming */
    private void applyOrderFieldChanges(RoomOrder existing, RoomOrder incoming, java.util.List<java.util.Map<String, Object>> changeLog) {
        // Scalar fields
        if (incoming.getBookPhone() != null && !incoming.getBookPhone().equals(existing.getBookPhone())) {
            addChange(changeLog, "联系电话", existing.getBookPhone(), incoming.getBookPhone());
            existing.setBookPhone(incoming.getBookPhone());
        }
        if (incoming.getStartDate() != null && !incoming.getStartDate().equals(existing.getStartDate())) {
            addChange(changeLog, "入住时间", String.valueOf(existing.getStartDate()), String.valueOf(incoming.getStartDate()));
            existing.setStartDate(incoming.getStartDate());
        }
        if (incoming.getEndDate() != null && !incoming.getEndDate().equals(existing.getEndDate())) {
            addChange(changeLog, "离店时间", String.valueOf(existing.getEndDate()), String.valueOf(incoming.getEndDate()));
            existing.setEndDate(incoming.getEndDate());
        }
        if (incoming.getBizType() != null && !incoming.getBizType().equals(existing.getBizType())) {
            addChange(changeLog, "业务类型", String.valueOf(existing.getBizType()), String.valueOf(incoming.getBizType()));
            existing.setBizType(incoming.getBizType());
        }
        if (incoming.getCustomerType() != null && !incoming.getCustomerType().equals(existing.getCustomerType())) {
            addChange(changeLog, "客户类型", String.valueOf(existing.getCustomerType()), String.valueOf(incoming.getCustomerType()));
            existing.setCustomerType(incoming.getCustomerType());
        }
        if (incoming.getStatus() != null && !incoming.getStatus().equals(existing.getStatus())) {
            addChange(changeLog, "订单状态", String.valueOf(existing.getStatus()), String.valueOf(incoming.getStatus()));
            existing.setStatus(incoming.getStatus());
        }
        if (incoming.getRemarks() != null && !incoming.getRemarks().equals(existing.getRemarks())) {
            addChange(changeLog, "备注", existing.getRemarks(), incoming.getRemarks());
            existing.setRemarks(incoming.getRemarks());
        }
        if (incoming.getRoomFee() != null && incoming.getRoomFee().compareTo(existing.getRoomFee() != null ? existing.getRoomFee() : BigDecimal.ZERO) != 0) {
            addChange(changeLog, "房费", String.valueOf(existing.getRoomFee()), String.valueOf(incoming.getRoomFee()));
            existing.setRoomFee(incoming.getRoomFee());
        }
        if (incoming.getServiceFee() != null && incoming.getServiceFee().compareTo(existing.getServiceFee() != null ? existing.getServiceFee() : BigDecimal.ZERO) != 0) {
            addChange(changeLog, "服务费", String.valueOf(existing.getServiceFee()), String.valueOf(incoming.getServiceFee()));
            existing.setServiceFee(incoming.getServiceFee());
        }
        if (incoming.getTotalAmount() != null && incoming.getTotalAmount().compareTo(existing.getTotalAmount() != null ? existing.getTotalAmount() : BigDecimal.ZERO) != 0) {
            addChange(changeLog, "总金额", String.valueOf(existing.getTotalAmount()), String.valueOf(incoming.getTotalAmount()));
            existing.setTotalAmount(incoming.getTotalAmount());
        }
        if (incoming.getGroupName() != null && !incoming.getGroupName().equals(existing.getGroupName())) {
            addChange(changeLog, "团体名称", existing.getGroupName(), incoming.getGroupName());
            existing.setGroupName(incoming.getGroupName());
        }
        if (incoming.getContactName() != null && !incoming.getContactName().equals(existing.getContactName())) {
            addChange(changeLog, "联系人姓名", existing.getContactName(), incoming.getContactName());
            existing.setContactName(incoming.getContactName());
        }
        if (incoming.getContactPhone() != null && !incoming.getContactPhone().equals(existing.getContactPhone())) {
            addChange(changeLog, "联系电话(团体)", existing.getContactPhone(), incoming.getContactPhone());
            existing.setContactPhone(incoming.getContactPhone());
        }
        if (incoming.getCompany() != null && !incoming.getCompany().equals(existing.getCompany())) {
            addChange(changeLog, "所属公司", existing.getCompany(), incoming.getCompany());
            existing.setCompany(incoming.getCompany());
        }
        if (incoming.getCostCenter() != null && !incoming.getCostCenter().equals(existing.getCostCenter())) {
            addChange(changeLog, "成本中心", existing.getCostCenter(), incoming.getCostCenter());
            existing.setCostCenter(incoming.getCostCenter());
        }
        if (incoming.getActivityCode() != null && !incoming.getActivityCode().equals(existing.getActivityCode())) {
            addChange(changeLog, "活动编码", existing.getActivityCode(), incoming.getActivityCode());
            existing.setActivityCode(incoming.getActivityCode());
        }
        if (incoming.getProjectCode() != null && !incoming.getProjectCode().equals(existing.getProjectCode())) {
            addChange(changeLog, "项目编码", existing.getProjectCode(), incoming.getProjectCode());
            existing.setProjectCode(incoming.getProjectCode());
        }

        // Reference fields
        if (incoming.getBooker() != null && incoming.getBooker().getId() != null) {
            Long oldBookerId = existing.getBooker() != null ? existing.getBooker().getId() : null;
            if (!incoming.getBooker().getId().equals(oldBookerId)) {
                String oldName = existing.getBooker() != null ? existing.getBooker().getRealName() : "空";
                // Resolve new booker name
                SysUser newBooker = userRepository.findById(incoming.getBooker().getId()).orElse(incoming.getBooker());
                String newName = newBooker.getRealName() != null ? newBooker.getRealName() : newBooker.getUsername();
                addChange(changeLog, "订房人", oldName, newName);
                existing.setBooker(newBooker);
            }
        }
        if (incoming.getPurpose() != null && incoming.getPurpose().getId() != null) {
            Long oldPurposeId = existing.getPurpose() != null ? existing.getPurpose().getId() : null;
            if (!incoming.getPurpose().getId().equals(oldPurposeId)) {
                String oldName = existing.getPurpose() != null ? existing.getPurpose().getName() : "空";
                addChange(changeLog, "订房事由", oldName, "已变更");
                existing.setPurpose(incoming.getPurpose());
            }
        }
    }

    private java.util.List<RoomChangeInfo> processRoomOccupiesIncremental(RoomOrder existing, RoomOrder incoming, java.util.List<java.util.Map<String, Object>> changeLog) {
        java.util.List<RoomChangeInfo> roomChanges = new java.util.ArrayList<>();

        if (incoming.getRoomOccupies() == null) return roomChanges;

        // Check if any _action is set; if not, fall back to legacy clear+add
        boolean hasAction = incoming.getRoomOccupies().stream().anyMatch(ro -> ro.get_action() != null);
        if (!hasAction) {
            // Legacy: clear and re-add (existing behavior)
            existing.getRoomOccupies().clear();
            for (RoomOccupy o : incoming.getRoomOccupies()) {
                o.setOrder(existing);
                if (o.getRoom() != null && o.getRoom().getId() != null) {
                    o.setRoom(roomRepository.findById(o.getRoom().getId()).orElse(o.getRoom()));
                }
                if (o.getOccupantUser() != null && o.getOccupantUser().getId() != null) {
                    o.setOccupantUser(userRepository.findById(o.getOccupantUser().getId()).orElse(o.getOccupantUser()));
                }
                existing.getRoomOccupies().add(o);
            }
            return roomChanges;
        }

        for (RoomOccupy ro : incoming.getRoomOccupies()) {
            String action = ro.get_action();
            if (action == null || action.isEmpty()) continue;

            switch (action) {
                case "add":
                    processRoomAdd(ro, existing, changeLog);
                    break;
                case "modify":
                    RoomChangeInfo info = processRoomModify(ro, existing, changeLog);
                    if (info != null) roomChanges.add(info);
                    break;
                case "delete":
                    processRoomDelete(ro, existing, changeLog);
                    break;
                case "unchanged":
                    // Skip
                    break;
            }
        }

        return roomChanges;
    }

    private void processRoomAdd(RoomOccupy ro, RoomOrder existing, java.util.List<java.util.Map<String, Object>> changeLog) {
        RoomOccupy newOccupy = new RoomOccupy();
        newOccupy.setOrder(existing);
        if (ro.getRoom() != null && ro.getRoom().getId() != null) {
            Room room = roomRepository.findById(ro.getRoom().getId()).orElse(null);
            newOccupy.setRoom(room);
            addChange(changeLog, "新增房间", "", room != null ? room.getRoomNo() : String.valueOf(ro.getRoom().getId()));
            // Update room status for individual orders (consistent with processRoomModify)
            if (room != null) {
                boolean isGroup = existing.getCustomerType() != null && existing.getCustomerType() == 2;
                if (!isGroup) {
                    room.setStatus(1); // Occupied
                    roomRepository.save(room);
                }
            }
        }
        if (ro.getOccupantUser() != null && ro.getOccupantUser().getId() != null) {
            newOccupy.setOccupantUser(userRepository.findById(ro.getOccupantUser().getId()).orElse(null));
        }
        newOccupy.setOccupantName(ro.getOccupantName());
        newOccupy.setCheckInTime(ro.getCheckInTime());
        newOccupy.setCheckOutTime(ro.getCheckOutTime());
        newOccupy.setOccupantCount(ro.getOccupantCount());
        newOccupy.setCoOccupants(ro.getCoOccupants());
        newOccupy.setStatus(ro.getStatus() != null ? ro.getStatus() : RoomOccupy.STATUS_PENDING);
        newOccupy.setActualPrice(ro.getActualPrice());
        newOccupy.setQuantity(ro.getQuantity());
        existing.getRoomOccupies().add(newOccupy);
    }

    private RoomChangeInfo processRoomModify(RoomOccupy ro, RoomOrder existing, java.util.List<java.util.Map<String, Object>> changeLog) {
        if (ro.getId() == null) return null;

        // Find the existing occupy
        RoomOccupy existingOccupy = null;
        for (RoomOccupy eo : existing.getRoomOccupies()) {
            if (ro.getId().equals(eo.getId())) {
                existingOccupy = eo;
                break;
            }
        }
        if (existingOccupy == null) return null;

        String roomPrefix = "房间[" + (existingOccupy.getRoom() != null ? existingOccupy.getRoom().getRoomNo() : ro.getId()) + "]";
        RoomChangeInfo roomChangeInfo = null;

        // Check roomId change
        if (ro.getRoom() != null && ro.getRoom().getId() != null) {
            Long oldRoomId = existingOccupy.getRoom() != null ? existingOccupy.getRoom().getId() : null;
            if (!ro.getRoom().getId().equals(oldRoomId)) {
                Room newRoom = roomRepository.findById(ro.getRoom().getId()).orElse(null);
                String oldRoomNo = existingOccupy.getRoom() != null ? existingOccupy.getRoom().getRoomNo() : "未知";
                String newRoomNo = newRoom != null ? newRoom.getRoomNo() : "未知";
                addChange(changeLog, roomPrefix + "房间号", oldRoomNo, newRoomNo);

                // Room change detected
                roomChangeInfo = new RoomChangeInfo(oldRoomNo, newRoomNo, existingOccupy.getRoom(), newRoom);

                // Free old room, occupy new room
                if (existingOccupy.getRoom() != null) {
                    existingOccupy.getRoom().setStatus(0); // Available
                    roomRepository.save(existingOccupy.getRoom());
                }
                existingOccupy.setRoom(newRoom);
                // Reset actualPrice so calculatePrices will recalculate from the new room's type
                existingOccupy.setActualPrice(null);
                if (newRoom != null) {
                    boolean isGroup = existing.getCustomerType() != null && existing.getCustomerType() == 2;
                    if (!isGroup) {
                        newRoom.setStatus(1); // Occupied
                        roomRepository.save(newRoom);
                    }
                }
            }
        }

        // Check other field changes (only apply non-null fields)
        if (ro.getOccupantUser() != null && ro.getOccupantUser().getId() != null) {
            Long oldUserId = existingOccupy.getOccupantUser() != null ? existingOccupy.getOccupantUser().getId() : null;
            if (!ro.getOccupantUser().getId().equals(oldUserId)) {
                SysUser newUser = userRepository.findById(ro.getOccupantUser().getId()).orElse(null);
                String oldName = existingOccupy.getOccupantUser() != null ? existingOccupy.getOccupantUser().getRealName() : "空";
                String newName = newUser != null ? newUser.getRealName() : "空";
                addChange(changeLog, roomPrefix + "入住人", oldName, newName);
                existingOccupy.setOccupantUser(newUser);
            }
        }
        if (ro.getOccupantName() != null && !ro.getOccupantName().equals(existingOccupy.getOccupantName())) {
            addChange(changeLog, roomPrefix + "入住人姓名", existingOccupy.getOccupantName(), ro.getOccupantName());
            existingOccupy.setOccupantName(ro.getOccupantName());
        }
        if (ro.getCheckInTime() != null && !ro.getCheckInTime().equals(existingOccupy.getCheckInTime())) {
            addChange(changeLog, roomPrefix + "入住时间", String.valueOf(existingOccupy.getCheckInTime()), String.valueOf(ro.getCheckInTime()));
            existingOccupy.setCheckInTime(ro.getCheckInTime());
        }
        if (ro.getCheckOutTime() != null && !ro.getCheckOutTime().equals(existingOccupy.getCheckOutTime())) {
            addChange(changeLog, roomPrefix + "退房时间", String.valueOf(existingOccupy.getCheckOutTime()), String.valueOf(ro.getCheckOutTime()));
            existingOccupy.setCheckOutTime(ro.getCheckOutTime());
        }
        if (ro.getOccupantCount() != null && !ro.getOccupantCount().equals(existingOccupy.getOccupantCount())) {
            addChange(changeLog, roomPrefix + "入住人数", String.valueOf(existingOccupy.getOccupantCount()), String.valueOf(ro.getOccupantCount()));
            existingOccupy.setOccupantCount(ro.getOccupantCount());
        }
        if (ro.getCoOccupants() != null && !ro.getCoOccupants().equals(existingOccupy.getCoOccupants())) {
            addChange(changeLog, roomPrefix + "同住人", existingOccupy.getCoOccupants(), ro.getCoOccupants());
            existingOccupy.setCoOccupants(ro.getCoOccupants());
        }
        if (ro.getStatus() != null && !ro.getStatus().equals(existingOccupy.getStatus())) {
            // Reject invalid status transitions (e.g., checked-in back to pending)
            if (existingOccupy.getStatus() == RoomOccupy.STATUS_CHECKED_IN && ro.getStatus() == RoomOccupy.STATUS_PENDING) {
                throw new BusinessException(ErrorCode.ORDER_OCCUPY_STATUS_INVALID);
            }
            if (existingOccupy.getStatus() == RoomOccupy.STATUS_CHECKED_OUT || existingOccupy.getStatus() == RoomOccupy.STATUS_CANCELED) {
                throw new BusinessException(ErrorCode.ORDER_OCCUPY_FINISHED);
            }
            addChange(changeLog, roomPrefix + "状态", String.valueOf(existingOccupy.getStatus()), String.valueOf(ro.getStatus()));
            existingOccupy.setStatus(ro.getStatus());
        }
        if (ro.getActualPrice() != null && (existingOccupy.getActualPrice() == null || ro.getActualPrice().compareTo(existingOccupy.getActualPrice()) != 0)) {
            addChange(changeLog, roomPrefix + "单价", String.valueOf(existingOccupy.getActualPrice()), String.valueOf(ro.getActualPrice()));
            existingOccupy.setActualPrice(ro.getActualPrice());
        }
        if (ro.getQuantity() != null && !ro.getQuantity().equals(existingOccupy.getQuantity())) {
            addChange(changeLog, roomPrefix + "数量", String.valueOf(existingOccupy.getQuantity()), String.valueOf(ro.getQuantity()));
            existingOccupy.setQuantity(ro.getQuantity());
        }

        return roomChangeInfo;
    }

    private void processRoomDelete(RoomOccupy ro, RoomOrder existing, java.util.List<java.util.Map<String, Object>> changeLog) {
        if (ro.getId() == null) return;

        RoomOccupy toRemove = null;
        for (RoomOccupy eo : existing.getRoomOccupies()) {
            if (ro.getId().equals(eo.getId())) {
                toRemove = eo;
                break;
            }
        }
        if (toRemove != null) {
            String roomNo = toRemove.getRoom() != null ? toRemove.getRoom().getRoomNo() : String.valueOf(ro.getId());
            addChange(changeLog, "删除房间", roomNo, "");
            // Free room
            if (toRemove.getRoom() != null) {
                toRemove.getRoom().setStatus(0);
                roomRepository.save(toRemove.getRoom());
            }
            existing.getRoomOccupies().remove(toRemove);
        }
    }

    private void processProductDetailsIncremental(RoomOrder existing, RoomOrder incoming, java.util.List<java.util.Map<String, Object>> changeLog) {
        if (incoming.getProductDetails() == null) return;

        boolean hasAction = incoming.getProductDetails().stream().anyMatch(pd -> pd.get_action() != null);
        if (!hasAction) {
            // Legacy: clear and re-add
            existing.getProductDetails().clear();
            for (com.apartment.entity.OrderProductDetail d : incoming.getProductDetails()) {
                d.setOrder(existing);
                if (d.getProduct() != null && d.getProduct().getId() != null) {
                    d.setProduct(productPriceRepository.findById(d.getProduct().getId()).orElse(d.getProduct()));
                }
                existing.getProductDetails().add(d);
            }
            return;
        }

        for (com.apartment.entity.OrderProductDetail pd : incoming.getProductDetails()) {
            String action = pd.get_action();
            if (action == null || action.isEmpty()) continue;

            switch (action) {
                case "add":
                    processProductAdd(pd, existing, changeLog);
                    break;
                case "modify":
                    processProductModify(pd, existing, changeLog);
                    break;
                case "delete":
                    processProductDelete(pd, existing, changeLog);
                    break;
                case "unchanged":
                    break;
            }
        }
    }

    private void processProductAdd(com.apartment.entity.OrderProductDetail pd, RoomOrder existing, java.util.List<java.util.Map<String, Object>> changeLog) {
        com.apartment.entity.OrderProductDetail newDetail = new com.apartment.entity.OrderProductDetail();
        newDetail.setOrder(existing);
        if (pd.getProduct() != null && pd.getProduct().getId() != null) {
            newDetail.setProduct(productPriceRepository.findById(pd.getProduct().getId()).orElse(pd.getProduct()));
            addChange(changeLog, "新增商品", "", pd.getProduct().getId().toString());
        }
        newDetail.setActualPrice(pd.getActualPrice());
        newDetail.setQuantity(pd.getQuantity() != null ? pd.getQuantity() : 1);
        newDetail.setConsumeDate(pd.getConsumeDate());
        newDetail.setRemarks(pd.getRemarks());
        existing.getProductDetails().add(newDetail);
    }

    private void processProductModify(com.apartment.entity.OrderProductDetail pd, RoomOrder existing, java.util.List<java.util.Map<String, Object>> changeLog) {
        if (pd.getId() == null) return;
        com.apartment.entity.OrderProductDetail existingDetail = null;
        for (com.apartment.entity.OrderProductDetail ed : existing.getProductDetails()) {
            if (pd.getId().equals(ed.getId())) {
                existingDetail = ed;
                break;
            }
        }
        if (existingDetail == null) return;

        String prefix = "商品[" + pd.getId() + "]";
        if (pd.getProduct() != null && pd.getProduct().getId() != null) {
            Long oldId = existingDetail.getProduct() != null ? existingDetail.getProduct().getId() : null;
            if (!pd.getProduct().getId().equals(oldId)) {
                existingDetail.setProduct(productPriceRepository.findById(pd.getProduct().getId()).orElse(pd.getProduct()));
                addChange(changeLog, prefix + "商品", String.valueOf(oldId), String.valueOf(pd.getProduct().getId()));
            }
        }
        if (pd.getActualPrice() != null && (existingDetail.getActualPrice() == null || pd.getActualPrice().compareTo(existingDetail.getActualPrice()) != 0)) {
            addChange(changeLog, prefix + "单价", String.valueOf(existingDetail.getActualPrice()), String.valueOf(pd.getActualPrice()));
            existingDetail.setActualPrice(pd.getActualPrice());
        }
        if (pd.getQuantity() != null && !pd.getQuantity().equals(existingDetail.getQuantity())) {
            addChange(changeLog, prefix + "数量", String.valueOf(existingDetail.getQuantity()), String.valueOf(pd.getQuantity()));
            existingDetail.setQuantity(pd.getQuantity());
        }
        if (pd.getConsumeDate() != null && !pd.getConsumeDate().equals(existingDetail.getConsumeDate())) {
            addChange(changeLog, prefix + "消费日期", String.valueOf(existingDetail.getConsumeDate()), String.valueOf(pd.getConsumeDate()));
            existingDetail.setConsumeDate(pd.getConsumeDate());
        }
        if (pd.getRemarks() != null && !pd.getRemarks().equals(existingDetail.getRemarks())) {
            addChange(changeLog, prefix + "备注", existingDetail.getRemarks(), pd.getRemarks());
            existingDetail.setRemarks(pd.getRemarks());
        }
    }

    private void processProductDelete(com.apartment.entity.OrderProductDetail pd, RoomOrder existing, java.util.List<java.util.Map<String, Object>> changeLog) {
        if (pd.getId() == null) return;
        com.apartment.entity.OrderProductDetail toRemove = null;
        for (com.apartment.entity.OrderProductDetail ed : existing.getProductDetails()) {
            if (pd.getId().equals(ed.getId())) {
                toRemove = ed;
                break;
            }
        }
        if (toRemove != null) {
            addChange(changeLog, "删除商品", String.valueOf(pd.getId()), "");
            existing.getProductDetails().remove(toRemove);
        }
    }

    private void notifyRoomChange(RoomOrder order, RoomChangeInfo info) {
        String displayName = getOrderDisplayName(order);
        String switchDateStr = LocalDateTime.now().format(DATE_ONLY);
        SysUser booker = order.getBooker();
        String locale = booker != null && booker.getLocale() != null ? booker.getLocale() : "zh";
        String changeSubject = messageTemplateService.buildRoomChangeSubject(locale, displayName, info.oldRoomNo, info.newRoomNo);
        String changeContent = messageTemplateService.buildRoomChangeContent(locale, order.getOrderNo(), displayName, info.oldRoomNo, info.newRoomNo, switchDateStr);
        notifyBooker(order, changeSubject, changeContent, "room_change");
    }

    private void addChange(java.util.List<java.util.Map<String, Object>> changes, String field, String oldVal, String newVal) {
        if (oldVal == null) oldVal = "空";
        if (newVal == null) newVal = "空";
        if (!oldVal.equals(newVal)) {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("field", field);
            m.put("oldValue", oldVal);
            m.put("newValue", newVal);
            changes.add(m);
        }
    }

    /** Internal class to carry room change info for notification */
    private static class RoomChangeInfo {
        final String oldRoomNo;
        final String newRoomNo;
        final Room oldRoom;
        final Room newRoom;

        RoomChangeInfo(String oldRoomNo, String newRoomNo, Room oldRoom, Room newRoom) {
            this.oldRoomNo = oldRoomNo;
            this.newRoomNo = newRoomNo;
            this.oldRoom = oldRoom;
            this.newRoom = newRoom;
        }
    }
}
