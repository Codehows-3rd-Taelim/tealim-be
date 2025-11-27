package com.codehows.taelimbe.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RobotDTO {

    private String sn;
    private String mac;

    private String nickname;
    private boolean online;
    private int battery;
    private int status;

    private String productCode;
    private String softVersion;

    private Long storeId;  // 내부 DB store_id
}
