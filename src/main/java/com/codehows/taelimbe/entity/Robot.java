package com.codehows.taelimbe.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "robot")
public class Robot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sn;

    @Column(nullable = false, unique = true)
    private String mac;

    private String nickname;
    private boolean online;
    private int battery;
    private int status;
    private String productCode;
    private String softVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    // ===== 생성자 (필수값만) =====
    public Robot(String sn, String mac, Store store) {
        this.sn = sn;
        this.mac = mac;
        this.store = store;
    }

    // ===== 로봇 상태 업데이트용 메서드 =====
    public void updateRobotInfo(String nickname, boolean online, int battery,
                                int status, String productCode, String softVersion) {
        this.nickname = nickname;
        this.online = online;
        this.battery = battery;
        this.status = status;
        this.productCode = productCode;
        this.softVersion = softVersion;
    }

    // ===== 매장 변경 (필요시) =====
    public void changeStore(Store store) {
        this.store = store;
    }
}
