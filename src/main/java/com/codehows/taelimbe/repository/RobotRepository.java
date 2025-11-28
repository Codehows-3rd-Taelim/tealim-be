package com.codehows.taelimbe.repository;

import com.codehows.taelimbe.entity.Robot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RobotRepository extends JpaRepository<Robot, Long> {
}
