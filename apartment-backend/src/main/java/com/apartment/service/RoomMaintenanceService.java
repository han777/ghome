package com.apartment.service;

import com.apartment.entity.CleaningTask;
import com.apartment.entity.RoomMaintenance;
import com.apartment.repository.RoomMaintenanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RoomMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(RoomMaintenanceService.class);

    @Autowired
    private RoomMaintenanceRepository maintenanceRepository;

    @Autowired
    private CleaningTaskService cleaningTaskService;

    /**
     * 定时任务：每5分钟检查到期但仍处于维修中的维护，自动完成
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional
    public void autoCompleteExpiredMaintenances() {
        LocalDateTime now = LocalDateTime.now();
        List<RoomMaintenance> all = maintenanceRepository.findAll();

        List<RoomMaintenance> expired = all.stream()
                .filter(m -> m.getStatus() == 0 && m.getEndTime() != null && m.getEndTime().isBefore(now))
                .toList();

        if (expired.isEmpty()) return;

        log.info("发现 {} 个到期未完成的维护记录，开始自动完成", expired.size());

        for (RoomMaintenance m : expired) {
            completeMaintenance(m);
            log.info("维护记录 {} 已自动完成（到期时间: {}）", m.getId(), m.getEndTime());
        }
    }

    /**
     * 完成维护并触发强打扫任务
     */
    @Transactional
    public void completeMaintenance(RoomMaintenance maintenance) {
        maintenance.setStatus(1);
        maintenanceRepository.save(maintenance);

        if (maintenance.getRoom() != null) {
            CleaningTask task = cleaningTaskService.createDeepCleanForMaintenance(maintenance.getRoom());
            if (task != null) {
                log.info("为房间 {} 创建强打扫任务（维护完成）", maintenance.getRoom().getRoomNo());
            }
        }
    }
}
