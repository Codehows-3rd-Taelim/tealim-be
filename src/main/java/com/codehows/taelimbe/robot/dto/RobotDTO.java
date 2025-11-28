package com.codehows.taelimbe.robot.dto;

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
    private Integer battery;
    private Integer status;

    private String productCode;
    private String softVersion;

    private Long storeId;
}
