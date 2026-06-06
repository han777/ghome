package com.apartment.service;

import com.apartment.dto.RoomStatusDashboardDTO;
import com.apartment.entity.*;
import com.apartment.repository.CleaningTaskRepository;
import com.apartment.repository.RoomOrderRepository;
import com.apartment.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RoomStatusService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomOrderRepository orderRepository;

    @Autowired
    private com.apartment.repository.RoomMaintenanceRepository maintenanceRepository;

    @Autowired
    private CleaningTaskRepository cleaningTaskRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String getRoomTypeNameZh(RoomType roomType) {
        if (roomType == null) return "N/A";
        String json = roomType.getNameIntlJson();
        if (json == null || json.isBlank()) return roomType.getTypeCode();
        try {
            JsonNode node = objectMapper.readTree(json);
            String zh = node.has("zh") ? node.get("zh").asText() : null;
            return zh != null && !zh.isBlank() ? zh : roomType.getTypeCode();
        } catch (Exception e) {
            return roomType.getTypeCode();
        }
    }

    /** Resolve guest display name: prefer occupantName (group orders), fallback to booker.realName */
    private String resolveGuestName(RoomOrder order, Long roomId) {
        if (order.getRoomOccupies() != null) {
            for (RoomOccupy ro : order.getRoomOccupies()) {
                if (ro.getRoom() != null && ro.getRoom().getId().equals(roomId) && ro.getOccupantName() != null && !ro.getOccupantName().isBlank()) {
                    return ro.getOccupantName();
                }
            }
        }
        return order.getBooker() != null ? order.getBooker().getRealName() : "-";
    }

    public RoomStatusDashboardDTO getDashboardData() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        RoomStatusDashboardDTO dto = new RoomStatusDashboardDTO();

        // 获取所有订单
        List<RoomOrder> allOrders = orderRepository.findAll();

        // 有效订单 (排除已完成 status=3 和已取消 status=4)
        List<RoomOrder> activeOrders = allOrders.stream()
            .filter(o -> o.getStatus() != null && o.getStatus() != 3 && o.getStatus() != 4)
            .collect(Collectors.toList());

        // 维修中的房间
        List<RoomMaintenance> activeMaintenances = maintenanceRepository.findActiveMaintenancesInPeriod(now, now);

        // 所有未完成的清扫任务（包括过期的）
        List<CleaningTask> pendingTasks = cleaningTaskRepository.findByStatus(0);

        List<Room> allRooms = roomRepository.findAll();

        List<RoomStatusDashboardDTO.RoomDetailDTO> roomDetails = allRooms.stream().map(room -> {
            RoomStatusDashboardDTO.RoomDetailDTO detail = new RoomStatusDashboardDTO.RoomDetailDTO();
            detail.setRoomId(room.getId());
            detail.setRoomNo(room.getRoomNo());
            detail.setRoomTypeName(getRoomTypeNameZh(room.getRoomType()));

            if (room.getFloor() != null) {
                detail.setFloorId(room.getFloor().getId());
                detail.setFloorName(room.getFloor().getName());
            }

            // 标签（基于 arrivingDays/departingDays 在计算后追加）
            List<String> labels = new ArrayList<>();

            // 维护状态（维修+锁房合并）
            RoomMaintenance activeMaintenance = activeMaintenances.stream()
                .filter(m -> m.getRoom() != null && m.getRoom().getId().equals(room.getId()))
                .findFirst().orElse(null);

            // 当前订单：基于 RoomOccupy 状态判断，只有已入住房间才算"当前在住"
            RoomOrder currentOrder = activeOrders.stream()
                .filter(o -> o.getRoomOccupies() != null && o.getRoomOccupies().stream()
                    .anyMatch(ro -> ro.getRoom() != null && ro.getRoom().getId().equals(room.getId())
                        && ro.getStatus() != null && ro.getStatus() == RoomOccupy.STATUS_CHECKED_IN))
                .findFirst().orElse(null);

            // 该房间所有未完成的清扫任务（包括过期的）
            List<CleaningTask> roomPendingTasks = pendingTasks.stream()
                .filter(t -> t.getRoom() != null && t.getRoom().getId().equals(room.getId()))
                .collect(Collectors.toList());
            boolean hasPendingTask = !roomPendingTasks.isEmpty();

            // 状态判断
            if (activeMaintenance != null) {
                detail.setStatus(2); // 维护（含维修和锁房）
                detail.setMaintenanceType(activeMaintenance.getMaintenanceType() != null ? activeMaintenance.getMaintenanceType() : 1);
                detail.setMaintenanceContent(activeMaintenance.getContent());
                detail.setMaintenanceId(activeMaintenance.getId());
            } else if (currentOrder != null) {
                // 有当前订单
                detail.setGuestName(resolveGuestName(currentOrder, room.getId()));
                detail.setOrderId(currentOrder.getId());
                if (currentOrder.getPurpose() != null) {
                    detail.setPurposeId(currentOrder.getPurpose().getId());
                    detail.setPurposeName(currentOrder.getPurpose().getName());
                }

                // 判断住脏：已入住但有未完成的清扫任务
                if (hasPendingTask) {
                    detail.setStatus(5); // 住脏
                } else {
                    detail.setStatus(1); // 正常在住
                }
            } else {
                // 无当前订单
                // 判断空脏：已退房但有未完成的清扫任务
                if (hasPendingTask) {
                    detail.setStatus(4); // 空脏
                } else {
                    detail.setStatus(0); // 空闲
                }

                // 找最近的预订订单
                List<Integer> futureStatuses = Arrays.asList(0, 1);
                RoomOrder futureOrder = orderRepository.findByRoomIdAndStatusIn(room.getId(), futureStatuses).stream()
                    .filter(o -> o.getStartDate() != null && o.getStartDate().isAfter(now))
                    .min(Comparator.comparing(RoomOrder::getStartDate))
                    .orElse(null);
                if (futureOrder != null) {
                    detail.setOrderId(futureOrder.getId());
                    if (futureOrder.getPurpose() != null) {
                        detail.setPurposeId(futureOrder.getPurpose().getId());
                        detail.setPurposeName(futureOrder.getPurpose().getName());
                    }
                }
            }

            // 清扫任务信息（所有未完成任务，包括过期的）
            if (!roomPendingTasks.isEmpty()) {
                List<RoomStatusDashboardDTO.CleaningTaskInfo> taskInfos = roomPendingTasks.stream().map(t -> {
                    RoomStatusDashboardDTO.CleaningTaskInfo taskInfo = new RoomStatusDashboardDTO.CleaningTaskInfo();
                    taskInfo.setId(t.getId());
                    taskInfo.setTaskType(t.getTaskType());
                    taskInfo.setStatus(t.getStatus());
                    taskInfo.setContent(t.getContent());
                    taskInfo.setTaskDate(t.getTaskDate());
                    return taskInfo;
                }).collect(Collectors.toList());
                detail.setCleaningTasks(taskInfos);
            }

            // 最近抵达（基于 RoomOccupy 的 checkInTime：该房间待入住）
            RoomOccupy nearestArrivingOccupy = activeOrders.stream()
                .flatMap(o -> o.getRoomOccupies() != null ? o.getRoomOccupies().stream() : java.util.stream.Stream.empty())
                .filter(ro -> ro.getRoom() != null && ro.getRoom().getId().equals(room.getId())
                    && ro.getStatus() != null && ro.getStatus() == RoomOccupy.STATUS_PENDING
                    && ro.getCheckInTime() != null)
                .min(Comparator.comparing(RoomOccupy::getCheckInTime))
                .orElse(null);

            if (nearestArrivingOccupy != null) {
                RoomOrder nearestArriving = nearestArrivingOccupy.getOrder();
                RoomStatusDashboardDTO.OrderInfo orderInfo = new RoomStatusDashboardDTO.OrderInfo();
                orderInfo.setOrderId(nearestArriving.getId());
                orderInfo.setGuestName(resolveGuestName(nearestArriving, room.getId()));
                orderInfo.setRoomNo(room.getRoomNo());
                detail.setNearestArriving(orderInfo);

                int arrivingDays = (int) ChronoUnit.DAYS.between(today, nearestArrivingOccupy.getCheckInTime().toLocalDate());
                detail.setArrivingDays(arrivingDays);
            }

            // 最近离开（基于 RoomOccupy 的 checkOutTime：该房间已入住）
            RoomOccupy nearestDepartingOccupy = activeOrders.stream()
                .flatMap(o -> o.getRoomOccupies() != null ? o.getRoomOccupies().stream() : java.util.stream.Stream.empty())
                .filter(ro -> ro.getRoom() != null && ro.getRoom().getId().equals(room.getId())
                    && ro.getStatus() != null && ro.getStatus() == RoomOccupy.STATUS_CHECKED_IN
                    && ro.getCheckOutTime() != null)
                .min(Comparator.comparing(RoomOccupy::getCheckOutTime))
                .orElse(null);

            if (nearestDepartingOccupy != null) {
                RoomOrder nearestDeparting = nearestDepartingOccupy.getOrder();
                RoomStatusDashboardDTO.OrderInfo orderInfo = new RoomStatusDashboardDTO.OrderInfo();
                orderInfo.setOrderId(nearestDeparting.getId());
                orderInfo.setGuestName(resolveGuestName(nearestDeparting, room.getId()));
                orderInfo.setRoomNo(room.getRoomNo());
                detail.setNearestDeparting(orderInfo);

                int departingDays = (int) ChronoUnit.DAYS.between(today, nearestDepartingOccupy.getCheckOutTime().toLocalDate());
                detail.setDepartingDays(departingDays);
            }

            // 基于 arrivingDays/departingDays 设置标签
            if (detail.getArrivingDays() != null) {
                if (detail.getArrivingDays() < 0) labels.add("OVERDUE_ARRIVING");
                else if (detail.getArrivingDays() == 0) labels.add("ARRIVING_TODAY");
                else if (detail.getArrivingDays() <= 3) labels.add("ARRIVING_SOON");
            }
            if (detail.getDepartingDays() != null) {
                if (detail.getDepartingDays() < 0) labels.add("OVERDUE_DEPARTING");
                else if (detail.getDepartingDays() == 0) labels.add("DEPARTING_TODAY");
            }

            // 空闲房间用抵达订单补充 orderId
            if (detail.getOrderId() == null && detail.getNearestArriving() != null) {
                detail.setOrderId(detail.getNearestArriving().getOrderId());
            }

            detail.setLabels(labels);
            return detail;
        }).collect(Collectors.toList());

        // 抵达/离开统计（基于房间的 arrivingDays/departingDays）
        dto.setArrivingToday((int) roomDetails.stream().filter(r -> r.getArrivingDays() != null && r.getArrivingDays() == 0).count());
        dto.setDepartingToday((int) roomDetails.stream().filter(r -> r.getDepartingDays() != null && r.getDepartingDays() == 0).count());
        dto.setArrivingInNDays((int) roomDetails.stream().filter(r -> r.getArrivingDays() != null && r.getArrivingDays() > 0 && r.getArrivingDays() <= 3).count());

        // 状态计数
        Map<String, Long> counts = new HashMap<>();
        counts.put("FREE", roomDetails.stream().filter(r -> r.getStatus() == 0).count());
        counts.put("OCCUPIED", roomDetails.stream().filter(r -> r.getStatus() == 1).count());
        counts.put("MAINTENANCE", roomDetails.stream().filter(r -> r.getStatus() == 2).count());
        counts.put("MAINT_REPAIR", roomDetails.stream().filter(r -> r.getStatus() == 2 && r.getMaintenanceType() != null && r.getMaintenanceType() == 1).count());
        counts.put("MAINT_LOCKED", roomDetails.stream().filter(r -> r.getStatus() == 2 && r.getMaintenanceType() != null && r.getMaintenanceType() == 2).count());
        counts.put("EMPTY_DIRTY", roomDetails.stream().filter(r -> r.getStatus() == 4).count());
        counts.put("OCCUPIED_DIRTY", roomDetails.stream().filter(r -> r.getStatus() == 5).count());
        counts.put("OVERDUE_ARRIVING", roomDetails.stream().filter(r -> r.getLabels() != null && r.getLabels().contains("OVERDUE_ARRIVING")).count());
        counts.put("OVERDUE_DEPARTING", roomDetails.stream().filter(r -> r.getLabels() != null && r.getLabels().contains("OVERDUE_DEPARTING")).count());
        dto.setStatusCounts(counts);

        // 事由计数
        Map<String, Long> purposeCounts = new HashMap<>();
        for (RoomStatusDashboardDTO.RoomDetailDTO detail : roomDetails) {
            String pName = detail.getPurposeName();
            if (pName != null) {
                purposeCounts.merge(pName, 1L, Long::sum);
            }
        }
        dto.setPurposeCounts(purposeCounts);

        // 按抵达天数聚合（基于房间的 arrivingDays 字段，含逾期负数天）
        Map<Integer, Integer> arrivingByDays = new HashMap<>();
        for (RoomStatusDashboardDTO.RoomDetailDTO detail : roomDetails) {
            if (detail.getArrivingDays() != null && detail.getArrivingDays() >= -30 && detail.getArrivingDays() <= 30) {
                arrivingByDays.merge(detail.getArrivingDays(), 1, Integer::sum);
            }
        }
        dto.setArrivingByDays(arrivingByDays);

        // 按离开天数聚合（基于房间的 departingDays 字段，含逾期负数天）
        Map<Integer, Integer> departingByDays = new HashMap<>();
        for (RoomStatusDashboardDTO.RoomDetailDTO detail : roomDetails) {
            if (detail.getDepartingDays() != null && detail.getDepartingDays() >= -30 && detail.getDepartingDays() <= 30) {
                departingByDays.merge(detail.getDepartingDays(), 1, Integer::sum);
            }
        }
        dto.setDepartingByDays(departingByDays);

        dto.setRooms(roomDetails);
        return dto;
    }
}
