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

    /**
     * 특정 매장의 로봇을 Pudu API에서 조회하여 DB에 저장/업데이트
     * @param req 매장 ID 포함 요청 정보
     * @return 저장된 로봇 개수
     */
    @Transactional
    public int syncRobots(RobotSyncRequestDTO req) {

        Store store = storeRepository.findById(req.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));
        Long shopId = store.getShopId();

        List<RobotDTO> robots = getRobotListByShop(shopId);

        int cnt = 0;
        for (RobotDTO dto : robots) {
            saveRobot(dto, store);
            cnt++;
        }
        return cnt;
    }

    /**
     * DB에 저장된 모든 매장의 로봇을 Pudu API에서 조회하여 동기화
     * 관리자가 전체 매장의 로봇 정보를 한 번에 업데이트할 때 사용
     * @return 저장된 전체 로봇 개수
     */
    @Transactional
    public int syncAllStoresRobots() {

        List<Store> stores = storeRepository.findAll();

        System.out.println("\n===== Sync All Stores Robots =====");
        System.out.println("Total Stores: " + stores.size());

        int totalCount = 0;

        for (Store store : stores) {
            System.out.println("\n--- Processing Store: " + store.getStoreId() + " ---");

            try {
                int count = syncRobots(RobotSyncRequestDTO.builder()
                        .storeId(store.getStoreId())
                        .build());
                totalCount += count;
                System.out.println("Store " + store.getStoreId() + " Synced: " + count + " robots");
            } catch (Exception e) {
                System.out.println("Error syncing store " + store.getStoreId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("\n===== All Stores Robot Sync Complete =====");
        System.out.println("Total Synced: " + totalCount);
        System.out.println("==========================================\n");

        return totalCount;
    }

    /**
     * DB에서 특정 매장의 로봇 목록을 조회
     * @param storeId 매장 ID
     * @return 해당 매장에 속한 로봇 목록
     */
    public List<RobotDTO> getRobotListFromDB(Long storeId) {
        return robotRepository.findAllByStore_StoreId(storeId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * DB의 로봇 정보와 API의 최신 상태 정보를 병합하여 조회
     * @param sn 로봇 시리얼 번호
     * @param storeId 매장 ID
     * @return 병합된 로봇 정보
     */
    public RobotDTO getRobotInfoByStoreId(String sn, Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));

        Long shopId = store.getShopId();

        RobotDTO api = getRobotInfo(sn, shopId);
        Robot robot = robotRepository.findBySn(sn).orElse(null);
        if (robot == null) return api;
        RobotDTO dto = convertToDto(robot);

        dto.setBattery(api.getBattery());
        dto.setOnline(api.getOnline());
        dto.setStatus(api.getStatus());
        dto.setProductCode(api.getProductCode());
        dto.setSoftVersion(api.getSoftVersion());

        return dto;
    }

    /**
     * 로봇 정보를 DB에 저장 또는 업데이트
     * @param dto 저장할 로봇 정보
     * @param store 로봇이 속한 매장
     * @return 저장된 로봇 엔티티
     */
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
                dto.getSoftVersion()
        );

        robot.changeStore(store);

        return robotRepository.save(robot);
    }

    /**
     * Pudu API에서 로봇의 모든 정보를 조회하여 DTO로 변환
     * @param sn 로봇 시리얼 번호
     * @param shopId 샵 ID
     * @return 조회된 로봇 정보
     */
    public RobotDTO getRobotInfo(String sn, Long shopId) {
        String mac = null;
        String nickname = null;
        Boolean online = false;
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

    /**
     * 샵에 속한 모든 로봇 목록을 Pudu API에서 조회
     * @param shopId 샵 ID
     * @return 로봇 정보 리스트
     */
    public List<RobotDTO> getRobotListByShop(Long shopId) {
        List<JsonNode> list = fetchRobotListAll(shopId);
        List<RobotDTO> result = new ArrayList<>();

        for (JsonNode node : list) {
            String sn = node.path("sn").asText();
            result.add(getRobotInfo(sn, shopId));
        }

        return result;
    }

    /**
     * Pudu API에서 샵의 모든 로봇 목록 조회 (기본 정보)
     * @param shopId 샵 ID
     * @return 로봇 JSON 노드 리스트
     */
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

    /**
     * Pudu API에서 시리얼 번호로 특정 로봇 조회
     * @param sn 로봇 시리얼 번호
     * @param shopId 샵 ID
     * @return 로봇 JSON 노드
     */
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

    /**
     * Pudu API에서 로봇의 상세 정보 조회 (별칭, 배터리, 온라인 상태 등)
     * @param sn 로봇 시리얼 번호
     * @return 로봇 상세 정보 JSON 노드
     */
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

    /**
     * Pudu API에서 로봇의 최신 충전 로그 조회 (제품 코드, 소프트웨어 버전 등)
     * @param sn 로봇 시리얼 번호
     * @param shopId 샵 ID
     * @return 최신 충전 로그 JSON 노드
     */
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

    /**
     * DB에서 시리얼 번호로 로봇 조회
     * @param sn 로봇 시리얼 번호
     * @return 로봇 정보 DTO
     */
    public RobotDTO getRobotBySn(String sn) {
        return robotRepository.findBySn(sn)
                .map(this::convertToDto)
                .orElseThrow(() -> new IllegalArgumentException("Robot not found"));
    }

    /**
     * 로봇 엔티티를 DTO로 변환
     * @param robot 로봇 엔티티
     * @return 변환된 로봇 DTO
     */
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