package com.apartment.controller;

import com.apartment.entity.ScheduledTaskLog;
import com.apartment.repository.ScheduledTaskLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/scheduled-task-logs")
public class ScheduledTaskLogController {

    @Autowired
    private ScheduledTaskLogRepository scheduledTaskLogRepository;

    @GetMapping("/paged")
    public Page<ScheduledTaskLog> getPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String taskName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime executeFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime executeTo) {

        List<ScheduledTaskLog> all = scheduledTaskLogRepository.findAll();

        List<ScheduledTaskLog> filtered = all;
        if (taskName != null || executeFrom != null || executeTo != null) {
            filtered = new ArrayList<>();
            for (ScheduledTaskLog log : all) {
                if (taskName != null && !taskName.isBlank()
                        && (log.getTaskName() == null || !log.getTaskName().contains(taskName))) continue;
                if (executeFrom != null && log.getExecuteTime() != null && log.getExecuteTime().isBefore(executeFrom)) continue;
                if (executeTo != null && log.getExecuteTime() != null && log.getExecuteTime().isAfter(executeTo)) continue;
                filtered.add(log);
            }
        }

        filtered.sort((a, b) -> {
            LocalDateTime aTime = a.getExecuteTime() != null ? a.getExecuteTime() : LocalDateTime.MIN;
            LocalDateTime bTime = b.getExecuteTime() != null ? b.getExecuteTime() : LocalDateTime.MIN;
            return bTime.compareTo(aTime);
        });

        int total = filtered.size();
        int start = Math.min(page * size, total);
        int end = Math.min(start + size, total);
        List<ScheduledTaskLog> pageContent = filtered.subList(start, end);

        return new PageImpl<>(pageContent, PageRequest.of(page, size), total);
    }
}
