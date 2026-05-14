package com.apartment.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class RoomTypeForecastDTO {
    private String startDate;
    private String endDate;
    private List<String> dates;           // date strings in window
    private List<RoomTypeRow> rows;
    private TotalRow total;

    @Data
    public static class RoomTypeRow {
        private Long roomTypeId;
        private String roomTypeName;
        private int totalRooms;            // total available rooms of this type
        private Map<String, Integer> bookedCount;   // date -> booked room count
        private Map<String, Integer> maintenanceCount; // date -> maintenance room count
        private Map<String, Integer> availableCount;   // date -> available = total - booked - maintenance
    }

    @Data
    public static class TotalRow {
        private int totalRooms;            // sum of all room types
        private Map<String, Integer> bookedCount;
        private Map<String, Integer> maintenanceCount;
        private Map<String, Integer> availableCount;
    }
}