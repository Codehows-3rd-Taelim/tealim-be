package com.codehows.taelimbe.robot.repository;

import com.codehows.taelimbe.robot.entity.Robot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RobotRepository extends JpaRepository<Robot, Long> {

    Optional<Robot> findBySn(String sn);

    List<Robot> findAllByStore_StoreId(Long storeId);



}
