package com.codehows.taelimbe.service;

import com.codehows.taelimbe.client.PuduAPIClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class CleaningMachineService {

    private final PuduAPIClient puduAPIClient;
    private final ObjectMapper objectMapper;
    private static final String ENCODING = "UTF-8";

    public CleaningMachineService(PuduAPIClient puduAPIClient, ObjectMapper objectMapper) {
        this.puduAPIClient = puduAPIClient;
        this.objectMapper = objectMapper;
    }

    public ResponseEntity<String> getRobotDetail(String sn) {
        try {
            System.out.println("====== 로봇 상세 조회 시작 ======");
            System.out.println("SN: " + sn);

            String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/cleanbot-service/v1/api/open/robot/detail")
                    .queryParam("sn", sn)
                    .toUriString();

            System.out.println("Target URL: " + url);
            return puduAPIClient.callPuduAPI(url, "GET");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    //이거는 여러개 조합할 용도로 만든건데 위치를... 고민중 여기있어도 되려나
    public ResponseEntity<?> getRobotFullInfo(String sn, long shop_id, Long start_time, Long end_time,
                                              int timezone_offset, int limit) {
        try {
            System.out.println("====== 로봇 전체 정보 조회 시작 ======");
            System.out.println("SN: " + sn);
            System.out.println("Shop ID: " + shop_id);
            System.out.println("Start Time: " + start_time);
            System.out.println("End Time: " + end_time);
            System.out.println("Timezone Offset: " + timezone_offset);
            System.out.println("Limit: " + limit);

            // 1. 로봇 상세 정보 조회
            ResponseEntity<String> robotResponse = getRobotDetail(sn);
            String robotJson = robotResponse.getBody();

            // 2. 충전 정보 조회
            // start_time, end_time이 null이면 기본값으로 24시간 전 설정
            long st = (start_time != null) ? start_time : System.currentTimeMillis() / 1000 - 86400;
            long et = (end_time != null) ? end_time : System.currentTimeMillis() / 1000;

            String chargingUrl = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/data-board/v1/log/charge/query_list")
                    .queryParam("start_time", st)
                    .queryParam("end_time", et)
                    .queryParam("offset", 0)
                    .queryParam("limit", limit)
                    .queryParam("timezone_offset", timezone_offset)
                    .queryParam("shop_id", shop_id)
                    .toUriString();

            System.out.println("Charging URL: " + chargingUrl);
            ResponseEntity<String> chargingResponse = puduAPIClient.callPuduAPI(chargingUrl, "GET");
            String chargingJson = chargingResponse.getBody();

            // 3. 데이터 결합
            ObjectNode combinedData = combineRobotData(robotJson, chargingJson);

            return ResponseEntity.ok(combinedData);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * 로봇 정보와 충전 정보를 결합
     *
     * 추후 다른 서비스에서도 같은 로직이 필요하면
     * DataCombiningService로 분리 예정
     */
    private ObjectNode combineRobotData(String robotJson, String chargingJson) throws Exception {
        JsonNode robotNode = objectMapper.readTree(robotJson);
        JsonNode chargingNode = objectMapper.readTree(chargingJson);

        ObjectNode result = objectMapper.createObjectNode();

        // 로봇 정보 추출
        if (robotNode.has("data")) {
            JsonNode data = robotNode.get("data");
            result.put("sn", data.get("sn").asText());
            result.put("mac", data.get("mac").asText());
            result.put("nickname", data.get("nickname").asText());
            result.put("online", data.get("online").asInt());
            result.put("battery", data.get("battery").asInt());
            result.put("robotStatus", data.get("status").asInt());
        }

        // 충전 정보 추출
        if (chargingNode.has("data")) {
            JsonNode data = chargingNode.get("data");
            if (data.isArray() && data.size() > 0) {
                JsonNode firstRecord = data.get(0);
                result.put("modelName", firstRecord.get("model_name").asText());
                result.put("softwareVersion", firstRecord.get("software_version").asText());
            }
        }

        return result;
    }
}