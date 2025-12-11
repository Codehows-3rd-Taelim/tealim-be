package com.codehows.taelimbe.robot.service;

import com.codehows.taelimbe.client.PuduAPIClient;
import com.codehows.taelimbe.robot.dto.RobotSyncRequestDTO;
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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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

    // 특정 매장의 로봇 동기화
    @Transactional
    public int syncRobots(RobotSyncRequestDTO req) {

        Store store = storeRepository.findById(req.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));
        Long shopId = store.getShopId();

        List<RobotDTO> robots = getRobotListByShop(shopId);

        int newCount = 0;

        for (RobotDTO dto : robots) {
            Robot existing = robotRepository.findBySn(dto.getSn()).orElse(null);
            boolean isNew = (existing == null);
            saveRobot(dto, store);
            if (isNew) {
                newCount++;
            }



        }
        return newCount;
    }

    // 모든 매장의 로봇 동기화
    @Transactional
    public int syncAllStoresRobots() {

        List<Store> stores = storeRepository.findAll();

        System.out.println("\n===== Sync All Stores Robots =====");
        System.out.println("Total Stores: " + stores.size());

        int newTotal = 0;

        for (Store store : stores) {
            System.out.println("\n--- Processing Store: " + store.getStoreId() + " ---");

            try {
                int count = syncRobots(RobotSyncRequestDTO.builder()
                        .storeId(store.getStoreId())
                        .build());
                newTotal += count;
                System.out.println("Store " + store.getStoreId() + " Synced: " + count + " robots");
            } catch (Exception e) {
                System.out.println("Error syncing store " + store.getStoreId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("\n===== All Stores Robot Sync Complete =====");
        System.out.println("Total New Robots: " + newTotal);
        System.out.println("==========================================\n");

        return newTotal;
    }


    // 매장별 로봇 목록 조회
    public List<RobotDTO> getRobotListFromDB(Long storeId) {
        return robotRepository.findAllByStore_StoreId(storeId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }


    // 로봇 정보 저장/업데이트
    @Transactional
    public Robot saveRobot(RobotDTO dto, Store store) {
        Robot robot = robotRepository.findBySn(dto.getSn())
                .orElseGet(() -> new Robot(dto.getSn(), dto.getMac(), store));

        robot.updateRobotInfo(
                dto.getNickname(),
                dto.getOnline(),
                dto.getBattery(),
                dto.getStatus(),
                dto.getProductCode(),
                dto.getSoftVersion(),
                dto.getIsCharging()
        );

        robot.changeStore(store);

        return robotRepository.save(robot);
    }


    public RobotDTO getRobotInfo(String sn, Long shopId) {
        String mac = null;
        String nickname = null;
        Boolean online = false;
        int battery = 0;
        int status = 0;
        String productCode = null;
        String softVersion = null;
        int isCharging = 0;

        JsonNode base = fetchRobotBySn(sn, shopId);
        if (base != null) mac = base.path("mac").asText(null);

        JsonNode detail = fetchRobotDetail(sn);
        if (detail != null) {
            nickname = detail.path("nickname").asText(null);
            battery = detail.path("battery").asInt();
            online = detail.path("online").asBoolean();
            status = detail.path("cleanbot").path("clean").path("status").asInt();
        }

        JsonNode chargeLog = fetchLatestChargeLog(sn, shopId);
        if (chargeLog != null) {
            productCode = chargeLog.path("product_code").asText(null);
            softVersion = chargeLog.path("soft_version").asText(null);
        }

        JsonNode chargeStatus = fetchRobotStatusV2(sn);
        if (chargeStatus != null) {
            isCharging = chargeStatus.path("is_charging").asInt();
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
                .isCharging(isCharging)
                .build();
    }


    public List<RobotDTO> getRobotListByShop(Long shopId) {
        List<JsonNode> list = fetchRobotListAll(shopId);
        List<RobotDTO> result = new ArrayList<>();

        for (JsonNode node : list) {
            String sn = node.path("sn").asText();
            result.add(getRobotInfo(sn, shopId));
        }

        return result;
    }


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
        long start = end - 60L * 60 * 24 * 90; // 최근 90일
        int limit = 20;

        JsonNode latest = null;

        try {
            for (int offset = 0; ; offset += limit) {

                String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                        .path("/data-board/v1/log/charge/query_list")
                        .queryParam("start_time", start)
                        .queryParam("end_time", end)
                        .queryParam("limit", limit)
                        .queryParam("offset", offset)
                        .queryParam("shop_id", shopId)
                        .toUriString();

                ResponseEntity<String> res = puduAPIClient.callPuduAPI(url, "GET");

                JsonNode arr = mapper.readTree(res.getBody())
                        .path("data").path("list");

                // 더 이상 데이터 없음 → 종료
                if (!arr.isArray() || arr.size() == 0) break;

                for (JsonNode node : arr) {

                    // SN 일치하는 것만 체크
                    if (!sn.equals(node.path("sn").asText())) continue;

                    if (latest == null) {
                        latest = node;
                        continue;
                    }

                    long t1 = convertTime(node.path("upload_time").asText());
                    long t2 = convertTime(latest.path("upload_time").asText());

                    if (t1 > t2) latest = node;
                }


                if (latest != null && offset > 40) break;
            }

        } catch (Exception ignored) {}

        return latest;
    }


    private JsonNode fetchRobotStatusV2(String sn) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/open-platform-service/v2/status/get_by_sn")
                    .queryParam("sn", sn)
                    .toUriString();


            ResponseEntity<String> res = puduAPIClient.callPuduAPI(url, "GET");

            return mapper.readTree(res.getBody()).path("data");

        } catch (Exception ignored) {}

        return null;
    }





    // 시리얼 번호로 로봇 조회
    public RobotDTO getRobotBySn(String sn) {
        return robotRepository.findBySn(sn)
                .map(this::convertToDto)
                .orElseThrow(() -> new IllegalArgumentException("Robot not found"));
    }

    private long convertTime(String timeStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime dt = LocalDateTime.parse(timeStr, formatter);
            return dt.atZone(ZoneId.of("Asia/Seoul")).toEpochSecond();
        } catch (Exception e) {
            return 0;
        }
    }



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
                .isCharging(robot.getIsCharging())
                .build();
    }
}