package com.apartment.controller;

import com.apartment.entity.OrderLog;
import com.apartment.entity.RoomOccupy;
import com.apartment.entity.RoomOrder;
import com.apartment.entity.SysUser;
import com.apartment.exception.BusinessException;
import com.apartment.exception.ErrorCode;
import com.apartment.repository.OrderLogRepository;
import com.apartment.repository.ProductPriceRepository;
import com.apartment.repository.RoomOccupyRepository;
import com.apartment.repository.RoomOrderRepository;
import com.apartment.repository.RoomRepository;
import com.apartment.repository.SysUserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
public class RoomOrderController {
    @Autowired
    private RoomOrderRepository orderRepository;

    @Autowired
    private com.apartment.service.RoomOrderService orderService;

    @Autowired
    private com.apartment.repository.BookingPurposeRepository bookingPurposeRepository;

    @Autowired
    private SysUserRepository userRepository;

    @Autowired
    private OrderLogRepository orderLogRepository;

    @Autowired
    private RoomOccupyRepository occupyRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ProductPriceRepository productPriceRepository;

    private SysUser getCurrentUser() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    private void logOperation(RoomOrder order, String type, String content, String changedFields) {
        SysUser user = getCurrentUser();
        if (user == null) return;
        OrderLog log = new OrderLog();
        log.setOrder(order);
        log.setOperator(user);
        log.setOperationTime(LocalDateTime.now());
        log.setOperationType(type);
        log.setOperationContent(content);
        log.setChangedFields(changedFields);
        orderLogRepository.save(log);
    }

    @GetMapping("/all")
    public List<RoomOrder> getAllOrders() {
        return orderRepository.findAll();
    }

    @GetMapping("/mine")
    public List<RoomOrder> getMyOrders() {
        SysUser u = getCurrentUser();
        if (u == null) return List.of();
        return orderRepository.findByBookerIdOrderByCreatedAtDesc(u.getId());
    }

    @GetMapping("/mine/paged")
    public org.springframework.data.domain.Page<RoomOrder> getMyOrdersPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        SysUser u = getCurrentUser();
        if (u == null) return org.springframework.data.domain.Page.empty();
        return orderRepository.findByBookerIdOrderByCreatedAtDesc(u.getId(),
                org.springframework.data.domain.PageRequest.of(page, size));
    }

    @GetMapping("/mine/pending")
    public org.springframework.data.domain.Page<RoomOrder> getMyPendingOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        SysUser u = getCurrentUser();
        if (u == null) return org.springframework.data.domain.Page.empty();
        // Pending check-in: status 0 (Cooling-off) or 1 (Pending/Booked)
        return orderRepository.findByBookerIdAndStatusInOrderByCreatedAtDesc(u.getId(),
                java.util.Arrays.asList(0, 1),
                org.springframework.data.domain.PageRequest.of(page, size));
}

    @GetMapping("/mine/pending-count")
    public long getMyPendingCount() {
        SysUser u = getCurrentUser();
        if (u == null) return 0;
        return orderRepository.findByBookerIdAndStatusIn(u.getId(), java.util.Arrays.asList(0, 1)).size();
    }

    @PostMapping
    public RoomOrder saveOrder(@RequestBody RoomOrder order) {
        SysUser u = getCurrentUser();
        if (u != null) {
            order.setLastUpdateUser(u);

            // If creating a new order in status 0 (Cooling-off), reject if one already exists
            if (order.getId() == null && order.getStatus() != null && order.getStatus() == 0) {
                List<RoomOrder> existing = orderRepository.findByBookerIdAndStatus(u.getId(), 0);
                if (!existing.isEmpty()) {
                    throw new BusinessException(ErrorCode.ORDER_ALREADY_EXISTS);
                }

                // Mobile cutoff: after 15:00 Beijing time, cannot book for today
                java.time.ZonedDateTime beijingNow = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Shanghai"));
                if (beijingNow.getHour() >= 15 && order.getStartDate() != null) {
                    java.time.LocalDate today = beijingNow.toLocalDate();
                    java.time.LocalDate orderStartDate = order.getStartDate().toLocalDate();
                    if (!orderStartDate.isAfter(today)) {
                        throw new BusinessException(ErrorCode.ORDER_MOBILE_CUTOFF);
                    }
                }
            }

            if (order.getId() == null) {
                // NEW ORDER — apply entity defaults that were removed from field initializers
                // to prevent Jackson defaults from overwriting existing values on incremental updates
                order.setOrderNo(null); // Force regeneration to prevent duplicate orderNo from frontend cache
                if (order.getCustomerType() == null) order.setCustomerType(1);
                if (order.getStatus() == null) order.setStatus(0);
                if (order.getTotalAmount() == null) order.setTotalAmount(BigDecimal.ZERO);
                if (order.getRoomFee() == null) order.setRoomFee(BigDecimal.ZERO);
                if (order.getServiceFee() == null) order.setServiceFee(BigDecimal.ZERO);
                if (order.getCreateUser() == null) {
                    order.setCreateUser(u);
                }
                if (order.getBooker() == null) {
                    order.setBooker(u);
                }
                if (order.getPurpose() == null) {
                    bookingPurposeRepository.findByName("差旅").ifPresent(order::setPurpose);
                }
            } else {
                // UPDATE ORDER
                RoomOrder existing = orderRepository.findById(order.getId()).orElse(null);
                if (existing != null) {
                    existing.setLastUpdateUser(u);
                    if (order.getCreateUser() == null) {
                        order.setCreateUser(existing.getCreateUser());
                    }
                    if (order.getCreatedAt() == null) {
                        order.setCreatedAt(existing.getCreatedAt());
                    }

                    // Check if incremental protocol is used (any _action marker set)
                    boolean hasActionMarkers = false;
                    if (order.getRoomOccupies() != null) {
                        hasActionMarkers = order.getRoomOccupies().stream()
                            .anyMatch(ro -> ro.get_action() != null && !ro.get_action().isEmpty());
                    }
                    if (!hasActionMarkers && order.getProductDetails() != null) {
                        hasActionMarkers = order.getProductDetails().stream()
                            .anyMatch(pd -> pd.get_action() != null && !pd.get_action().isEmpty());
                    }

                    if (hasActionMarkers) {
                        // Incremental protocol: partial update with action markers
                        String changes = orderService.saveOrderIncremental(existing, order);
                        RoomOrder saved = orderRepository.findById(existing.getId()).orElse(existing);
                        if (changes != null) {
                            logOperation(saved, "SAVE", "编辑订单并保存", changes);
                        }
                        return saved;
                    }

                    // Legacy protocol: full replace
                    String changes = buildChangeLog(existing, order);
                    if (changes != null && !changes.isEmpty()) {
                        // Copy room occupies from incoming payload onto managed entity
                        existing.setBooker(order.getBooker());
                        existing.setBookPhone(order.getBookPhone());
                        existing.setStartDate(order.getStartDate());
                        existing.setEndDate(order.getEndDate());
                        existing.setBizType(order.getBizType());
                        existing.setCustomerType(order.getCustomerType());
                        existing.setStatus(order.getStatus());
                        existing.setRemarks(order.getRemarks());
                        existing.setRoomFee(order.getRoomFee());
                        existing.setServiceFee(order.getServiceFee());
                        existing.setTotalAmount(order.getTotalAmount());
                        existing.setCompany(order.getCompany());
                        existing.setCostCenter(order.getCostCenter());
                        existing.setGroupName(order.getGroupName());
                        existing.setContactName(order.getContactName());
                        existing.setContactPhone(order.getContactPhone());
                        existing.setActivityCode(order.getActivityCode());
                        existing.setProjectCode(order.getProjectCode());
                        existing.setPurpose(order.getPurpose());

                        // Replace room occupies on the managed entity
                        if (order.getRoomOccupies() != null) {
                            existing.getRoomOccupies().clear();
                            for (com.apartment.entity.RoomOccupy o : order.getRoomOccupies()) {
                                o.setOrder(existing);
                                if (o.getRoom() != null && o.getRoom().getId() != null) {
                                    o.setRoom(roomRepository.findById(o.getRoom().getId()).orElse(o.getRoom()));
                                }
                                if (o.getOccupantUser() != null && o.getOccupantUser().getId() != null) {
                                    o.setOccupantUser(userRepository.findById(o.getOccupantUser().getId()).orElse(o.getOccupantUser()));
                                }
                                existing.getRoomOccupies().add(o);
                            }
                        }

                        // Replace product details on the managed entity
                        if (order.getProductDetails() != null) {
                            existing.getProductDetails().clear();
                            for (com.apartment.entity.OrderProductDetail d : order.getProductDetails()) {
                                d.setOrder(existing);
                                if (d.getProduct() != null && d.getProduct().getId() != null) {
                                    d.setProduct(productPriceRepository.findById(d.getProduct().getId()).orElse(d.getProduct()));
                                }
                                existing.getProductDetails().add(d);
                            }
                        }

                        orderService.validateOrder(existing);
                        RoomOrder saved = orderRepository.save(existing);
                        logOperation(saved, "SAVE", "编辑订单并保存", changes);
                        return saved;
                    }
                }
            }

            if (order.getBookPhone() == null || order.getBookPhone().isBlank()) {
                order.setBookPhone(u.getPhone());
            }
            boolean isGroup = order.getCustomerType() != null && order.getCustomerType() == 2;
            if (order.getRoomOccupies() != null) {
                order.getRoomOccupies().forEach(occupy -> {
                    occupy.setOrder(order);
                    if (!isGroup && occupy.getOccupantUser() == null && order.getId() == null) {
                        occupy.setOccupantUser(u);
                    }
                });
            }
        }
        orderService.validateOrder(order);
        if (order.getProductDetails() != null) {
            order.getProductDetails().forEach(detail -> detail.setOrder(order));
        }
        RoomOrder saved = orderRepository.save(order);

        // Send order notification email for new orders with status 1 (Pending/Booked)
        if (saved.getId() != null && saved.getStatus() != null && saved.getStatus() == 1) {
            try {
                orderService.sendOrderNotification(saved.getId());
            } catch (Exception e) {
                // Notification failure should not block order creation
                org.slf4j.LoggerFactory.getLogger(getClass()).warn("Failed to send order notification for order {}", saved.getId(), e);
            }
        }

        return saved;
    }

    /** Compare old and new order fields, return JSON array of changes */
    private String buildChangeLog(RoomOrder oldOrder, RoomOrder newOrder) {
        java.util.List<java.util.Map<String, Object>> changes = new java.util.ArrayList<>();
        addChange(changes, "订房人", oldValue(oldOrder.getBooker() != null ? oldOrder.getBooker().getRealName() : null),
                newValue(newOrder.getBooker() != null ? newOrder.getBooker().getRealName() : null));
        addChange(changes, "联系电话", oldValue(oldOrder.getBookPhone()), newValue(newOrder.getBookPhone()));
        addChange(changes, "入住时间", oldValue(oldOrder.getStartDate()), newValue(newOrder.getStartDate()));
        addChange(changes, "离店时间", oldValue(oldOrder.getEndDate()), newValue(newOrder.getEndDate()));
        addChange(changes, "业务类型", oldValue(oldOrder.getBizType()), newValue(newOrder.getBizType()));
        addChange(changes, "订房事由",
                oldValue(oldOrder.getPurpose() != null ? oldOrder.getPurpose().getName() : null),
                newValue(newOrder.getPurpose() != null ? newOrder.getPurpose().getName() : null));
        addChange(changes, "客户类型", oldValue(oldOrder.getCustomerType()), newValue(newOrder.getCustomerType()));
        addChange(changes, "订单状态", oldValue(oldOrder.getStatus()), newValue(newOrder.getStatus()));
        addChange(changes, "备注", oldValue(oldOrder.getRemarks()), newValue(newOrder.getRemarks()));
        addChange(changes, "房费", oldValue(oldOrder.getRoomFee()), newValue(newOrder.getRoomFee()));
        addChange(changes, "服务费", oldValue(oldOrder.getServiceFee()), newValue(newOrder.getServiceFee()));
        addChange(changes, "总金额", oldValue(oldOrder.getTotalAmount()), newValue(newOrder.getTotalAmount()));
        addChange(changes, "团体名称", oldValue(oldOrder.getGroupName()), newValue(newOrder.getGroupName()));
        addChange(changes, "联系人姓名", oldValue(oldOrder.getContactName()), newValue(newOrder.getContactName()));
        addChange(changes, "联系电话", oldValue(oldOrder.getContactPhone()), newValue(newOrder.getContactPhone()));
        addChange(changes, "所属公司", oldValue(oldOrder.getCompany()), newValue(newOrder.getCompany()));
        addChange(changes, "成本中心", oldValue(oldOrder.getCostCenter()), newValue(newOrder.getCostCenter()));
        addChange(changes, "活动编码", oldValue(oldOrder.getActivityCode()), newValue(newOrder.getActivityCode()));
        addChange(changes, "项目编码", oldValue(oldOrder.getProjectCode()), newValue(newOrder.getProjectCode()));
        if (changes.isEmpty()) return null;
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(changes);
        } catch (Exception e) {
            return changes.toString();
        }
    }

    private String formatValue(Object val) {
        if (val == null) return "空";
        return String.valueOf(val);
    }

    private String oldValue(Object val) { return formatValue(val); }
    private String newValue(Object val) { return formatValue(val); }

    private void addChange(java.util.List<java.util.Map<String, Object>> changes, String field, String oldVal, String newVal) {
        if (!java.util.Objects.equals(oldVal, newVal)) {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("field", field);
            m.put("oldValue", oldVal);
            m.put("newValue", newVal);
            changes.add(m);
        }
    }

    @PostMapping("/{id}/send-card")
    public RoomOrder sendRoomCard(@PathVariable Long id, @RequestBody(required = false) java.util.Map<String, String> body) {
        String keyBoxNo = body != null ? body.get("keyBoxNo") : null;
        String boxPassword = body != null ? body.get("boxPassword") : null;
        RoomOrder result = orderService.sendRoomCard(id, keyBoxNo, boxPassword);
        logOperation(result, "SEND_CARD", "发送房卡", null);
        return result;
    }

    @PostMapping("/{id}/add-fee")
    public com.apartment.entity.OrderFee addFee(@PathVariable Long id, @RequestBody com.apartment.entity.OrderFee fee) {
        return orderService.addExtraFee(id, fee.getFeeType(), fee.getAmount(), fee.getRemarks());
    }

    @GetMapping("/{id}/fees")
    public List<com.apartment.entity.OrderFee> getFees(@PathVariable Long id) {
        return orderService.getOrderFees(id);
    }

    @GetMapping("/{id}/logs")
    public List<OrderLog> getLogs(@PathVariable Long id) {
        return orderLogRepository.findByOrderIdOrderByOperationTimeDesc(id);
    }

    @PostMapping("/{id}/cancel")
    public RoomOrder cancel(@PathVariable Long id) {
        RoomOrder result = orderService.cancelOrder(id);
        logOperation(result, "CANCEL", "取消订单", null);
        return result;
    }

    @PostMapping("/{id}/admin-cancel")
    public RoomOrder adminCancel(@PathVariable Long id) {
        RoomOrder result = orderService.adminCancelOrder(id);
        logOperation(result, "CANCEL", "管理后台取消订单", null);
        return result;
    }

    @PostMapping("/{id}/checkout")
    public RoomOrder checkout(@PathVariable Long id) {
        RoomOrder result = orderService.checkoutOrder(id);
        logOperation(result, "CHECKOUT", "办理退房", null);
        return result;
    }

    @PostMapping("/occupy/{occupyId}/change-room")
    public RoomOrder changeRoom(@PathVariable Long occupyId, @RequestParam Long roomId,
                                @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime switchDate) {
        RoomOrder result = orderService.changeRoom(occupyId, roomId, switchDate);
        String desc = "换房: " + switchDate;
        logOperation(result, "CHANGE_ROOM", desc, null);
        return result;
    }

    @PostMapping("/{id}/add-room")
    public RoomOrder addRoom(@PathVariable Long id, @RequestParam Long roomId) {
        return orderService.addRoom(id, roomId);
    }

    @PostMapping("/occupy/{occupyId}/check-in")
    public RoomOrder checkInRoom(@PathVariable Long occupyId) {
        RoomOccupy occupy = occupyRepository.findById(occupyId).orElse(null);
        String roomNo = occupy != null && occupy.getRoom() != null ? occupy.getRoom().getRoomNo() : "未知房间";
        RoomOrder result = orderService.checkInRoom(occupyId);
        logOperation(result, "CHECK_IN", "房间入住: " + roomNo, null);
        return result;
    }

    @PostMapping("/occupy/{occupyId}/checkout")
    public RoomOrder checkoutRoom(@PathVariable Long occupyId) {
        RoomOccupy occupy = occupyRepository.findById(occupyId).orElse(null);
        String roomNo = occupy != null && occupy.getRoom() != null ? occupy.getRoom().getRoomNo() : "未知房间";
        RoomOrder result = orderService.checkoutRoom(occupyId);
        logOperation(result, "ROOM_CHECKOUT", "房间退房: " + roomNo, null);
        return result;
    }

    @DeleteMapping("/occupy/{occupyId}")
    public RoomOrder deleteRoomOccupy(@PathVariable Long occupyId) {
        RoomOccupy occupy = occupyRepository.findById(occupyId).orElse(null);
        String roomNo = occupy != null && occupy.getRoom() != null ? occupy.getRoom().getRoomNo() : "未知房间";
        RoomOrder result = orderService.deleteRoomOccupy(occupyId);
        logOperation(result, "DELETE_ROOM", "删除房间: " + roomNo, null);
        return result;
    }

    @DeleteMapping("/{id}")
    public void deleteOrder(@PathVariable Long id) {
        orderRepository.deleteById(id);
    }

    private static final DateTimeFormatter REPORT_DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @GetMapping("/paged-for-report")
    public Page<RoomOrder> getPagedForReport(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String bookerName,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long purposeId) {

        List<RoomOrder> allOrders = orderRepository.findAll();

        // Filter: only checked-out orders (status=3)
        List<RoomOrder> filtered = new java.util.ArrayList<>();
        for (RoomOrder o : allOrders) {
            if (o.getStatus() == null || o.getStatus() != 3) continue;
            // Date range filter: order endDate (checkout date) within range
            if (startDate != null && o.getEndDate() != null && o.getEndDate().isBefore(startDate)) continue;
            if (endDate != null && o.getEndDate() != null && o.getEndDate().isAfter(endDate)) continue;
            // Booker name filter
            if (bookerName != null && !bookerName.isBlank()) {
                String name = o.getBooker() != null ? o.getBooker().getRealName() : "";
                if (name == null || !name.contains(bookerName)) continue;
            }
            // Order number filter
            if (orderNo != null && !orderNo.isBlank()) {
                String no = o.getOrderNo() != null ? o.getOrderNo() : "";
                if (!no.contains(orderNo)) continue;
            }
            // Purpose filter
            if (purposeId != null) {
                if (o.getPurpose() == null || !o.getPurpose().getId().equals(purposeId)) continue;
            }
            filtered.add(o);
        }

        // Sort by checkout date (endDate) descending
        filtered.sort((a, b) -> {
            LocalDateTime aTime = a.getEndDate() != null ? a.getEndDate() : LocalDateTime.MIN;
            LocalDateTime bTime = b.getEndDate() != null ? b.getEndDate() : LocalDateTime.MIN;
            return bTime.compareTo(aTime);
        });

        // Pagination
        int total = filtered.size();
        int start = Math.min(page * size, total);
        int end = Math.min(start + size, total);
        List<RoomOrder> pageContent = filtered.subList(start, end);

        return new PageImpl<>(pageContent, PageRequest.of(page, size), total);
    }

    @GetMapping("/report-excel")
    public ResponseEntity<byte[]> exportReportExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String bookerName,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long purposeId) {

        // Get all filtered data (same filter logic as paged endpoint)
        List<RoomOrder> allOrders = orderRepository.findAll();
        // Filter: only checked-out orders (status=3)
        List<RoomOrder> filtered = new java.util.ArrayList<>();
        for (RoomOrder o : allOrders) {
            if (o.getStatus() == null || o.getStatus() != 3) continue;
            // Date range filter: order endDate (checkout date) within range
            if (startDate != null && o.getEndDate() != null && o.getEndDate().isBefore(startDate)) continue;
            if (endDate != null && o.getEndDate() != null && o.getEndDate().isAfter(endDate)) continue;
            if (bookerName != null && !bookerName.isBlank()) {
                String name = o.getBooker() != null ? o.getBooker().getRealName() : "";
                if (name == null || !name.contains(bookerName)) continue;
            }
            if (orderNo != null && !orderNo.isBlank()) {
                String no = o.getOrderNo() != null ? o.getOrderNo() : "";
                if (!no.contains(orderNo)) continue;
            }
            if (purposeId != null) {
                if (o.getPurpose() == null || !o.getPurpose().getId().equals(purposeId)) continue;
            }
            filtered.add(o);
        }

        // Sort by checkout date (endDate) descending
        filtered.sort((a, b) -> {
            LocalDateTime aTime = a.getEndDate() != null ? a.getEndDate() : LocalDateTime.MIN;
            LocalDateTime bTime = b.getEndDate() != null ? b.getEndDate() : LocalDateTime.MIN;
            return bTime.compareTo(aTime);
        });

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("财务报表");

            CellStyle boldStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldStyle.setFont(boldFont);

            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            Row header = sheet.createRow(0);
            String[] headers = {"#", "订单号", "订房人", "用户名", "电话号码", "邮箱", "所属公司", "成本中心", "活动编码", "项目编码", "订房事由", "入住时间", "退房时间", "房间费", "商品服务费", "订单总金额"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(boldStyle);
            }

            int rowNum = 1;
            for (int idx = 0; idx < filtered.size(); idx++) {
                RoomOrder o = filtered.get(idx);
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(idx + 1);
                row.createCell(1).setCellValue(o.getOrderNo() != null ? o.getOrderNo() : "");
                row.createCell(2).setCellValue(o.getBooker() != null ? o.getBooker().getRealName() : "");
                row.createCell(3).setCellValue(o.getBooker() != null ? o.getBooker().getUsername() : "");
                row.createCell(4).setCellValue(o.getBookPhone() != null ? o.getBookPhone() : "");
                row.createCell(5).setCellValue(o.getBooker() != null && o.getBooker().getEmail() != null ? o.getBooker().getEmail() : "");
                row.createCell(6).setCellValue(o.getCompany() != null ? o.getCompany() : "");
                row.createCell(7).setCellValue(o.getCostCenter() != null ? o.getCostCenter() : "");
                row.createCell(8).setCellValue(o.getActivityCode() != null ? o.getActivityCode() : "");
                row.createCell(9).setCellValue(o.getProjectCode() != null ? o.getProjectCode() : "");
                row.createCell(10).setCellValue(o.getPurpose() != null ? o.getPurpose().getName() : "");
                row.createCell(11).setCellValue(o.getStartDate() != null ? o.getStartDate().format(REPORT_DT_FMT) : "");
                row.createCell(12).setCellValue(o.getEndDate() != null ? o.getEndDate().format(REPORT_DT_FMT) : "");
                Cell roomFeeCell = row.createCell(13);
                roomFeeCell.setCellValue(o.getRoomFee() != null ? o.getRoomFee().doubleValue() : 0);
                roomFeeCell.setCellStyle(moneyStyle);
                Cell serviceFeeCell = row.createCell(14);
                serviceFeeCell.setCellValue(o.getServiceFee() != null ? o.getServiceFee().doubleValue() : 0);
                serviceFeeCell.setCellStyle(moneyStyle);
                Cell totalCell = row.createCell(15);
                totalCell.setCellValue(o.getTotalAmount() != null ? o.getTotalAmount().doubleValue() : 0);
                totalCell.setCellStyle(moneyStyle);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"financial_report.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel: " + e.getMessage(), e);
        }
    }

    @PostMapping("/occupy/{occupyId}/adjust-time")
    public RoomOrder adjustOccupyTime(@PathVariable Long occupyId,
                                     @RequestParam String startDate,
                                     @RequestParam String endDate) {
        java.time.LocalDateTime start = java.time.LocalDateTime.parse(startDate);
        java.time.LocalDateTime end = java.time.LocalDateTime.parse(endDate);

        // Fetch occupy info before modification for logging
        RoomOccupy occupy = occupyRepository.findById(occupyId).orElse(null);
        String roomNo = occupy != null && occupy.getRoom() != null ? occupy.getRoom().getRoomNo() : "未知房间";
        String oldStart = occupy != null && occupy.getCheckInTime() != null ? occupy.getCheckInTime().toString() : "-";
        String oldEnd = occupy != null && occupy.getCheckOutTime() != null ? occupy.getCheckOutTime().toString() : "-";

        RoomOrder result = orderService.adjustOccupyTime(occupyId, start, end);
        String desc = String.format("房间%s入住时间调整: %s→%s, %s→%s", roomNo, oldStart, start, oldEnd, end);
        logOperation(result, "ADJUST_TIME", desc, null);
        return result;
    }
}
