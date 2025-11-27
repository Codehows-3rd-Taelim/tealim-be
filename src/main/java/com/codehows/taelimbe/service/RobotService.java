package com.codehows.taelimbe.service;

import com.codehows.taelimbe.client.PuduAPIClient;
import com.codehows.taelimbe.dto.RobotDTO;
import com.codehows.taelimbe.entity.Robot;
import com.codehows.taelimbe.entity.Store;
import com.codehows.taelimbe.repository.RobotRepository;
import com.codehows.taelimbe.repository.StoreRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RobotService {

    private final PuduAPIClient puduAPIClient;
    private final RobotRepository robotRepository;
    private final StoreRepository storeRepository;
    private final ObjectMapper mapper = new ObjectMapper();


    // ==================================================================
    // 1) 매장(storeId) 기준 → 로봇 전체 DB 저장/업데이트
    // ==================================================================
    @Transactional
    public int syncRobotsByStoreId(Long storeId) {

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));

        Long shopId = store.getShopId();

        List<RobotDTO> robots = getRobotListByShop(shopId);

        int savedCount = 0;
        for (RobotDTO dto : robots) {
            saveRobot(dto, store);
            savedCount++;
        }

        return savedCount;
    }


    // ==================================================================
    // 2) 단일 로봇 저장/업데이트 위에 전체 매장 업데이트에서 사용중
    // ==================================================================
    @Transactional
    public Robot saveRobot(RobotDTO dto, Store store) {

        Robot robot = robotRepository.findBySn(dto.getSn())
                .orElseGet(() -> new Robot(dto.getSn(), dto.getMac(), store));

        robot.updateRobotInfo(
                dto.getNickname(),
                dto.isOnline(),
                dto.getBattery(),
                dto.getStatus(),
                dto.getProductCode(),
                dto.getSoftVersion()
        );

        robot.changeStore(store); // store 매핑 필요 시

        return robotRepository.save(robot);
    }


    // ==================================================================
    // 3) 단일 로봇 조회 (storeId 기반)
    // ==================================================================
    public RobotDTO getRobotInfoByStoreId(String sn, Long storeId) {

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));

        Long shopId = store.getShopId();

        return getRobotInfo(sn, shopId);
    }


    // ==================================================================
    // 4) 단일 로봇 정보 조회 & 조합 (외부 API)
    // ==================================================================
    public RobotDTO getRobotInfo(String sn, Long shopId) {

        RobotDTO dto = new RobotDTO();
        dto.setSn(sn);

        // (1) MAC 조회
        JsonNode robot = fetchRobotBySn(sn, shopId);
        if (robot != null) {
            dto.setMac(robot.path("mac").asText(null));
        }

        // (2) 상세 조회
        JsonNode detail = fetchRobotDetail(sn);
        if (detail != null) {
            dto.setNickname(detail.path("nickname").asText(null));
            dto.setBattery(detail.path("battery").asInt());
            dto.setOnline(detail.path("online").asBoolean());
            dto.setStatus(detail.path("cleanbot").path("clean").path("status").asInt());
        }

        // (3) 충전 기록 조회
        JsonNode charge = fetchLatestChargeLog(sn, shopId);
        if (charge != null) {
            dto.setProductCode(charge.path("product_code").asText(null));
            dto.setSoftVersion(charge.path("soft_version").asText(null));
        }

        return dto;
    }


    // ==================================================================
    // 5) 매장(storeId) 전체 로봇 조회
    // ==================================================================
    public List<RobotDTO> getRobotListByStoreId(Long storeId) {

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));

        return getRobotListByShop(store.getShopId());
    }


    // ==================================================================
    // 6) 매장(shopId) 전체 로봇 조회
    // ==================================================================
    public List<RobotDTO> getRobotListByShop(Long shopId) {

        List<JsonNode> robots = fetchRobotListAll(shopId);
        List<RobotDTO> result = new ArrayList<>();

        for (JsonNode node : robots) {
            String sn = node.path("sn").asText();
            result.add(getRobotInfo(sn, shopId));
        }

        return result;
    }


    // ==================================================================
    // 7) 외부 API 호출들
    // ==================================================================
    private JsonNode fetchRobotBySn(String sn, Long shopId) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/data-open-platform-service/v1/api/robot")
                    .queryParam("limit", 100)
                    .queryParam("offset", 0)
                    .queryParam("shop_id", shopId)
                    .toUriString();

            ResponseEntity<String> response = puduAPIClient.callPuduAPI(url, "GET");
            JsonNode list = mapper.readTree(response.getBody())
                    .path("data").path("list");

            for (JsonNode node : list) {
                if (sn.equals(node.path("sn").asText())) {
                    return node;
                }
            }

        } catch (Exception ignored) {}

        return null;
    }




    private JsonNode fetchLatestChargeLog(String sn, Long shopId) {

        long end = System.currentTimeMillis() / 1000;
        long start = end - 60L * 60 * 24 * 90; // 90일

        try {
            String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/data-board/v1/log/charge/query_list")
                    .queryParam("start_time", start)
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

        } catch (Exception ignored) {}

        return null;
    }


    private List<JsonNode> fetchRobotListAll(Long shopId) {
        List<JsonNode> result = new ArrayList<>();

        try {
            String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/data-open-platform-service/v1/api/robot")
                    .queryParam("limit", 100)
                    .queryParam("offset", 0)
                    .queryParam("shop_id", shopId)
                    .toUriString();

            ResponseEntity<String> response = puduAPIClient.callPuduAPI(url, "GET");
            JsonNode list = mapper.readTree(response.getBody())
                    .path("data").path("list");

            if (list.isArray()) list.forEach(result::add);

        } catch (Exception ignored) {}

        return result;
    }


    private JsonNode fetchRobotDetail(String sn) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/cleanbot-service/v1/api/open/robot/detail")
                    .queryParam("sn", sn)
                    .toUriString();

            ResponseEntity<String> response = puduAPIClient.callPuduAPI(url, "GET");
            return mapper.readTree(response.getBody())
                    .path("data");

        } catch (Exception ignored) {}

        return null;
    }



    // ==================================================================
    // 8) V2 상태 조회
    // ==================================================================
    public ResponseEntity<String> getRobotStatusV2(String sn, String mac) {

        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl("https://open-platform.pudutech.com")
                    .path("/open-platform-service/v2/status/get_by_sn");

            if (sn != null && !sn.isBlank()) builder.queryParam("sn", sn);
            if (mac != null && !mac.isBlank()) builder.queryParam("mac", mac);

            return puduAPIClient.callOpenPlatformAPI(builder.toUriString());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
