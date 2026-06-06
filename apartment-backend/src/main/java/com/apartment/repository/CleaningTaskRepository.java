package com.apartment.repository;

import com.apartment.entity.CleaningTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CleaningTaskRepository extends JpaRepository<CleaningTask, Long> {

    boolean existsByRoomIdAndTaskDate(Long roomId, LocalDate taskDate);

    List<CleaningTask> findByTaskDate(LocalDate date);

    Optional<CleaningTask> findByRoomIdAndTaskDate(Long roomId, LocalDate taskDate);

    List<CleaningTask> findByTaskDateOrderByCreatedAtDesc(LocalDate date);

    List<CleaningTask> findAllByOrderByTaskDateDescCreatedAtDesc();

    List<CleaningTask> findByRoomIdAndStatus(Long roomId, Integer status);

    List<CleaningTask> findByStatus(Integer status);

    List<CleaningTask> findByRoomIdAndStatusIn(Long roomId, Collection<Integer> statuses);

    boolean existsByRoomIdAndStatus(Long roomId, Integer status);

    boolean existsByRoomIdAndTaskTypeAndStatus(Long roomId, Integer taskType, Integer status);
}
