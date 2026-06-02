package com.apartment.repository;

import com.apartment.entity.BookingPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookingPurposeRepository extends JpaRepository<BookingPurpose, Long> {
    Optional<BookingPurpose> findByName(String name);
}
