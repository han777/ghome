package com.apartment.controller;

import com.apartment.dto.ServiceFeeDetailDTO;
import com.apartment.entity.*;
import com.apartment.repository.OrderProductDetailRepository;
import com.apartment.repository.ProductPriceRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/api/service-fee-detail")
public class ServiceFeeDetailController {

    @Autowired
    private OrderProductDetailRepository detailRepository;
    @Autowired
    private ProductPriceRepository productRepository;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private ServiceFeeDetailDTO toDTO(OrderProductDetail detail) {
        ServiceFeeDetailDTO dto = new ServiceFeeDetailDTO();
        dto.setId(detail.getId());

        // Order info
        RoomOrder order = detail.getOrder();
        dto.setOrderNo(order != null ? order.getOrderNo() : "-");

        // Product info
        ProductPrice product = detail.getProduct();
        if (product != null) {
            dto.setProductName(product.getProductName());
            dto.setStandardPrice(product.getPrice());
        } else {
            dto.setProductName("-");
            dto.setStandardPrice(BigDecimal.ZERO);
        }

        dto.setActualPrice(detail.getActualPrice() != null ? detail.getActualPrice() : BigDecimal.ZERO);
        dto.setQuantity(detail.getQuantity() != null ? detail.getQuantity() : 1);
        dto.setAmount(dto.getActualPrice().multiply(BigDecimal.valueOf(dto.getQuantity())));

        return dto;
    }

    @GetMapping("/paged")
    public org.springframework.data.domain.Page<ServiceFeeDetailDTO> getPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkInFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkInTo) {

        List<OrderProductDetail> all = detailRepository.findAll();

        // Filter by order's startDate (check-in time)
        List<OrderProductDetail> filtered = all;
        if (checkInFrom != null || checkInTo != null) {
            filtered = new ArrayList<>();
            for (OrderProductDetail d : all) {
                RoomOrder order = d.getOrder();
                if (order == null || order.getStartDate() == null) continue;
                LocalDateTime ci = order.getStartDate();
                if (checkInFrom != null && ci.isBefore(checkInFrom)) continue;
                if (checkInTo != null && ci.isAfter(checkInTo)) continue;
                filtered.add(d);
            }
        }

        // Sort by order startDate descending
        filtered.sort((a, b) -> {
            LocalDateTime aTime = a.getOrder() != null ? a.getOrder().getCreatedAt() : LocalDateTime.MIN;
            LocalDateTime bTime = b.getOrder() != null ? b.getOrder().getCreatedAt() : LocalDateTime.MIN;
            return bTime.compareTo(aTime);
        });

        // Pagination
        int total = filtered.size();
        int start = Math.min(page * size, total);
        int end = Math.min(start + size, total);
        List<ServiceFeeDetailDTO> pageContent = new ArrayList<>();
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
        List<OrderProductDetail> all = detailRepository.findAll();
        List<OrderProductDetail> filtered = all;
        if (checkInFrom != null || checkInTo != null) {
            filtered = new ArrayList<>();
            for (OrderProductDetail d : all) {
                RoomOrder order = d.getOrder();
                if (order == null || order.getStartDate() == null) continue;
                LocalDateTime ci = order.getStartDate();
                if (checkInFrom != null && ci.isBefore(checkInFrom)) continue;
                if (checkInTo != null && ci.isAfter(checkInTo)) continue;
                filtered.add(d);
            }
        }

        filtered.sort((a, b) -> {
            LocalDateTime aTime = a.getOrder() != null ? a.getOrder().getCreatedAt() : LocalDateTime.MIN;
            LocalDateTime bTime = b.getOrder() != null ? b.getOrder().getCreatedAt() : LocalDateTime.MIN;
            return bTime.compareTo(aTime);
        });

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Service Fee Detail");

            Row header = sheet.createRow(0);
            String[] headers = {"#", "Order No", "Product/Service", "Standard Price", "Actual Price", "Quantity", "Amount"};
            CellStyle boldStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldStyle.setFont(boldFont);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(boldStyle);
            }

            int rowNum = 1;
            for (int idx = 0; idx < filtered.size(); idx++) {
                ServiceFeeDetailDTO dto = toDTO(filtered.get(idx));
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(idx + 1);
                row.createCell(1).setCellValue(dto.getOrderNo());
                row.createCell(2).setCellValue(dto.getProductName());
                row.createCell(3).setCellValue(dto.getStandardPrice() != null ? dto.getStandardPrice().doubleValue() : 0);
                row.createCell(4).setCellValue(dto.getActualPrice() != null ? dto.getActualPrice().doubleValue() : 0);
                row.createCell(5).setCellValue(dto.getQuantity());
                row.createCell(6).setCellValue(dto.getAmount() != null ? dto.getAmount().doubleValue() : 0);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"service_fee_detail.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel: " + e.getMessage(), e);
        }
    }
}