package com.codehows.taelimbe.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RobotInfoDTO {
    private String sn;
    private String mac;
    private String nickname;
    private int online;
    private int battery;
    private int robotStatus;
    private String modelName;
    private String softwareVersion;

}