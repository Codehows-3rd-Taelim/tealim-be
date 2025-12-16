package com.codehows.taelimbe.notification.repository;

import com.codehows.taelimbe.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

}