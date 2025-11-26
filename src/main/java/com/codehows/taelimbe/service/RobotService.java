package com.codehows.taelimbe.service;

import com.codehows.taelimbe.client.PuduAPIClient;
import com.codehows.taelimbe.dto.RobotDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class RobotService {

    private final PuduAPIClient puduAPIClient;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 전체 로봇 정보 통합 (RobotDTO 생성)
     */
    public RobotDTO combineRobotInfo(String sn, Long shopId) {

        RobotDTO dto = new RobotDTO();
        dto.setSn(sn);
        dto.setStoreId(shopId);

        // 1) 로봇 목록 API (SN → MAC 얻기)
        JsonNode robotInfo = fetchRobotList(sn, shopId);
        if (robotInfo != null) {
            dto.setMac(robotInfo.get("mac").asText());
        }

        // 2) 상세 정보 API
        JsonNode detail = fetchRobotDetail(sn);
        if (detail != null) {
            dto.setNickname(detail.path("nickname").asText());
            dto.setBattery(detail.path("battery").asInt());
            dto.setOnline(detail.path("online").asBoolean());
            dto.setStatus(detail.path("cleanbot").path("clean").path("status").asInt());
        }

        // 3) 충전 로그 API → soft_version, product_code
        JsonNode charge = fetchLatestChargeLog(sn, shopId);
        if (charge != null) {
            dto.setProductCode(charge.path("product_code").asText());
            dto.setSoftVersion(charge.path("soft_version").asText());
        }



        return dto;
    }

    // ------------------------------------------------------------------------
    // 1) 로봇 목록 API
    // ------------------------------------------------------------------------
    private JsonNode fetchRobotList(String sn, Long shopId) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/data-open-platform-service/v1/api/robot")
                    .queryParam("limit", 50)
                    .queryParam("offset", 0)
                    .queryParam("shop_id", shopId)
                    .toUriString();

            ResponseEntity<String> res = puduAPIClient.callPuduAPI(url, "GET");

            JsonNode json = mapper.readTree(res.getBody());
            JsonNode list = json.path("data").path("list");

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

    // ------------------------------------------------------------------------
    // 2) 로봇 상세 API
    // ------------------------------------------------------------------------
    private JsonNode fetchRobotDetail(String sn) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/cleanbot-service/v1/api/open/robot/detail")
                    .queryParam("sn", sn)
                    .toUriString();

            ResponseEntity<String> res = puduAPIClient.callPuduAPI(url, "GET");


            return mapper.readTree(res.getBody()).path("data");

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ------------------------------------------------------------------------
    // 3) 충전 로그 최신 1개 조회
    // ------------------------------------------------------------------------
    private JsonNode fetchLatestChargeLog(String sn, Long shopId) {

        long end = System.currentTimeMillis() / 1000;
        long start = end - (60L * 60 * 24 * 90); //90일


        try {
            String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/data-board/v1/log/charge/query_list")
                    .queryParam("start_time", start)
                    .queryParam("end_time", end)
                    .queryParam("limit", 1)
                    .queryParam("offset", 0)
                    .queryParam("shop_id", shopId)
                    .toUriString();

            ResponseEntity<String> res = puduAPIClient.callPuduAPI(url, "GET");
            JsonNode json = mapper.readTree(res.getBody());

            JsonNode list = json.path("data").path("list");
            if (list.isArray() && list.size() > 0) {
                return list.get(0);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // ------------------------------------------------------------------------
    // V2 상태 조회 (원하면 그대로 사용)
    // ------------------------------------------------------------------------
    public ResponseEntity<String> getRobotStatusV2(String sn, String mac) {
        try {
            UriComponentsBuilder builder =
                    UriComponentsBuilder.fromHttpUrl("https://open-platform.pudutech.com") // 상위 도메인
                            .path("/open-platform-service/v2/status/get_by_sn");

            if (sn != null && !sn.isBlank()) builder.queryParam("sn", sn);
            if (mac != null && !mac.isBlank()) builder.queryParam("mac", mac);

            return puduAPIClient.callOpenPlatformAPI(builder.toUriString());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
