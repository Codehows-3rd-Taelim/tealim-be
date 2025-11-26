package com.codehows.taelimbe.service.detail;

import com.codehows.taelimbe.dto.RobotDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class DataCombiningService {

    private final ObjectMapper objectMapper;

    public DataCombiningService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RobotDTO combineRobotData(String robotJson, String chargingJson) throws Exception {
        JsonNode robotNode = objectMapper.readTree(robotJson);
        JsonNode chargingNode = objectMapper.readTree(chargingJson);

        RobotDTO robotDto = new RobotDTO();

        // robotJson에서 추출
        if (robotNode.has("data")) {
            JsonNode data = robotNode.get("data");
            robotDto.setSn(data.get("sn").asText());
            robotDto.setMac(data.get("mac").asText());
            robotDto.setNickname(data.get("nickname").asText());
            robotDto.setOnline(data.get("online").asInt());
            robotDto.setBattery(data.get("battery").asInt());
            robotDto.setRobotStatus(data.get("status").asInt());
        }

        // chargingJson에서 추출
        if (chargingNode.has("data")) {
            JsonNode data = chargingNode.get("data");
            if (data.isArray() && data.size() > 0) {
                JsonNode firstRecord = data.get(0);
                robotDto.setModelName(firstRecord.get("model_name").asText());
                robotDto.setSoftwareVersion(firstRecord.get("software_version").asText());
            }
        }

        return robotDto;
    }
}