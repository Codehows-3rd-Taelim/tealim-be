package com.codehows.taelimbe.repository;

import com.codehows.taelimbe.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsById(String id);
    Optional<User> findById(String id);

    // 💡 Fetch Join을 사용하여 User를 로드할 때 Store 정보도 즉시 로드합니다.
    @Query("SELECT u FROM User u JOIN FETCH u.store WHERE u.id = :id")
    Optional<User> findByIdWithStore(String id);

    List<User> findByStore_StoreId(Long storeId);

}
