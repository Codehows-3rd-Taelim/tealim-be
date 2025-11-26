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
    private int Status;

    private String productCode;
    private String softVersion;

    private Long storeId;
}