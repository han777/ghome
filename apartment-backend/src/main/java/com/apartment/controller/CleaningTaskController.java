package com.apartment.controller;

import com.apartment.entity.CleaningTask;
import com.apartment.service.CleaningTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cleaning-tasks")
public class CleaningTaskController {

    @Autowired
    private CleaningTaskService taskService;

    @GetMapping("/all")
    public List<CleaningTask> getAllTasks() {
        return taskService.getAllTasks();
    }

    @GetMapping("/date/{date}")
    public List<CleaningTask> getTasksByDate(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return taskService.getTasksByDate(date);
    }

    @GetMapping("/{id}")
    public CleaningTask getTask(@PathVariable Long id) {
        return taskService.getAllTasks().stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @PostMapping
    public CleaningTask saveTask(@RequestBody CleaningTask task) {
        return taskService.saveTask(task);
    }

    @PostMapping("/{id}/complete")
    public CleaningTask completeTask(@PathVariable Long id) {
        return taskService.completeTask(id);
    }

    @DeleteMapping("/{id}")
    public void deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
    }

    @PostMapping("/generate/{date}")
    public int manualGenerate(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return taskService.manualGenerateTasks(date);
    }
}
