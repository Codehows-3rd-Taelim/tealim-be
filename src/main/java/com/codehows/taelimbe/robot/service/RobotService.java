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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RobotService {

    private final PuduAPIClient puduAPIClient;
    private final RobotRepository robotRepository;
    private final StoreRepository storeRepository;
    private final RobotAsyncProcessor processor;
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

        List<JsonNode> baseList = fetchRobotListAll(shopId);

        List<CompletableFuture<RobotDTO>> futures = baseList.stream()
                .map(baseNode -> processor.fetchRobotAsync(baseNode, shopId))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        int newCount = 0;

        for (CompletableFuture<RobotDTO> f : futures) {
            RobotDTO dto = f.join();
            if (dto == null) continue;

            Robot existing = robotRepository.findBySn(dto.getSn()).orElse(null);
            boolean isNew = (existing == null);

            saveRobot(dto, store);

            if (isNew) newCount++;
        }

        return newCount;
    }


    /**
     * DB에 저장된 모든 매장의 로봇을 Pudu API에서 조회하여 동기화
     * 관리자가 전체 매장의 로봇 정보를 한 번에 업데이트할 때 사용
     */

    @Transactional
    public int syncAllStoresRobots() {

        List<Store> stores = storeRepository.findAll();


        int newTotal = 0;

        for (Store store : stores) {

            try {
                int count = syncRobots(RobotSyncRequestDTO.builder()
                        .storeId(store.getStoreId())
                        .build());
                newTotal += count;

            } catch (Exception e) {

                e.printStackTrace();
            }
        }


        return newTotal;
    }

    // 매장별 CC1/MT1 로봇 조회
    public List<RobotDTO> getRobotsByStore(Long storeId) {
        List<Robot> robots = robotRepository.findAllByStoreAndCc1OrMt1(storeId);
        return robots.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 전체 매장의 CC1/MT1 로봇 조회
    public List<RobotDTO> getAllRobotsWithCc1OrMt1() {
        List<Robot> robots = robotRepository.findAll()
                .stream()
                .filter(r -> "CC1".equals(r.getProductCode()) || "MT1".equals(r.getProductCode()))
                .toList();

        return robots.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
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
                dto.getIsCharging()
        );

        robot.changeStore(store);

        return robotRepository.save(robot);
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
                .storeId(robot.getStore().getStoreId())
                .isCharging(robot.getIsCharging())
                .build();
    }
}