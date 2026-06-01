package com.apartment.repository;

import com.apartment.entity.BookingPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingPurposeRepository extends JpaRepository<BookingPurpose, Long> {
}
