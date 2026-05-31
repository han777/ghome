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

        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        // 获取所有订单
        List<RoomOrder> allOrders = orderRepository.findAll();

        // 待确认订单 (status=1) - 用于按抵达聚合
        List<RoomOrder> pendingOrders = allOrders.stream()
            .filter(o -> o.getStatus() != null && o.getStatus() == 1)
            .collect(Collectors.toList());

        // 已入住订单 (status=2) - 纯按订单状态判断，不用时间范围
        List<RoomOrder> activeOrders = allOrders.stream()
            .filter(o -> o.getStatus() != null && o.getStatus() == 2)
            .collect(Collectors.toList());

        // 今日抵达/离开（仅统计未入住/未退房的订单）
        List<RoomOrder> arrivingToday = allOrders.stream()
            .filter(o -> o.getStatus() != null && (o.getStatus() == 0 || o.getStatus() == 1))
            .filter(o -> o.getStartDate() != null
                && !o.getStartDate().isBefore(startOfDay)
                && !o.getStartDate().isAfter(endOfDay))
            .collect(Collectors.toList());

        List<RoomOrder> departingToday = allOrders.stream()
            .filter(o -> o.getStatus() != null && (o.getStatus() == 1 || o.getStatus() == 2))
            .filter(o -> o.getEndDate() != null
                && !o.getEndDate().isBefore(startOfDay)
                && !o.getEndDate().isAfter(endOfDay))
            .collect(Collectors.toList());

        // 未来3日抵达（仅统计未入住订单）
        List<RoomOrder> arrivingSoon = allOrders.stream()
            .filter(o -> o.getStatus() != null && (o.getStatus() == 0 || o.getStatus() == 1))
            .filter(o -> o.getStartDate() != null
                && !o.getStartDate().isBefore(today.plusDays(1).atStartOfDay())
                && !o.getStartDate().isAfter(today.plusDays(3).atTime(23, 59, 59)))
            .collect(Collectors.toList());

        // 维修中的房间
        List<RoomMaintenance> activeMaintenances = maintenanceRepository.findActiveMaintenancesInPeriod(now, now);

        // 今日清扫任务
        List<CleaningTask> todayTasks = cleaningTaskRepository.findByTaskDate(today);

        dto.setArrivingToday(arrivingToday.size());
        dto.setDepartingToday(departingToday.size());
        dto.setArrivingInNDays(arrivingSoon.size());

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

            // 标签（先设基础标签，逾期标签在计算天数后追加）
            List<String> labels = new ArrayList<>();
            if (arrivingToday.stream().anyMatch(o -> o.getRoomOccupies() != null && o.getRoomOccupies().stream().anyMatch(ro -> ro.getRoom().getId().equals(room.getId())))) {
                labels.add("ARRIVING_TODAY");
            }
            if (departingToday.stream().anyMatch(o -> o.getRoomOccupies() != null && o.getRoomOccupies().stream().anyMatch(ro -> ro.getRoom().getId().equals(room.getId())))) {
                labels.add("DEPARTING_TODAY");
            }
            if (arrivingSoon.stream().anyMatch(o -> o.getRoomOccupies() != null && o.getRoomOccupies().stream().anyMatch(ro -> ro.getRoom().getId().equals(room.getId())))) {
                labels.add("ARRIVING_SOON");
            }

            // 维护状态（维修+锁房合并）
            RoomMaintenance activeMaintenance = activeMaintenances.stream()
                .filter(m -> m.getRoom() != null && m.getRoom().getId().equals(room.getId()))
                .findFirst().orElse(null);

            // 当前订单
            RoomOrder currentOrder = activeOrders.stream()
                .filter(o -> o.getRoomOccupies() != null && o.getRoomOccupies().stream().anyMatch(ro -> ro.getRoom().getId().equals(room.getId())))
                .findFirst().orElse(null);

            // 今日清扫任务
            CleaningTask todayTask = todayTasks.stream()
                .filter(t -> t.getRoom() != null && t.getRoom().getId().equals(room.getId()))
                .findFirst().orElse(null);

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

                // 判断住脏：已入住但有未完成的清扫任务
                if (todayTask != null && todayTask.getStatus() == 0) {
                    detail.setStatus(5); // 住脏
                } else {
                    detail.setStatus(1); // 正常在住
                }
            } else {
                // 无当前订单
                // 判断空脏：已退房但有未完成的清扫任务
                if (todayTask != null && todayTask.getStatus() == 0) {
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
                }
            }

            // 清扫任务信息 - 不返回已完成的任务，返回计划中和已取消的
            if (todayTask != null && todayTask.getStatus() == 0) {
                RoomStatusDashboardDTO.CleaningTaskInfo taskInfo = new RoomStatusDashboardDTO.CleaningTaskInfo();
                taskInfo.setId(todayTask.getId());
                taskInfo.setTaskType(todayTask.getTaskType());
                taskInfo.setStatus(todayTask.getStatus());
                taskInfo.setContent(todayTask.getContent());
                detail.setCleaningTask(taskInfo);
            }

            // 最近到达订单信息（未抵达）
            // 待确认订单(status=1): 无论时间早晚，都视为未抵达
            // 已入住订单(status=2): 视为已抵达，不在此筛选
            RoomOrder nearestArriving = allOrders.stream()
                .filter(o -> o.getStatus() != null && o.getStatus() == 1)
                .filter(o -> o.getRoomOccupies() != null && o.getRoomOccupies().stream().anyMatch(ro -> ro.getRoom().getId().equals(room.getId())))
                .filter(o -> o.getStartDate() != null)
                .min(Comparator.comparing(RoomOrder::getStartDate))
                .orElse(null);

            if (nearestArriving != null) {
                RoomStatusDashboardDTO.OrderInfo orderInfo = new RoomStatusDashboardDTO.OrderInfo();
                orderInfo.setOrderId(nearestArriving.getId());
                orderInfo.setGuestName(resolveGuestName(nearestArriving, room.getId()));
                orderInfo.setRoomNo(room.getRoomNo());
                detail.setNearestArriving(orderInfo);

                int arrivingDays = (int) ChronoUnit.DAYS.between(today, nearestArriving.getStartDate().toLocalDate());
                detail.setArrivingDays(arrivingDays);
                if (arrivingDays < 0) {
                    labels.add("OVERDUE_ARRIVING");
                }
            }

            // 最近离开订单信息（已抵达）
            // 已入住订单(status=2): 无论时间早晚，都视为已抵达，按离开时间计算
            RoomOrder nearestDeparting = allOrders.stream()
                .filter(o -> o.getStatus() != null && o.getStatus() == 2)
                .filter(o -> o.getRoomOccupies() != null && o.getRoomOccupies().stream().anyMatch(ro -> ro.getRoom().getId().equals(room.getId())))
                .filter(o -> o.getEndDate() != null)
                .min(Comparator.comparing(RoomOrder::getEndDate))
                .orElse(null);

            if (nearestDeparting != null) {
                RoomStatusDashboardDTO.OrderInfo orderInfo = new RoomStatusDashboardDTO.OrderInfo();
                orderInfo.setOrderId(nearestDeparting.getId());
                orderInfo.setGuestName(resolveGuestName(nearestDeparting, room.getId()));
                orderInfo.setRoomNo(room.getRoomNo());
                detail.setNearestDeparting(orderInfo);

                int departingDays = (int) ChronoUnit.DAYS.between(today, nearestDeparting.getEndDate().toLocalDate());
                detail.setDepartingDays(departingDays);
                if (departingDays < 0) {
                    labels.add("OVERDUE_DEPARTING");
                }
            }

            // 确保 ARRIVING_TODAY 房间有 orderId
            if (detail.getOrderId() == null && labels.contains("ARRIVING_TODAY")) {
                RoomOrder arrivingOrder = arrivingToday.stream()
                    .filter(o -> o.getRoomOccupies() != null && o.getRoomOccupies().stream().anyMatch(ro -> ro.getRoom().getId().equals(room.getId())))
                    .findFirst().orElse(null);
                if (arrivingOrder != null) {
                    detail.setOrderId(arrivingOrder.getId());
                }
            }

            detail.setLabels(labels);
            return detail;
        }).collect(Collectors.toList());

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

        // 按抵达天数聚合（基于房间的 arrivingDays 字段，含逾期负数天）
        Map<Integer, Integer> arrivingByDays = new HashMap<>();
        for (RoomStatusDashboardDTO.RoomDetailDTO detail : roomDetails) {
            if (detail.getArrivingDays() != null && detail.getArrivingDays() >= -7 && detail.getArrivingDays() <= 30) {
                arrivingByDays.merge(detail.getArrivingDays(), 1, Integer::sum);
            }
        }
        dto.setArrivingByDays(arrivingByDays);

        // 按离开天数聚合（基于房间的 departingDays 字段，含逾期负数天）
        Map<Integer, Integer> departingByDays = new HashMap<>();
        for (RoomStatusDashboardDTO.RoomDetailDTO detail : roomDetails) {
            if (detail.getDepartingDays() != null && detail.getDepartingDays() >= -7 && detail.getDepartingDays() <= 30) {
                departingByDays.merge(detail.getDepartingDays(), 1, Integer::sum);
            }
        }
        dto.setDepartingByDays(departingByDays);

        dto.setRooms(roomDetails);
        return dto;
    }
}
