package com.apartment.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class RoomStatusDashboardDTO {
    private long arrivingToday;
    private long departingToday;
    private long arrivingInNDays; // N=3 for example
    private Map<String, Long> statusCounts; // FREE, OCCUPIED, REPAIR
    private List<RoomDetailDTO> rooms;

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
    }
}
