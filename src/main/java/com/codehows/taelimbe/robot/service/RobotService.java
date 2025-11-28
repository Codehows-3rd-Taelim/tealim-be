package com.codehows.taelimbe.robot.service;

import com.codehows.taelimbe.client.PuduAPIClient;
import com.codehows.taelimbe.robot.dto.RobotDTO;
import com.codehows.taelimbe.robot.entity.Robot;
import com.codehows.taelimbe.store.entity.Store;
import com.codehows.taelimbe.robot.repository.RobotRepository;
import com.codehows.taelimbe.store.repository.StoreRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RobotService {

    private final PuduAPIClient puduAPIClient;
    private final RobotRepository robotRepository;
    private final StoreRepository storeRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    // 1) API → DB 저장 (동기화)
    @Transactional
    public int syncRobotsByStoreId(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));
        Long shopId = store.getShopId();

        // ⚠ API 전용 메서드 (그대로 유지)
        List<RobotDTO> robots = getRobotListByShop(shopId);

        int cnt = 0;
        for (RobotDTO dto : robots) {
            saveRobot(dto, store);
            cnt++;
        }
        return cnt;
    }

    // 2) DB 조회 전용(list)
    public List<RobotDTO> getRobotListFromDB(Long storeId) {
        return robotRepository.findAllByStore_StoreId(storeId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 3) 단일 조회(DB + API 병합)
    public RobotDTO getRobotInfoByStoreId(String sn, Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));

        Long shopId = store.getShopId();

        RobotDTO api = getRobotInfo(sn, shopId);
        Robot robot = robotRepository.findBySn(sn).orElse(null);
        // DB 없을 때 API만 반환
        if (robot == null) return api;
        RobotDTO dto = convertToDto(robot);

        // API 최신값 덮어쓰기
        dto.setBattery(api.getBattery());
        dto.setOnline(api.isOnline());
        dto.setStatus(api.getStatus());
        dto.setProductCode(api.getProductCode());
        dto.setSoftVersion(api.getSoftVersion());

        return dto;
    }

    // 4) Robot 저장(DB 업데이트)
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

        robot.changeStore(store);

        return robotRepository.save(robot);
    }

    // 5) API 단일 상세 조회 정보 조립
    public RobotDTO getRobotInfo(String sn, Long shopId) {
        String mac = null;
        String nickname = null;
        boolean online = false;
        int battery = 0;
        int status = 0;
        String productCode = null;
        String softVersion = null;

        JsonNode base = fetchRobotBySn(sn, shopId);
        if (base != null) mac = base.path("mac").asText(null);

        JsonNode detail = fetchRobotDetail(sn);
        if (detail != null) {
            nickname = detail.path("nickname").asText(null);
            battery = detail.path("battery").asInt();
            online = detail.path("online").asBoolean();
            status = detail.path("cleanbot").path("clean").path("status").asInt();
        }

        JsonNode charge = fetchLatestChargeLog(sn, shopId);
        if (charge != null) {
            productCode = charge.path("product_code").asText(null);
            softVersion = charge.path("soft_version").asText(null);
        }

        return RobotDTO.builder()
                .sn(sn)
                .mac(mac)
                .nickname(nickname)
                .online(online)
                .battery(battery)
                .status(status)
                .productCode(productCode)
                .softVersion(softVersion)
                .build();
    }

    // 6) Shop 기준 전체 조회(API 전용)
    public List<RobotDTO> getRobotListByShop(Long shopId) {
        List<JsonNode> list = fetchRobotListAll(shopId);
        List<RobotDTO> result = new ArrayList<>();

        for (JsonNode node : list) {
            String sn = node.path("sn").asText();
            result.add(getRobotInfo(sn, shopId));
        }

        return result;
    }

    // 7) API 호출들
    private List<JsonNode> fetchRobotListAll(Long shopId) {
        List<JsonNode> list = new ArrayList<>();

        try {
            String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/data-open-platform-service/v1/api/robot")
                    .queryParam("limit", 100)
                    .queryParam("offset", 0)
                    .queryParam("shop_id", shopId)
                    .toUriString();

            ResponseEntity<String> res = puduAPIClient.callPuduAPI(url, "GET");

            JsonNode arr = mapper.readTree(res.getBody()).path("data").path("list");

            if (arr.isArray()) arr.forEach(list::add);

        } catch (Exception ignored) {}

        return list;
    }

    private JsonNode fetchRobotBySn(String sn, Long shopId) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/data-open-platform-service/v1/api/robot")
                    .queryParam("limit", 100)
                    .queryParam("offset", 0)
                    .queryParam("shop_id", shopId)
                    .toUriString();

            ResponseEntity<String> res = puduAPIClient.callPuduAPI(url, "GET");

            JsonNode nodes = mapper.readTree(res.getBody()).path("data").path("list");

            for (JsonNode n : nodes) {
                if (sn.equals(n.path("sn").asText())) return n;
            }

        } catch (Exception ignored) {}

        return null;
    }

    private JsonNode fetchRobotDetail(String sn) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/cleanbot-service/v1/api/open/robot/detail")
                    .queryParam("sn", sn)
                    .toUriString();

            ResponseEntity<String> res = puduAPIClient.callPuduAPI(url, "GET");

            return mapper.readTree(res.getBody()).path("data");

        } catch (Exception ignored) {}

        return null;
    }

    private JsonNode fetchLatestChargeLog(String sn, Long shopId) {
        long end = System.currentTimeMillis() / 1000;
        long start = end - 60L * 60 * 24 * 90;

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

            JsonNode arr = mapper.readTree(res.getBody()).path("data").path("list");

            if (arr.isArray() && arr.size() > 0) return arr.get(0);

        } catch (Exception ignored) {}

        return null;
    }

    // 8) Robot → DTO 변환 (DB)
    private RobotDTO convertToDto(Robot robot) {
        return RobotDTO.builder()
                .robotId(robot.getRobotId())
                .sn(robot.getSn())
                .mac(robot.getMac())
                .nickname(robot.getNickname())
                .online(robot.getOnline())
                .battery(robot.getBattery())
                .status(robot.getStatus())
                .productCode(robot.getProductCode())
                .softVersion(robot.getSoftVersion())
                .storeId(robot.getStore().getStoreId())
                .build();
    }
}
