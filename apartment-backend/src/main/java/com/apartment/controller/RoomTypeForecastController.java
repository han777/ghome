package com.apartment.controller;

import com.apartment.dto.RoomTypeForecastDTO;
import com.apartment.entity.Room;
import com.apartment.entity.RoomMaintenance;
import com.apartment.entity.RoomType;
import com.apartment.repository.RoomMaintenanceRepository;
import com.apartment.repository.RoomOrderRepository;
import com.apartment.repository.RoomRepository;
import com.apartment.repository.RoomTypeRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/forecast")
public class RoomTypeForecastController {

    @Autowired
    private RoomTypeRepository roomTypeRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private RoomOrderRepository orderRepository;
    @Autowired
    private RoomMaintenanceRepository maintenanceRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int MAX_DAYS = 30;

    @GetMapping("/room-type")
    public RoomTypeForecastDTO getForecast(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        LocalDate today = LocalDate.now();
        if (start == null) start = today;
        if (end == null) end = today.plusDays(9);
        if (end.isBefore(start)) end = start;
        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(start, end);
        if (daysDiff > MAX_DAYS - 1) {
            end = start.plusDays(MAX_DAYS - 1);
        }
        final LocalDate finalStart = start;
        final LocalDate finalEnd = end;

        List<String> dates = new ArrayList<>();
        for (LocalDate d = finalStart; !d.isAfter(finalEnd); d = d.plusDays(1)) {
            dates.add(d.format(DATE_FMT));
        }

        List<RoomType> roomTypes = roomTypeRepository.findAll();
        List<Room> allRooms = roomRepository.findAll();

        // Group rooms by roomTypeId
        Map<Long, List<Room>> roomsByType = allRooms.stream()
                .filter(r -> r.getRoomType() != null)
                .collect(Collectors.groupingBy(r -> r.getRoomType().getId()));

        // Map roomId -> roomTypeId
        Map<Long, Long> roomTypeMap = allRooms.stream()
                .filter(r -> r.getRoomType() != null)
                .collect(Collectors.toMap(Room::getId, r -> r.getRoomType().getId()));

        // Get all active maintenances in the period
        LocalDateTime startDT = finalStart.atStartOfDay();
        LocalDateTime endDT = finalEnd.plusDays(1).atStartOfDay();
        List<RoomMaintenance> maintenances = maintenanceRepository.findActiveMaintenancesInPeriod(startDT, endDT);

        // Map roomId -> set of dates under maintenance
        Map<Long, Set<String>> maintenanceByRoom = new HashMap<>();
        for (RoomMaintenance m : maintenances) {
            if (m.getRoom() == null) continue;
            Set<String> mDates = maintenanceByRoom.computeIfAbsent(m.getRoom().getId(), k -> new HashSet<>());
            LocalDate mStart = m.getStartTime().toLocalDate();
            LocalDate mEnd = m.getEndTime().toLocalDate();
            for (LocalDate d = mStart; !d.isAfter(mEnd); d = d.plusDays(1)) {
                String ds = d.format(DATE_FMT);
                if (dates.contains(ds)) {
                    mDates.add(ds);
                }
            }
        }

        // Get all active orders (status 0, 1, 2) in the period
        List<com.apartment.entity.RoomOrder> activeOrders = orderRepository.findByBookerIdAndStatusIn(
                null, Arrays.asList(0, 1, 2)); // We need all, not just by booker

        // Actually we need ALL active orders, not filtered by bookerId
        // Let's use a different approach: find orders by status
        List<com.apartment.entity.RoomOrder> allActiveOrders = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() != null && (o.getStatus() == 0 || o.getStatus() == 1 || o.getStatus() == 2))
                .filter(o -> o.getStartDate() != null && o.getEndDate() != null)
                .filter(o -> o.getStartDate().toLocalDate().isBefore(finalEnd.plusDays(1)) &&
                             o.getEndDate().toLocalDate().isAfter(finalStart.minusDays(1)))
                .collect(Collectors.toList());

        // Map roomId -> set of dates booked
        Map<Long, Set<String>> bookedByRoom = new HashMap<>();
        for (com.apartment.entity.RoomOrder order : allActiveOrders) {
            if (order.getRoomOccupies() == null) continue;
            for (com.apartment.entity.RoomOccupy occupy : order.getRoomOccupies()) {
                if (occupy.getRoom() == null) continue;
                Set<String> bDates = bookedByRoom.computeIfAbsent(occupy.getRoom().getId(), k -> new HashSet<>());
                LocalDate oStart = order.getStartDate().toLocalDate();
                LocalDate oEnd = order.getEndDate().toLocalDate();
                for (LocalDate d = oStart; !d.isBefore(oEnd); d = d.plusDays(1)) {
                    String ds = d.format(DATE_FMT);
                    if (dates.contains(ds)) {
                        bDates.add(ds);
                    }
                }
            }
        }

        // Build rows per room type
        List<RoomTypeForecastDTO.RoomTypeRow> rows = new ArrayList<>();
        int grandTotalRooms = 0;
        Map<String, Integer> totalBooked = new LinkedHashMap<>();
        Map<String, Integer> totalMaintenance = new LinkedHashMap<>();
        Map<String, Integer> totalAvailable = new LinkedHashMap<>();

        for (String d : dates) {
            totalBooked.put(d, 0);
            totalMaintenance.put(d, 0);
            totalAvailable.put(d, 0);
        }

        for (RoomType rt : roomTypes) {
            RoomTypeForecastDTO.RoomTypeRow row = new RoomTypeForecastDTO.RoomTypeRow();
            row.setRoomTypeId(rt.getId());
            row.setRoomTypeName(rt.getTypeCode());

            List<Room> typeRooms = roomsByType.getOrDefault(rt.getId(), List.of());
            int totalTypeRooms = typeRooms.size();
            row.setTotalRooms(totalTypeRooms);
            grandTotalRooms += totalTypeRooms;

            Map<String, Integer> bookedCount = new LinkedHashMap<>();
            Map<String, Integer> maintenanceCount = new LinkedHashMap<>();
            Map<String, Integer> availableCount = new LinkedHashMap<>();

            for (String d : dates) {
                int booked = 0;
                int maintenance = 0;
                for (Room r : typeRooms) {
                    if (bookedByRoom.containsKey(r.getId()) && bookedByRoom.get(r.getId()).contains(d)) {
                        booked++;
                    }
                    if (maintenanceByRoom.containsKey(r.getId()) && maintenanceByRoom.get(r.getId()).contains(d)) {
                        maintenance++;
                    }
                }
                bookedCount.put(d, booked);
                maintenanceCount.put(d, maintenance);
                availableCount.put(d, totalTypeRooms - booked - maintenance);

                totalBooked.merge(d, booked, Integer::sum);
                totalMaintenance.merge(d, maintenance, Integer::sum);
                totalAvailable.merge(d, totalTypeRooms - booked - maintenance, Integer::sum);
            }

            row.setBookedCount(bookedCount);
            row.setMaintenanceCount(maintenanceCount);
            row.setAvailableCount(availableCount);
            rows.add(row);
        }

        RoomTypeForecastDTO.TotalRow totalRow = new RoomTypeForecastDTO.TotalRow();
        totalRow.setTotalRooms(grandTotalRooms);
        totalRow.setBookedCount(totalBooked);
        totalRow.setMaintenanceCount(totalMaintenance);
        totalRow.setAvailableCount(totalAvailable);

        RoomTypeForecastDTO dto = new RoomTypeForecastDTO();
        dto.setStartDate(finalStart.format(DATE_FMT));
        dto.setEndDate(finalEnd.format(DATE_FMT));
        dto.setDates(dates);
        dto.setRows(rows);
        dto.setTotal(totalRow);
        return dto;
    }

    @GetMapping("/room-type/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        RoomTypeForecastDTO data = getForecast(start, end);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Room Type Forecast");

            // Header row
            Row headerRow = sheet.createRow(0);
            int col = 0;
            headerRow.createCell(col++).setCellValue("Room Type");
            headerRow.createCell(col++).setCellValue("Total Rooms");
            for (String d : data.getDates()) {
                headerRow.createCell(col++).setCellValue(d + " Booked");
                headerRow.createCell(col++).setCellValue(d + " Maint.");
                headerRow.createCell(col++).setCellValue(d + " Available");
            }

            // Data rows
            int rowNum = 1;
            CellStyle numStyle = workbook.createCellStyle();
            DataFormat fmt = workbook.createDataFormat();
            numStyle.setDataFormat(fmt.getFormat("0"));

            for (RoomTypeForecastDTO.RoomTypeRow row : data.getRows()) {
                Row excelRow = sheet.createRow(rowNum++);
                col = 0;
                excelRow.createCell(col++).setCellValue(row.getRoomTypeName());
                excelRow.createCell(col++).setCellValue(row.getTotalRooms());
                for (String d : data.getDates()) {
                    excelRow.createCell(col++).setCellValue(row.getBookedCount().getOrDefault(d, 0));
                    excelRow.createCell(col++).setCellValue(row.getMaintenanceCount().getOrDefault(d, 0));
                    excelRow.createCell(col++).setCellValue(row.getAvailableCount().getOrDefault(d, 0));
                }
            }

            // Total row
            Row totalExcelRow = sheet.createRow(rowNum);
            col = 0;
            CellStyle boldStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldStyle.setFont(boldFont);
            Cell totalLabelCell = totalExcelRow.createCell(col);
            totalLabelCell.setCellValue("Total");
            totalLabelCell.setCellStyle(boldStyle);
            col++;
            totalExcelRow.createCell(col++).setCellValue(data.getTotal().getTotalRooms());
            for (String d : data.getDates()) {
                totalExcelRow.createCell(col++).setCellValue(data.getTotal().getBookedCount().getOrDefault(d, 0));
                totalExcelRow.createCell(col++).setCellValue(data.getTotal().getMaintenanceCount().getOrDefault(d, 0));
                totalExcelRow.createCell(col++).setCellValue(data.getTotal().getAvailableCount().getOrDefault(d, 0));
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            String filename = "room_type_forecast_" + data.getStartDate() + "_to_" + data.getEndDate() + ".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel: " + e.getMessage(), e);
        }
    }
}