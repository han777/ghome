package com.apartment.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class RoomStatusDashboardDTO {
    private long arrivingToday;
    private long departingToday;
    private long arrivingInNDays;
    private Map<String, Long> statusCounts;
    private List<RoomDetailDTO> rooms;

    // 按抵达天数聚合 (key: 天数0=今天, value: 订单数)
    private Map<Integer, Integer> arrivingByDays;
    // 按离开天数聚合 (key: 天数0=今天, value: 订单数)
    private Map<Integer, Integer> departingByDays;

    @Data
    public static class RoomDetailDTO {
        private Long roomId;
        private String roomNo;
        private Integer status;
        private String roomTypeName;
        private Long floorId;
        private String floorName;
        private List<String> labels;
        private String guestName;
        private Long orderId;

        // 维护类型 (1=维修, 2=锁房), 仅当 status=2(维护) 时有值
        private Integer maintenanceType;
        private String maintenanceContent;
        private Long maintenanceId;

        // 清扫任务信息
        private CleaningTaskInfo cleaningTask;

        // 最近到达订单信息
        private OrderInfo nearestArriving;
        private Integer arrivingDays;  // 几日达 (0=今日达)

        // 最近离开订单信息
        private OrderInfo nearestDeparting;
        private Integer departingDays;  // 几日离 (0=今日离)
    }

    @Data
    public static class CleaningTaskInfo {
        private Long id;
        private Integer taskType;  // 1=日常保洁, 2=强打扫
        private Integer status;    // 0=计划, 1=取消, 2=完成
        private String content;
    }

    @Data
    public static class OrderInfo {
        private Long orderId;
        private String guestName;
        private String roomNo;
    }
}
