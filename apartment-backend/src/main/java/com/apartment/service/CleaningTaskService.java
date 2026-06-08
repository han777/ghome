package com.apartment.service;

import com.apartment.entity.CleaningTask;
import com.apartment.entity.Room;
import com.apartment.entity.RoomOccupy;
import com.apartment.repository.CleaningTaskRepository;
import com.apartment.repository.RoomOccupyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CleaningTaskService {

    private static final Logger log = LoggerFactory.getLogger(CleaningTaskService.class);

    @Autowired
    private CleaningTaskRepository taskRepository;

    @Autowired
    private RoomOccupyRepository occupyRepository;

    /**
     * 定时任务：每天凌晨3点生成当日清扫任务
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void generateDailyTasks() {
        LocalDate today = LocalDate.now();
        log.info("开始生成 {} 的清扫任务", today);

        Set<Long> processedRooms = new HashSet<>();

        // 1. 强打扫：当日退房的房间
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);
        List<RoomOccupy> checkoutOccupies = occupyRepository.findCheckingOutToday(startOfDay, endOfDay);

        for (RoomOccupy occupy : checkoutOccupies) {
            Room room = occupy.getRoom();
            if (room != null && !processedRooms.contains(room.getId())) {
                // 强打扫：先终止该房间所有计划状态的清扫任务
                List<CleaningTask> plannedTasks = taskRepository.findByRoomIdAndStatus(room.getId(), 0);
                for (CleaningTask plannedTask : plannedTasks) {
                    plannedTask.setStatus(1);  // 取消
                }
                if (!plannedTasks.isEmpty()) {
                    taskRepository.saveAll(plannedTasks);
                }
                createTask(room, 2, today, "当日退房，需强打扫");
                processedRooms.add(room.getId());
            }
        }

        // 2. 日常保洁：当前入住且当日不退房的房间
        List<RoomOccupy> currentOccupies = occupyRepository.findCurrentOccupying(endOfDay);

        for (RoomOccupy occupy : currentOccupies) {
            Room room = occupy.getRoom();
            if (room != null && !processedRooms.contains(room.getId())) {
                // 日常保洁：如果该房间有任何计划状态的清扫任务，则跳过
                if (!taskRepository.existsByRoomIdAndStatus(room.getId(), 0)) {
                    createTask(room, 1, today, "日常保洁");
                    processedRooms.add(room.getId());
                }
            }
        }

        log.info("清扫任务生成完成，共生成 {} 个任务", processedRooms.size());
    }

    /**
     * 创建清扫任务
     */
    private CleaningTask createTask(Room room, Integer taskType, LocalDate taskDate, String content) {
        CleaningTask task = new CleaningTask();
        task.setRoom(room);
        task.setTaskType(taskType);
        task.setTaskDate(taskDate);
        task.setContent(content);
        task.setStatus(0);  // 计划状态
        task.setGeneratedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    /**
     * 获取所有清扫任务
     */
    public List<CleaningTask> getAllTasks() {
        return taskRepository.findAllByOrderByTaskDateDescCreatedAtDesc();
    }

    /**
     * 获取指定日期的清扫任务
     */
    public List<CleaningTask> getTasksByDate(LocalDate date) {
        return taskRepository.findByTaskDateOrderByCreatedAtDesc(date);
    }

    /**
     * 保存清扫任务（新增或编辑）
     */
    @Transactional
    public CleaningTask saveTask(CleaningTask task) {
        return taskRepository.save(task);
    }

    /**
     * 完成任务
     */
    @Transactional
    public CleaningTask completeTask(Long id) {
        CleaningTask task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("清扫任务不存在: " + id));
        task.setStatus(2);  // 完成
        task.setCompletedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    /**
     * 完成某房间的全部未完成清扫任务
     */
    @Transactional
    public List<CleaningTask> completeAllTasksByRoom(Long roomId) {
        List<CleaningTask> pendingTasks = taskRepository.findByRoomIdAndStatus(roomId, 0);
        LocalDateTime now = LocalDateTime.now();
        for (CleaningTask task : pendingTasks) {
            task.setStatus(2);  // 完成
            task.setCompletedAt(now);
        }
        return taskRepository.saveAll(pendingTasks);
    }

    /**
     * 删除任务
     */
    @Transactional
    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

    /**
     * 维护完成后创建强打扫任务
     */
    @Transactional
    public CleaningTask createDeepCleanForMaintenance(Room room) {
        LocalDate today = LocalDate.now();
        if (taskRepository.existsByRoomIdAndTaskTypeAndStatus(room.getId(), 2, 0)) {
            return null;
        }
        return createTask(room, 2, today, "维护完成，需强打扫");
    }

    /**
     * 手动触发生成任务（用于测试）
     */
    @Transactional
    public int manualGenerateTasks(LocalDate date) {
        Set<Long> processedRooms = new HashSet<>();
        int count = 0;

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        // 1. 强打扫：当日退房
        List<RoomOccupy> checkoutOccupies = occupyRepository.findCheckingOutToday(startOfDay, endOfDay);
        for (RoomOccupy occupy : checkoutOccupies) {
            Room room = occupy.getRoom();
            if (room != null && !processedRooms.contains(room.getId())) {
                // 强打扫：先终止该房间所有计划状态的清扫任务
                List<CleaningTask> plannedTasks = taskRepository.findByRoomIdAndStatus(room.getId(), 0);
                for (CleaningTask plannedTask : plannedTasks) {
                    plannedTask.setStatus(1);  // 取消
                }
                if (!plannedTasks.isEmpty()) {
                    taskRepository.saveAll(plannedTasks);
                }
                createTask(room, 2, date, "当日退房，需强打扫");
                processedRooms.add(room.getId());
                count++;
            }
        }

        // 2. 日常保洁
        List<RoomOccupy> currentOccupies = occupyRepository.findCurrentOccupying(endOfDay);
        for (RoomOccupy occupy : currentOccupies) {
            Room room = occupy.getRoom();
            if (room != null && !processedRooms.contains(room.getId())) {
                // 日常保洁：如果该房间有任何计划状态的清扫任务，则跳过
                if (!taskRepository.existsByRoomIdAndStatus(room.getId(), 0)) {
                    createTask(room, 1, date, "日常保洁");
                    processedRooms.add(room.getId());
                    count++;
                }
            }
        }

        return count;
    }
}
