package com.codehows.taelimbe.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "sn", length = 20, nullable = false)
    private String sn;

    @Column(name = "mac", length = 17, nullable = false)
    private String mac;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_code", nullable = false)
    private ProductCode productCode;

    @Column(name = "soft_version", length = 255)
    private String softVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private WorkStatus workStatus;

    @Column(name = "nickname", length = 255)
    private String nickname;

    @Column(name = "battery")
    private Long battery;

    @Column(name = "online")
    private Boolean online;

    @ManyToOne
    @JoinColumn(name = "store_id")
    private Store store;

    public enum ProductCode {
        CC1, MT1
    }

    public enum WorkStatus {
        WAIT, WORK, CHARGE, OFFLINE
    }

}
