package com.codehows.taelimbe.repository;

import com.codehows.taelimbe.entity.Robot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RobotRepository extends JpaRepository<Robot, Long> {

    Optional<Robot> findBySn(String sn);
    Optional<Robot> findByMac(String mac);
}
