package com.codehows.taelimbe.service;

import com.codehows.taelimbe.client.PuduAPIClient;
import com.codehows.taelimbe.dto.RobotDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RobotService {

    private final PuduAPIClient puduAPIClient;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 단일 로봇 정보 조회
     */
    public RobotDTO getRobotInfo(String sn, Long shopId) {

        RobotDTO dto = new RobotDTO();
        dto.setSn(sn);
        dto.setStoreId(shopId);

        // 1) 목록에서 MAC 얻기
        JsonNode robot = fetchRobotBySn(sn, shopId);
        if (robot != null) {
            dto.setMac(robot.path("mac").asText(null));
        }

        // 2) 상세 정보
        JsonNode detail = fetchRobotDetail(sn);
        if (detail != null) {
            dto.setNickname(detail.path("nickname").asText(null));
            dto.setBattery(detail.path("battery").asInt());
            dto.setOnline(detail.path("online").asBoolean());
            dto.setStatus(detail.path("cleanbot").path("clean").path("status").asInt());
        }

        // 3) 충전 로그 (최신 1개)
        JsonNode charge = fetchLatestChargeLog(sn, shopId);
        if (charge != null) {
            dto.setProductCode(charge.path("product_code").asText(null));
            dto.setSoftVersion(charge.path("soft_version").asText(null));
        }

        return dto;
    }

    /**
     * 매장 전체 로봇 조회
     */
    public List<RobotDTO> getRobotListByShop(Long shopId) {

        List<JsonNode> robots = fetchRobotListAll(shopId);
        List<RobotDTO> result = new ArrayList<>();

        for (JsonNode robot : robots) {
            String sn = robot.path("sn").asText();
            result.add(getRobotInfo(sn, shopId));
        }

        return result;
    }

    /**
     * 목록에서 특정 SN만 찾기
     */
    private JsonNode fetchRobotBySn(String sn, Long shopId) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/data-open-platform-service/v1/api/robot")
                    .queryParam("limit", 100)
                    .queryParam("offset", 0)
                    .queryParam("shop_id", shopId)  // 외부 API는 snake_case 유지!
                    .toUriString();

            ResponseEntity<String> response = puduAPIClient.callPuduAPI(url, "GET");

            JsonNode list = mapper.readTree(response.getBody())
                    .path("data").path("list");

            for (JsonNode node : list) {
                if (sn.equals(node.path("sn").asText())) {
                    return node;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 매장 전체 목록
     */
    private List<JsonNode> fetchRobotListAll(Long shopId) {

        List<JsonNode> result = new ArrayList<>();

        try {
            String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/data-open-platform-service/v1/api/robot")
                    .queryParam("limit", 100)
                    .queryParam("offset", 0)
                    .queryParam("shop_id", shopId)  // 외부 API 규칙 때문
                    .toUriString();

            ResponseEntity<String> response = puduAPIClient.callPuduAPI(url, "GET");
            JsonNode list = mapper.readTree(response.getBody())
                    .path("data").path("list");

            if (list.isArray()) {
                list.forEach(result::add);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private JsonNode fetchRobotDetail(String sn) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/cleanbot-service/v1/api/open/robot/detail")
                    .queryParam("sn", sn)
                    .toUriString();

            ResponseEntity<String> response = puduAPIClient.callPuduAPI(url, "GET");
            return mapper.readTree(response.getBody()).path("data");

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private JsonNode fetchLatestChargeLog(String sn, Long shopId) {

        long end = System.currentTimeMillis() / 1000;
        long start = end - (60L * 60 * 24 * 90);

        try {
            String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/data-board/v1/log/charge/query_list")
                    .queryParam("start_time", start)  // 여기도 외부 API라 snake_case 유지
                    .queryParam("end_time", end)
                    .queryParam("limit", 1)
                    .queryParam("offset", 0)
                    .queryParam("shop_id", shopId)
                    .toUriString();

            ResponseEntity<String> response = puduAPIClient.callPuduAPI(url, "GET");

            JsonNode list = mapper.readTree(response.getBody())
                    .path("data").path("list");

            if (list.isArray() && list.size() > 0) {
                return list.get(0);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public ResponseEntity<String> getRobotStatusV2(String sn, String mac) {
        try {
            UriComponentsBuilder builder =
                    UriComponentsBuilder.fromHttpUrl("https://open-platform.pudutech.com")
                            .path("/open-platform-service/v2/status/get_by_sn");

            if (sn != null && !sn.isBlank()) builder.queryParam("sn", sn);
            if (mac != null && !mac.isBlank()) builder.queryParam("mac", mac);

            return puduAPIClient.callOpenPlatformAPI(builder.toUriString());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
