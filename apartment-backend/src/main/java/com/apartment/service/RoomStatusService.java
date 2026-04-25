package com.apartment.service;

import com.apartment.dto.RoomStatusDashboardDTO;
import com.apartment.entity.Room;
import com.apartment.entity.RoomOrder;
import com.apartment.repository.RoomOrderRepository;
import com.apartment.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoomStatusService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomOrderRepository orderRepository;

    public RoomStatusDashboardDTO getDashboardData() {
        LocalDate today = LocalDate.now();
        RoomStatusDashboardDTO dto = new RoomStatusDashboardDTO();

        List<RoomOrder> arrivingToday = orderRepository.findByStartDate(today);
        List<RoomOrder> departingToday = orderRepository.findByEndDate(today);
        List<RoomOrder> arrivingSoon = orderRepository.findByStartDateBetween(today.plusDays(1), today.plusDays(3));

        dto.setArrivingToday(arrivingToday.size());
        dto.setDepartingToday(departingToday.size());
        dto.setArrivingInNDays(arrivingSoon.size());

        List<Room> allRooms = roomRepository.findAll();
        Map<String, Long> counts = new HashMap<>();
        counts.put("FREE", allRooms.stream().filter(r -> r.getStatus() == 0).count());
        counts.put("OCCUPIED", allRooms.stream().filter(r -> r.getStatus() == 1).count());
        counts.put("REPAIR", allRooms.stream().filter(r -> r.getStatus() == 2).count());
        dto.setStatusCounts(counts);

        List<RoomStatusDashboardDTO.RoomDetailDTO> roomDetails = allRooms.stream().map(room -> {
            RoomStatusDashboardDTO.RoomDetailDTO detail = new RoomStatusDashboardDTO.RoomDetailDTO();
            detail.setRoomId(room.getId());
            detail.setRoomNo(room.getRoomNo());
            detail.setStatus(room.getStatus());
            detail.setRoomTypeName(room.getRoomType() != null ? room.getRoomType().getTypeCode() : "N/A");

            // Set labels
            if (arrivingToday.stream().anyMatch(o -> o.getRoom() != null && o.getRoom().getId().equals(room.getId()))) {
                detail.setLabel("ARRIVING_TODAY");
            } else if (departingToday.stream().anyMatch(o -> o.getRoom() != null && o.getRoom().getId().equals(room.getId()))) {
                detail.setLabel("DEPARTING_TODAY");
            } else if (arrivingSoon.stream().anyMatch(o -> o.getRoom() != null && o.getRoom().getId().equals(room.getId()))) {
                detail.setLabel("ARRIVING_SOON");
            }

            return detail;
        }).collect(Collectors.toList());

        dto.setRooms(roomDetails);
        return dto;
    }
}
