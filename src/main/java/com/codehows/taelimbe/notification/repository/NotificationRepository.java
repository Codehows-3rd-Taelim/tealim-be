package com.codehows.taelimbe.notification.repository;

import com.codehows.taelimbe.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    //  아직 토스트 안 뜬 것
    List<Notification> findByUserIdAndDeliveredAtIsNull(Long userId);
}
