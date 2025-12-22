package com.codehows.taelimbe.robot.repository;

import com.codehows.taelimbe.robot.entity.Robot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RobotRepository extends JpaRepository<Robot, Long> {

    Optional<Robot> findBySn(String sn);

    List<Robot> findAllByStore_StoreId(Long storeId);

    @Query("""
        select r
        from Robot r
        where r.store.storeId = :storeId
          and r.productCode in ('CC1', 'MT1')
    """)
    List<Robot> findAllByStoreAndCc1OrMt1(@Param("storeId") Long storeId);


    @Query("""
    select r.robotId
    from Robot r
    where r.store.storeId = :storeId
""")
    List<Long> findRobotIdsByStoreId(@Param("storeId") Long storeId);
}
