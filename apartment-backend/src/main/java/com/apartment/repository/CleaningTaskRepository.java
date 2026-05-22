package com.apartment.repository;

import com.apartment.entity.CleaningTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CleaningTaskRepository extends JpaRepository<CleaningTask, Long> {

    boolean existsByRoomIdAndTaskDate(Long roomId, LocalDate taskDate);

    List<CleaningTask> findByTaskDate(LocalDate taskDate);

    Optional<CleaningTask> findByRoomIdAndTaskDate(Long roomId, LocalDate taskDate);

    List<CleaningTask> findByTaskDateOrderByCreatedAtDesc(LocalDate taskDate);

    List<CleaningTask> findAllByOrderByTaskDateDescCreatedAtDesc();
}
