package com.codehows.taelimbe.store.repository;

import com.codehows.taelimbe.store.constant.DeleteStatus;
import com.codehows.taelimbe.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByStoreId(Long storeId);

    // StoreRepository
    Optional<Store> findByShopId(Long shopId);

    Optional<Store> findByShopNameAndDelYn(
            String shopName,
            DeleteStatus delYn
    );
}
