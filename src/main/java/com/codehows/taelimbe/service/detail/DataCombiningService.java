package com.codehows.taelimbe.service.detail;

import com.codehows.taelimbe.dto.RobotInfoDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class DataCombiningService {

    private final ObjectMapper objectMapper;

    public DataCombiningService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RobotInfoDTO combineRobotData(String robotJson, String chargingJson) throws Exception {
        JsonNode robotNode = objectMapper.readTree(robotJson);
        JsonNode chargingNode = objectMapper.readTree(chargingJson);

        RobotInfoDTO robotInfo = new RobotInfoDTO();

        // robotJson에서 추출
        if (robotNode.has("data")) {
            JsonNode data = robotNode.get("data");
            robotInfo.setSn(data.get("sn").asText());
            robotInfo.setMac(data.get("mac").asText());
            robotInfo.setNickname(data.get("nickname").asText());
            robotInfo.setOnline(data.get("online").asInt());
            robotInfo.setBattery(data.get("battery").asInt());
            robotInfo.setRobotStatus(data.get("status").asInt());
        }

        // chargingJson에서 추출
        if (chargingNode.has("data")) {
            JsonNode data = chargingNode.get("data");
            if (data.isArray() && data.size() > 0) {
                JsonNode firstRecord = data.get(0);
                robotInfo.setModelName(firstRecord.get("model_name").asText());
                robotInfo.setSoftwareVersion(firstRecord.get("software_version").asText());
            }
        }

        return robotInfo;
    }
}