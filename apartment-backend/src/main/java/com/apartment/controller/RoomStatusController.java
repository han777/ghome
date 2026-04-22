package com.apartment.controller;

import com.apartment.dto.RoomStatusDashboardDTO;
import com.apartment.service.RoomStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class RoomStatusController {

    @Autowired
    private RoomStatusService roomStatusService;

    @GetMapping("/room-status")
    public RoomStatusDashboardDTO getRoomStatus() {
        return roomStatusService.getDashboardData();
    }
}
