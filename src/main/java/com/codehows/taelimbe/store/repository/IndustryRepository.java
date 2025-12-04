package com.codehows.taelimbe.store.repository;

import com.codehows.taelimbe.store.entity.Industry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IndustryRepository extends JpaRepository<Industry, Long> {
    // IndustryRepository
    Optional<Industry> findByIndustryName(String industryName);

}
