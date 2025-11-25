package com.codehows.taelimbe.dto;

public class RobotFullInfoDTO {
    private String sn;
    private String mac;
    private String nickname;
    private int online;
    private int battery;
    private int robotStatus;
    private String modelName;
    private String softwareVersion;

    // Getters and Setters
    public String getSn() { return sn; }
    public void setSn(String sn) { this.sn = sn; }

    public String getMac() { return mac; }
    public void setMac(String mac) { this.mac = mac; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public int getOnline() { return online; }
    public void setOnline(int online) { this.online = online; }

    public int getBattery() { return battery; }
    public void setBattery(int battery) { this.battery = battery; }

    public int getRobotStatus() { return robotStatus; }
    public void setRobotStatus(int robotStatus) { this.robotStatus = robotStatus; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getSoftwareVersion() { return softwareVersion; }
    public void setSoftwareVersion(String softwareVersion) { this.softwareVersion = softwareVersion; }
}