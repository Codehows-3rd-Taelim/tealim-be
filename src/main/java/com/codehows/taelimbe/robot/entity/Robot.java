package com.codehows.taelimbe.robot.entity;

import com.codehows.taelimbe.store.entity.Store;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "robot")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Robot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "robot_id")
    private Long robotId;

    @Column(nullable = false, unique = true)
    private String sn;

    @Column(nullable = false, unique = true)
    private String mac;

    private String nickname;
    private Boolean online;
    private Integer battery;
    private Integer status;
    private String productCode;

    @Column(name = "is_charging")
    private Integer isCharging;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    // ================= Constructor for Required Fields =================
    public Robot(String sn, String mac, Store store) {
        this.sn = sn;
        this.mac = mac;
        this.store = store;
    }

    // ================= Update Methods =================
    public void updateRobotInfo(String nickname, boolean online, int battery,
                                int status, String productCode, Integer isCharging) {
        this.nickname = nickname;
        this.online = online;
        this.battery = battery;
        this.status = status;
        this.productCode = productCode;
        this.isCharging = isCharging;
    }

    public void changeStore(Store store) {
        this.store = store;
    }
}
