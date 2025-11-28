package com.codehows.taelimbe.dto;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
public class RobotDTO {

    private Long robotId;
    private String sn;
    private String mac;

    private String nickname;
    private boolean online;
    private int battery;
    private int status;

    private String productCode;
    private String softVersion;

    private Long storeId;
}
