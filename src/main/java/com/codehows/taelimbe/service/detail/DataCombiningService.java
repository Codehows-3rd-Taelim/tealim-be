package com.codehows.taelimbe.service.detail;

import com.codehows.taelimbe.dto.RobotFullInfoDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class DataCombiningService {

    private final ObjectMapper objectMapper;

    public DataCombiningService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RobotFullInfoDTO combineRobotData(String robotJson, String chargingJson) throws Exception {
        JsonNode robotNode = objectMapper.readTree(robotJson);
        JsonNode chargingNode = objectMapper.readTree(chargingJson);

        RobotFullInfoDTO robotFullInfo = new RobotFullInfoDTO();

        // robotJson에서 추출
        if (robotNode.has("data")) {
            JsonNode data = robotNode.get("data");
            robotFullInfo.setSn(data.get("sn").asText());
            robotFullInfo.setMac(data.get("mac").asText());
            robotFullInfo.setNickname(data.get("nickname").asText());
            robotFullInfo.setOnline(data.get("online").asInt());
            robotFullInfo.setBattery(data.get("battery").asInt());
            robotFullInfo.setRobotStatus(data.get("status").asInt());
        }

        // chargingJson에서 추출
        if (chargingNode.has("data")) {
            JsonNode data = chargingNode.get("data");
            if (data.isArray() && data.size() > 0) {
                JsonNode firstRecord = data.get(0);
                robotFullInfo.setModelName(firstRecord.get("model_name").asText());
                robotFullInfo.setSoftwareVersion(firstRecord.get("software_version").asText());
            }
        }

        return robotFullInfo;
    }
}