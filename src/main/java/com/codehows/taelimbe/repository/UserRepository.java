package com.codehows.taelimbe.repository;

import com.codehows.taelimbe.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsById(String id);
    Optional<User> findById(String id);

}
