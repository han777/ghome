package com.apartment.service;

import com.apartment.dto.RoomStatusDashboardDTO;
import com.apartment.entity.Room;
import com.apartment.entity.RoomOrder;
import com.apartment.repository.RoomOrderRepository;
import com.apartment.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String getRoomTypeNameZh(com.apartment.entity.RoomType roomType) {
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

    public RoomStatusDashboardDTO getDashboardData() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        RoomStatusDashboardDTO dto = new RoomStatusDashboardDTO();

        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        List<RoomOrder> arrivingToday = orderRepository.findByStartDateBetween(startOfDay, endOfDay);
        List<RoomOrder> departingToday = orderRepository.findByEndDateBetween(startOfDay, endOfDay);
        List<RoomOrder> arrivingSoon = orderRepository.findByStartDateBetween(today.plusDays(1).atStartOfDay(), today.plusDays(3).atTime(23, 59, 59));
        List<RoomOrder> activeOrders = orderRepository.findAll().stream()
            .filter(o -> Arrays.asList(1, 2).contains(o.getStatus()) && !o.getStartDate().isAfter(now) && !o.getEndDate().isBefore(now))
            .collect(Collectors.toList());

        List<com.apartment.entity.RoomMaintenance> activeMaintenances = maintenanceRepository.findActiveMaintenancesInPeriod(now, now);

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
            detail.setLabels(labels);

            // Determine status
            boolean isMaintenance = activeMaintenances.stream().anyMatch(m -> m.getRoom() != null && m.getRoom().getId().equals(room.getId()));
            RoomOrder currentOrder = activeOrders.stream().filter(o -> o.getRoomOccupies() != null && o.getRoomOccupies().stream().anyMatch(ro -> ro.getRoom().getId().equals(room.getId()))).findFirst().orElse(null);

            if (isMaintenance) {
                detail.setStatus(3); // 3: Maintenance
            } else if (room.getStatus() != null && room.getStatus() == 1) {
                detail.setStatus(2); // 2: Locked
            } else if (currentOrder != null) {
                detail.setStatus(1); // 1: Occupied
                detail.setGuestName(currentOrder.getBooker() != null ? currentOrder.getBooker().getRealName() : "-");
            } else {
                detail.setStatus(0); // 0: Available
            }

            return detail;
        }).collect(Collectors.toList());

        Map<String, Long> counts = new HashMap<>();
        counts.put("FREE", roomDetails.stream().filter(r -> r.getStatus() == 0).count());
        counts.put("OCCUPIED", roomDetails.stream().filter(r -> r.getStatus() == 1).count());
        counts.put("LOCKED", roomDetails.stream().filter(r -> r.getStatus() == 2).count());
        counts.put("REPAIR", roomDetails.stream().filter(r -> r.getStatus() == 3).count());
        dto.setStatusCounts(counts);

        dto.setRooms(roomDetails);
        return dto;
    }
}
