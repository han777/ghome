package com.apartment.controller;

import com.apartment.dto.RoomFeeDetailDTO;
import com.apartment.entity.*;
import com.apartment.repository.RoomOccupyRepository;
import com.apartment.repository.RoomOrderRepository;
import com.apartment.repository.RoomRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/room-fee-detail")
public class RoomFeeDetailController {

    @Autowired
    private RoomOccupyRepository occupyRepository;
    @Autowired
    private RoomOrderRepository orderRepository;
    @Autowired
    private RoomRepository roomRepository;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private RoomFeeDetailDTO toDTO(RoomOccupy occupy) {
        RoomFeeDetailDTO dto = new RoomFeeDetailDTO();
        dto.setId(occupy.getId());

        // Order info
        RoomOrder order = occupy.getOrder();
        dto.setOrderNo(order != null ? order.getOrderNo() : "-");

        // Room info
        Room room = occupy.getRoom();
        if (room != null) {
            dto.setRoomNo(room.getRoomNo());
            // Fetch fresh room to ensure roomType is loaded
            Room freshRoom = roomRepository.findById(room.getId()).orElse(room);
            if (freshRoom.getRoomType() != null) {
                dto.setRoomTypeName(freshRoom.getRoomType().getTypeCode());
            } else {
                dto.setRoomTypeName("-");
            }
        } else {
            dto.setRoomNo("-");
            dto.setRoomTypeName("-");
        }

        // Time info: use occupy-level checkInTime/checkOutTime, fallback to order startDate/endDate
        LocalDateTime checkIn = occupy.getCheckInTime();
        LocalDateTime checkOut = occupy.getCheckOutTime();
        if (checkIn == null && order != null) checkIn = order.getStartDate();
        if (checkOut == null && order != null) checkOut = order.getEndDate();
        dto.setCheckInTime(checkIn);
        dto.setCheckOutTime(checkOut);

        // Days: use quantity if set, otherwise calculate
        int days = occupy.getQuantity() != null ? occupy.getQuantity() : 1;
        if (checkIn != null && checkOut != null) {
            long calcDays = java.time.temporal.ChronoUnit.DAYS.between(checkIn.toLocalDate(), checkOut.toLocalDate());
            if (calcDays > 0) days = (int) calcDays;
            if (calcDays <= 0) days = 1;
        }
        dto.setDays(days);

        // Price
        BigDecimal unitPrice = occupy.getActualPrice() != null ? occupy.getActualPrice() : BigDecimal.ZERO;
        dto.setUnitPrice(unitPrice);
        dto.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(days)));

        return dto;
    }

    @GetMapping("/paged")
    public Page<RoomFeeDetailDTO> getPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkInFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkInTo) {

        List<RoomOccupy> allOccupies = occupyRepository.findAll();

        // Filter by check-in time range
        List<RoomOccupy> filtered = allOccupies;
        if (checkInFrom != null || checkInTo != null) {
            filtered = new ArrayList<>();
            for (RoomOccupy o : allOccupies) {
                LocalDateTime ci = o.getCheckInTime();
                if (ci == null && o.getOrder() != null) ci = o.getOrder().getStartDate();
                if (ci == null) continue;
                if (checkInFrom != null && ci.isBefore(checkInFrom)) continue;
                if (checkInTo != null && ci.isAfter(checkInTo)) continue;
                filtered.add(o);
            }
        }

        // Sort by checkInTime descending
        filtered.sort((a, b) -> {
            LocalDateTime aTime = a.getCheckInTime();
            LocalDateTime bTime = b.getCheckInTime();
            if (aTime == null) aTime = a.getOrder() != null ? a.getOrder().getStartDate() : LocalDateTime.MIN;
            if (bTime == null) bTime = b.getOrder() != null ? b.getOrder().getStartDate() : LocalDateTime.MIN;
            return bTime.compareTo(aTime); // newest first
        });

        // Pagination
        int total = filtered.size();
        int start = Math.min(page * size, total);
        int end = Math.min(start + size, total);
        List<RoomFeeDetailDTO> pageContent = new ArrayList<>();
        for (int i = start; i < end; i++) {
            pageContent.add(toDTO(filtered.get(i)));
        }

        return new PageImpl<>(pageContent, PageRequest.of(page, size), total);
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkInFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkInTo) {

        // Get all filtered data (no pagination)
        List<RoomOccupy> allOccupies = occupyRepository.findAll();
        List<RoomOccupy> filtered = allOccupies;
        if (checkInFrom != null || checkInTo != null) {
            filtered = new ArrayList<>();
            for (RoomOccupy o : allOccupies) {
                LocalDateTime ci = o.getCheckInTime();
                if (ci == null && o.getOrder() != null) ci = o.getOrder().getStartDate();
                if (ci == null) continue;
                if (checkInFrom != null && ci.isBefore(checkInFrom)) continue;
                if (checkInTo != null && ci.isAfter(checkInTo)) continue;
                filtered.add(o);
            }
        }

        // Sort
        filtered.sort((a, b) -> {
            LocalDateTime aTime = a.getCheckInTime();
            LocalDateTime bTime = b.getCheckInTime();
            if (aTime == null) aTime = a.getOrder() != null ? a.getOrder().getStartDate() : LocalDateTime.MIN;
            if (bTime == null) bTime = b.getOrder() != null ? b.getOrder().getStartDate() : LocalDateTime.MIN;
            return bTime.compareTo(aTime);
        });

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Room Fee Detail");

            // Header
            Row header = sheet.createRow(0);
            String[] headers = {"#", "Order No", "Room No", "Room Type", "Check-in Time", "Check-out Time", "Days", "Unit Price", "Total Price"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                CellStyle bold = workbook.createCellStyle();
                Font bf = workbook.createFont();
                bf.setBold(true);
                bold.setFont(bf);
                cell.setCellStyle(bold);
            }

            // Data rows
            int rowNum = 1;
            for (int idx = 0; idx < filtered.size(); idx++) {
                RoomFeeDetailDTO dto = toDTO(filtered.get(idx));
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(idx + 1); // row number
                row.createCell(1).setCellValue(dto.getOrderNo());
                row.createCell(2).setCellValue(dto.getRoomNo());
                row.createCell(3).setCellValue(dto.getRoomTypeName());
                row.createCell(4).setCellValue(dto.getCheckInTime() != null ? dto.getCheckInTime().format(DT_FMT) : "");
                row.createCell(5).setCellValue(dto.getCheckOutTime() != null ? dto.getCheckOutTime().format(DT_FMT) : "");
                row.createCell(6).setCellValue(dto.getDays());
                row.createCell(7).setCellValue(dto.getUnitPrice() != null ? dto.getUnitPrice().doubleValue() : 0);
                row.createCell(8).setCellValue(dto.getTotalPrice() != null ? dto.getTotalPrice().doubleValue() : 0);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"room_fee_detail.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel: " + e.getMessage(), e);
        }
    }
}