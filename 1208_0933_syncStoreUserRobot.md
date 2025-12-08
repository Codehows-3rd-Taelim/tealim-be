# 1208_0933_syncStoreUserRobot.md

## `src/main/java/com/codehows/taelimbe/sync/SyncController.java`
```java
package com.codehows.taelimbe.sync;

import com.codehows.taelimbe.pudureport.dto.TimeRangeSyncRequestDTO;
import com.codehows.taelimbe.pudureport.service.PuduReportService;
import com.codehows.taelimbe.robot.service.RobotService;
import com.codehows.taelimbe.store.service.StoreService;
import com.codehows.taelimbe.sync.SyncResultDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sync")
public class SyncController {

    private final StoreService storeService;
    private final RobotService robotService;
    private final PuduReportService puduReportService;

    // 관리자 수동 동기화용
    @PostMapping("/now")
    public ResponseEntity<SyncResultDTO> syncNow() {
        System.out.println("\n[MANUAL SYNC] Starting Full Sync at " + LocalDateTime.now());

        int storeCount = 0;
        int robotCount = 0;
        int reportCount = 0;
        StringBuilder errorMessage = new StringBuilder();

        try {
            // 1. Store 동기화
            System.out.println("[MANUAL SYNC] Starting Store Sync...");
            try {
                storeCount = storeService.syncAllStores();
                System.out.println("[MANUAL SYNC] Store Sync Completed: " + storeCount + " stores");
            } catch (Exception e) {
                System.out.println("[MANUAL SYNC] Store Sync Failed: " + e.getMessage());
                errorMessage.append("Store sync failed: ").append(e.getMessage()).append("\n");
            }

            // 2. Robot 동기화
            System.out.println("[MANUAL SYNC] Starting Robot Sync...");
            try {
                robotCount = robotService.syncAllStoresRobots();
                System.out.println("[MANUAL SYNC] Robot Sync Completed: " + robotCount + " robots");
            } catch (Exception e) {
                System.out.println("[MANUAL SYNC] Robot Sync Failed: " + e.getMessage());
                errorMessage.append("Robot sync failed: ").append(e.getMessage()).append("\n");
            }

            // 3. Report 동기화 (지난 3시간)
            System.out.println("[MANUAL SYNC] Starting Report Sync...");
            try {
                LocalDateTime endTime = LocalDateTime.now();
                LocalDateTime startTime = endTime.minusHours(3);

                TimeRangeSyncRequestDTO req = TimeRangeSyncRequestDTO.builder()
                        .startTime(startTime)
                        .endTime(endTime)
                        .timezoneOffset(0)
                        .build();

                reportCount = puduReportService.syncAllStoresByTimeRange(req);
                System.out.println("[MANUAL SYNC] Report Sync Completed: " + reportCount + " reports");
            } catch (Exception e) {
                System.out.println("[MANUAL SYNC] Report Sync Failed: " + e.getMessage());
                errorMessage.append("Report sync failed: ").append(e.getMessage()).append("\n");
            }

            System.out.println("\n[MANUAL SYNC] Full Sync Completed");
            System.out.println("[MANUAL SYNC] Stores: " + storeCount + ", Robots: " + robotCount + ", Reports: " + reportCount);

            SyncResultDTO result = SyncResultDTO.builder()
                    .storeCount(storeCount)
                    .robotCount(robotCount)
                    .reportCount(reportCount)
                    .totalCount(storeCount + robotCount + reportCount)
                    .success(errorMessage.length() == 0)
                    .errorMessage(errorMessage.toString())
                    .syncTime(LocalDateTime.now())
                    .build();

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.out.println("[MANUAL SYNC] Unexpected Error: " + e.getMessage());
            e.printStackTrace();

            SyncResultDTO result = SyncResultDTO.builder()
                    .storeCount(storeCount)
                    .robotCount(robotCount)
                    .reportCount(reportCount)
                    .totalCount(storeCount + robotCount + reportCount)
                    .success(false)
                    .errorMessage("Unexpected error: " + e.getMessage())
                    .syncTime(LocalDateTime.now())
                    .build();

            return ResponseEntity.internalServerError().body(result);
        }
    }

    // 유저 수동 동기화용

}
```
## `src/main/java/com/codehows/taelimbe/sync/SyncResultDTO.java`
```java
package com.codehows.taelimbe.sync;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncResultDTO {

    private Integer storeCount;
    private Integer robotCount;
    private Integer reportCount;
    private Integer totalCount;

    private Boolean success;
    private String errorMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime syncTime;
}
```
## `src/main/java/com/codehows/taelimbe/sync/SyncScheduler.java`
```java
package com.codehows.taelimbe.sync;

import com.codehows.taelimbe.pudureport.dto.TimeRangeSyncRequestDTO;
import com.codehows.taelimbe.pudureport.service.PuduReportService;
import com.codehows.taelimbe.robot.service.RobotService;
import com.codehows.taelimbe.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class SyncScheduler {

    private final StoreService storeService;
    private final RobotService robotService;
    private final PuduReportService puduReportService;

    // 매장 동기화 + 로봇 동기화
    // 시간 : 00:00 / 03:00 / 06:00 / 09:00 / 12:00 / 15:00 / 18:00 / 21:00
    @Scheduled(cron = "0 0 0/3 * * *", zone = "Asia/Seoul")
    public void syncStoresAndRobotsScheduled() {
        System.out.println("\n[SCHEDULER] === Store + Robot Sync Start === " + LocalDateTime.now());

        try {
            int storeCount = storeService.syncAllStores();
            System.out.println("[SCHEDULER] Store Sync Completed → " + storeCount + " stores");

            int robotCount = robotService.syncAllStoresRobots();
            System.out.println("[SCHEDULER] Robot Sync Completed → " + robotCount + " robots");

            System.out.println("[SCHEDULER] === Store + Robot Sync FINISHED ===\n");
        } catch (Exception e) {
            System.out.println("[SCHEDULER]  Store+Robot Sync FAILED : " + e.getMessage());
            e.printStackTrace();
        }
    }


    // 시간 : 01:00 / 04:00 / 07:00 / 10:00 / 13:00 / 16:00 / 19:00 / 22:00
    @Scheduled(cron = "0 0 1/3 * * *", zone = "Asia/Seoul")
    public void syncReportsScheduled() {
        System.out.println("\n[SCHEDULER] === Report Sync Start === " + LocalDateTime.now());

        try {
            LocalDateTime end = LocalDateTime.now();
            LocalDateTime start = end.minusHours(3);

            int count = puduReportService.syncAllStoresByTimeRange(
                    TimeRangeSyncRequestDTO.builder()
                            .startTime(start)
                            .endTime(end)
                            .timezoneOffset(0)
                            .build()
            );

            System.out.println("[SCHEDULER] Report Sync Completed → " + count + " reports");
            System.out.println("[SCHEDULER] === Report Sync FINISHED ===\n");

        } catch (Exception e) {
            System.out.println("[SCHEDULER]  Report Sync FAILED : " + e.getMessage());
            e.printStackTrace();
        }
    }
}

```
## `src/main/java/com/codehows/taelimbe/robot/controller/RobotController.java`
```java
package com.codehows.taelimbe.robot.controller;

import com.codehows.taelimbe.robot.dto.RobotSyncRequestDTO;
import com.codehows.taelimbe.robot.dto.RobotDTO;
import com.codehows.taelimbe.robot.service.RobotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/robot")
public class RobotController {

    private final RobotService robotService;

    /**
     * 특정 매장의 로봇 동기화
     * 요청한 매장 ID에 해당하는 로봇을 Pudu API에서 조회하여 DB에 저장/업데이트
     * @param req storeId 포함 요청 정보
     * @return 저장된 로봇 개수
     */
    @PostMapping("/sync")
    public ResponseEntity<String> syncRobots(@Valid @RequestBody RobotSyncRequestDTO req) {
        int count = robotService.syncRobots(req);
        return ResponseEntity.ok(count + "개 로봇 저장/업데이트 완료");
    }

    /**
     * 모든 매장의 로봇 동기화
     * DB에 저장된 모든 매장의 로봇을 Pudu API에서 조회하여 한 번에 동기화
     * 관리자가 전체 로봇 정보를 업데이트할 때 사용
     * @return 저장된 전체 로봇 개수
     */
    @PostMapping("/sync-all-stores")
    public ResponseEntity<String> syncAllStoresRobots() {
        int count = robotService.syncAllStoresRobots();
        return ResponseEntity.ok(count + "개 로봇 저장/업데이트 완료 (모든 매장)");
    }

    /**
     * 시리얼 번호로 로봇 조회
     * DB에서 특정 시리얼 번호의 로봇 정보를 조회
     * @param sn 로봇 시리얼 번호
     * @return 로봇 정보
     */
    @GetMapping("/{sn}")
    public ResponseEntity<RobotDTO> getRobot(@PathVariable String sn) {
        return ResponseEntity.ok(robotService.getRobotBySn(sn));
    }

    /**
     * 매장별 로봇 목록 조회
     * 특정 매장에 속한 모든 로봇 목록을 DB에서 조회
     * @param storeId 매장 ID
     * @return 해당 매장의 로봇 목록
     */
    @GetMapping("/list")
    public ResponseEntity<List<RobotDTO>> getAllRobots(@RequestParam Long storeId) {
        return ResponseEntity.ok(robotService.getRobotListFromDB(storeId));
    }
}
```
## `src/main/java/com/codehows/taelimbe/robot/dto/RobotDTO.java`
```java
package com.codehows.taelimbe.robot.dto;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
public class RobotDTO {

    private Long robotId;
    private String sn;
    private String mac;

    private String nickname;
    private Boolean online;
    private Integer battery;
    private Integer status;

    private String productCode;
    private String softVersion;

    private Long storeId;
}
```
## `src/main/java/com/codehows/taelimbe/robot/dto/RobotSyncRequestDTO.java`
```java
package com.codehows.taelimbe.robot.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RobotSyncRequestDTO {

    @NotNull(message = "storeId는 필수입니다")
    @Positive(message = "storeId는 양수여야 합니다")
    private Long storeId;
}
```
## `src/main/java/com/codehows/taelimbe/robot/entity/Robot.java`
```java
package com.codehows.taelimbe.robot.entity;

import com.codehows.taelimbe.store.entity.Store;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "robot")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Robot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "robot_id")
    private Long robotId;

    @Column(nullable = false, unique = true)
    private String sn;

    @Column(nullable = false, unique = true)
    private String mac;

    private String nickname;
    private Boolean online;
    private Integer battery;
    private Integer status;
    private String productCode;
    private String softVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    // ================= Constructor for Required Fields =================
    public Robot(String sn, String mac, Store store) {
        this.sn = sn;
        this.mac = mac;
        this.store = store;
    }

    // ================= Update Methods =================
    public void updateRobotInfo(String nickname, boolean online, int battery,
                                int status, String productCode, String softVersion) {
        this.nickname = nickname;
        this.online = online;
        this.battery = battery;
        this.status = status;
        this.productCode = productCode;
        this.softVersion = softVersion;
    }

    public void changeStore(Store store) {
        this.store = store;
    }
}
```
## `src/main/java/com/codehows/taelimbe/robot/repository/RobotRepository.java`
```java
package com.codehows.taelimbe.robot.repository;

import com.codehows.taelimbe.robot.entity.Robot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RobotRepository extends JpaRepository<Robot, Long> {

    Optional<Robot> findBySn(String sn);
    Optional<Robot> findByMac(String mac);

    // Store.storeId = storeId
    List<Robot> findAllByStore_StoreId(Long storeId);



}
```
## `src/main/java/com/codehows/taelimbe/robot/service/RobotService.java`
```java
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
```
## `src/main/java/com/codehows/taelimbe/store/constant/DeleteStatus.java`
```java
package com.codehows.taelimbe.store.constant;

public enum DeleteStatus {
    Y, N
}
```
## `src/main/java/com/codehows/taelimbe/store/constant/IndustryType.java`
```java
package com.codehows.taelimbe.store.constant;

// 업종 타입 Enum으로 이름 변경을 제안합니다.
public enum IndustryType {
    FOOD_BEVERAGE("식음료"),
    RETAIL("소매"),
    HOSPITALITY("접객"),
    INDUSTRIAL_FACILITY("산업 시설/창고/물류"), // 기존 문자열과 동일하게 유지
    HEALTHCARE("헬스케어"),
    TRANSPORTATION("운송 및 관련 서비스"),
    ENTERTAINMENT_SPORTS("엔터테인먼트 및 스포츠"),
    RESIDENTIAL_OFFICE("주거 및 오피스 빌딩"),
    EDUCATION("교육"),
    PUBLIC_SERVICE("공공 서비스");

    private final String industryName;

    IndustryType(String industryName) {
        this.industryName = industryName;
    }

    public String getIndustryName() {
        return industryName;
    }
}
```
## `src/main/java/com/codehows/taelimbe/store/controller/StoreController.java`
```java
package com.codehows.taelimbe.store.controller;

import com.codehows.taelimbe.store.dto.StoreDTO;
import com.codehows.taelimbe.store.entity.Industry;
import com.codehows.taelimbe.store.repository.IndustryRepository;
import com.codehows.taelimbe.user.dto.UserResponseDTO;
import com.codehows.taelimbe.store.entity.Store;
import com.codehows.taelimbe.user.entity.User;
import com.codehows.taelimbe.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/store")
public class StoreController {

    private final StoreService storeService;
    private final IndustryRepository industryRepository;

    /**
     * @ResponseBody를 사용하면 @Controller에서도 JSON 응답을 반환할 수 있습니다.
     * /store?storeId=1 : storeId가 1인 매장만 조회
     * /store          : 모든 매장 조회
     *
     * @param storeId 선택적 매개변수 (Long 타입, 없을 경우 null)
     * @return 조회된 Store 엔티티 목록 (JSON)
     */
    @GetMapping
    @ResponseBody
    public ResponseEntity<List<Store>> getStore(
            @RequestParam(value = "storeId", required = false) Long storeId) {

        // 비즈니스 로직을 서비스 계층으로 위임합니다.
        List<Store> stores = storeService.findStores(storeId);

        // HTTP 200 OK와 함께 조회된 매장 목록을 JSON으로 반환
        return ResponseEntity.ok(stores);
    }

    // 매장 직원 불러오기
    @GetMapping("/user")
    @ResponseBody
    public ResponseEntity<List<UserResponseDTO>> getStoreUser(
            @RequestParam(value = "storeId", required = false) Long storeId) {

        List<User> users = storeService.findUsers(storeId);

        // 💡 User 엔티티 목록을 UserResponseDTO 목록으로 변환
        List<UserResponseDTO> userDTOs = users.stream()
                .map(UserResponseDTO::fromEntity) // DTO의 fromEntity 메서드 사용
                .collect(Collectors.toList());

        // HTTP 200 OK와 함께 DTO 목록을 JSON으로 반환
        return ResponseEntity.ok(userDTOs);
    }

    // 업종 불러오기
    @GetMapping("/industry")
    public ResponseEntity<List<Industry>> getIndustry() {

        // 비즈니스 로직을 서비스 계층으로 위임합니다.
        List<Industry> industries = industryRepository.findAll();

        // HTTP 200 OK와 함께 조회된 매장 목록을 JSON으로 반환
        return ResponseEntity.ok(industries);
    }

    @PutMapping("/{storeId}")
    @ResponseBody // JSON 응답을 위해 추가
    public ResponseEntity<StoreDTO> updateStore(
                                                 @PathVariable Long storeId,
                                                 @RequestBody StoreDTO dto
    ) {
        StoreDTO updatedDto = storeService.updateStore(storeId, dto);
        return ResponseEntity.ok(updatedDto);
    }

    @DeleteMapping("/{storeId}")
    public void deleteStore(@PathVariable Long storeId) {
        storeService.deleteStore(storeId);
    }

    /**
     * Pudu API에서 Store 목록을 동기화
     *
     * @return 저장된 Store 개수
     */
    @PostMapping("api/store/sync")
    public ResponseEntity<String> syncAllStores() {
        int count = storeService.syncAllStores();
        return ResponseEntity.ok(count + "개 Store 저장/업데이트 완료");
    }

}
```
## `src/main/java/com/codehows/taelimbe/store/dto/StoreDTO.java`
```java
package com.codehows.taelimbe.store.dto;

import com.codehows.taelimbe.store.entity.Store;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreDTO {

    private Long storeId;

    private Long shopId;

    private String shopName;

    private Long industryId;

    // Entity -> DTO 변환을 위한 팩토리 메서드
    public static StoreDTO fromEntity(Store store) {
        StoreDTO dto = new StoreDTO();
        dto.setStoreId(store.getStoreId());
        dto.setShopId(store.getShopId());
        dto.setShopName(store.getShopName());

        // Industry 엔티티가 null이 아닐 경우 ID를 설정
        if (store.getIndustry() != null) {
            dto.setIndustryId(store.getIndustry().getIndustryId());
        } else {
            dto.setIndustryId(null);
        }

        return dto;
    }
}
```
## `src/main/java/com/codehows/taelimbe/store/entity/Industry.java`
```java
package com.codehows.taelimbe.store.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "industry")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Industry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "industry_id")
    private Long industryId;

    @Column(name = "industry_name", length = 255)
    private String industryName;

}
```
## `src/main/java/com/codehows/taelimbe/store/entity/Store.java`
```java
package com.codehows.taelimbe.store.entity;

import com.codehows.taelimbe.store.constant.DeleteStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "store")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(name = "shop_name", length = 20, nullable = false)
    private String shopName;

    @Enumerated(EnumType.STRING)
    @Column(name = "del_yn", nullable = false, length = 1)
    @Builder.Default
    private DeleteStatus delYn = DeleteStatus.N;

    @ManyToOne
    @JoinColumn(name = "industry_id")
    private Industry industry;

}
```
## `src/main/java/com/codehows/taelimbe/store/repository/IndustryRepository.java`
```java
package com.codehows.taelimbe.store.repository;

import com.codehows.taelimbe.store.entity.Industry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IndustryRepository extends JpaRepository<Industry, Long> {
    // IndustryRepository
    Optional<Industry> findByIndustryName(String industryName);

}
```
## `src/main/java/com/codehows/taelimbe/store/repository/StoreRepository.java`
```java
package com.codehows.taelimbe.store.repository;

import com.codehows.taelimbe.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByStoreId(Long storeId);

    // StoreRepository
    Optional<Store> findByShopId(Long shopId);


}
```
## `src/main/java/com/codehows/taelimbe/store/service/StoreService.java`
```java
package com.codehows.taelimbe.store.service;

import com.codehows.taelimbe.store.dto.StoreDTO;
import com.codehows.taelimbe.client.PuduAPIClient;
import com.codehows.taelimbe.store.entity.Industry;
import com.codehows.taelimbe.store.entity.Store;
import com.codehows.taelimbe.store.repository.IndustryRepository;
import com.codehows.taelimbe.user.entity.User;
import com.codehows.taelimbe.store.repository.StoreRepository;
import com.codehows.taelimbe.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;

@Service // 이 클래스를 서비스 빈으로 등록합니다.
@RequiredArgsConstructor
public class StoreService {

    // 리포지토리를 주입받습니다.
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final IndustryRepository industryRepository;

    private final ObjectMapper mapper;
    private final PuduAPIClient puduAPIClient;

    /**
     * storeId 유무에 따라 매장 목록 전체 또는 특정 매장을 조회합니다.
     *
     * @param storeId 선택적 매장 ID
     * @return 조회된 Store 엔티티 목록
     */
    public List<Store> findStores(Long storeId) {
        if (storeId != null) {
            // 1. storeId가 있는 경우: 해당 storeId만 조회
            Optional<Store> storeOptional = storeRepository.findById(storeId);

            // 조회 결과가 있으면 해당 매장만 리스트에 담아 반환하고, 없으면 빈 리스트 반환
            return storeOptional.map(List::of).orElse(List.of());
        } else {
            // 2. storeId가 없는 경우: 모든 매장 조회
            return storeRepository.findAll();
        }
    }

    public List<User> findUsers(Long storeId) {
        if (storeId != null) {
            return userRepository.findByStore_StoreId(storeId);
        } else {
            return userRepository.findAll();
        }
    }

    @Transactional // 트랜잭션 처리
    public StoreDTO updateStore(Long storeId, StoreDTO dto) {
        // 1. 기존 Store 엔티티 조회 및 존재 여부 확인
        Store target = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("업데이트 대상 매장(StoreId: " + storeId + ")을 찾을 수 없습니다."));

        // 2. DTO 정보를 Entity에 반영

        // 매장명 업데이트 (shopName이 DTO에 있을 경우)
        if (dto.getShopName() != null && !dto.getShopName().isEmpty()) {
            target.setShopName(dto.getShopName());
        }

        // shopId 업데이트 (shopId가 DTO에 있을 경우)
        if (dto.getShopId() != null) {
            target.setShopId(dto.getShopId());
        }

        // 3. Industry (업종) 업데이트 처리
        if (dto.getIndustryId() != null) {
            // DTO의 industryId로 Industry 엔티티 조회
            Industry industry = industryRepository.findById(dto.getIndustryId())
                    .orElseThrow(() -> new IllegalArgumentException("업종(IndustryId: " + dto.getIndustryId() + ")을 찾을 수 없습니다."));

            // Store 엔티티에 Industry 연결
            target.setIndustry(industry);
        } else {
            // industryId가 null이면, 업종 연결을 해제 (미지정 상태)
            target.setIndustry(null);
        }

        // 4. 업데이트된 엔티티 저장 (Transactional로 인해 자동 저장될 수 있으나 명시적으로 호출)
        Store updated = storeRepository.save(target);

        // 5. 업데이트된 엔티티를 DTO로 변환하여 반환
        return StoreDTO.fromEntity(updated);
    }

    @Transactional // 트랜잭션 처리
    public void deleteStore(Long storeId) {
        storeRepository.deleteById(storeId);
    }

    /**
     * Pudu API에서 Store 목록을 HMAC 인증으로 동기화하여 DB에 저장
     *
     * @return 저장된 Store 개수
     */
    @Transactional
    public int syncAllStores() {
        int totalCount = 0;
        int offset = 0;
        int limit = 100;
        boolean hasMore = true;

        System.out.println("\n===== Sync All Stores =====");

        while (hasMore) {

            try {
                // URL 생성
                String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                        .path("/data-open-platform-service/v1/api/shop")
                        .queryParam("limit", limit)
                        .queryParam("offset", offset)
                        .toUriString();

                //  HMAC 인증 API 호출
                ResponseEntity<String> res = puduAPIClient.callPuduAPI(url, "GET");

                JsonNode root = mapper.readTree(res.getBody());
                JsonNode list = root.path("data").path("list");

                // 데이터 없으면 종료
                if (!list.isArray() || list.size() == 0) {
                    hasMore = false;
                    break;
                }

                // 리스트 순회
                for (JsonNode node : list) {
                    Long shopId = node.path("shop_id").asLong();
                    String shopName = node.path("shop_name").asText();
                    String industryName = node.path("industry_name").asText();

                    // 기존 Store 조회
                    Optional<Store> existing = storeRepository.findByShopId(shopId);

                    // 🔵 Industry 조회 또는 생성
                    Industry industry = null;
                    if (industryName != null && !industryName.isEmpty()) {
                        industry = industryRepository.findByIndustryName(industryName)
                                .orElseGet(() -> industryRepository.save(
                                        Industry.builder()
                                                .industryName(industryName)
                                                .build()
                                ));
                    }

                    // Store 생성 또는 업데이트
                    Store store = existing.orElse(new Store());
                    store.setShopId(shopId);
                    store.setShopName(shopName);
                    store.setIndustry(industry);

                    storeRepository.save(store);
                    totalCount++;
                }

                // 페이지네이션
                if (list.size() < limit) {
                    hasMore = false;
                }

                offset += limit;

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                e.printStackTrace();
                hasMore = false;
            }
        }

        System.out.println("Total Saved: " + totalCount + "\n");
        return totalCount;
    }

}
```
## `src/main/java/com/codehows/taelimbe/user/config/AuthEntryPoint.java`
```java
package com.codehows.taelimbe.user.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;

@Component
public class AuthEntryPoint implements AuthenticationEntryPoint
{
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException)
            throws IOException, ServletException
    {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);  // SC_UNAUTHORIZED ==> 401 에러(인증불가)
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        out.println("인증에 실패했습니다. : " + authException.getMessage());
    }
}
```
## `src/main/java/com/codehows/taelimbe/user/config/JwtFilter.java`
```java
package com.codehows.taelimbe.user.config;

import com.codehows.taelimbe.user.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter
{
    private final JwtService jwtService;
    private final Servlet servlet;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // OPTIONS 요청(Preflight)은 JWT 검증 없이 바로 통과
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 필터 ==> 요청, 응답을 중간에서 가로챈 다음 ==> 필요한 동작을 수행
        // 1. 요청 헤더 (Authorization)에서 JWT 토큰을 꺼냄
        String jwtToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (jwtToken != null)
        {
            // 2. 꺼낸 토큰에서 유저 정보 추출
            String id = jwtService.parseToken(request);
            // 3. 추출된 유저 정보로 Authentication 을 만들어서 SecurityContext에 set
            if(id != null)
            {
                Authentication authentication =
                        new UsernamePasswordAuthenticationToken(id, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        // 마지막에 다음 필터를 호출
        filterChain.doFilter(request, response);
    }
}
```
## `src/main/java/com/codehows/taelimbe/user/config/SecurityConfig.java`
```java
package com.codehows.taelimbe.user.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final AuthEntryPoint authEntryPoint;
    private final JwtFilter jwtFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement
                        ((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling((ex) -> ex.authenticationEntryPoint(authEntryPoint));
        return http.build();
    }

    /**
     * CORS 설정 Bean 추가
     * PUT, DELETE 등의 요청에 대한 preflight(OPTIONS) 처리를 위해 필요
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 Origin (프론트엔드 URL)
        // 개발: http://localhost:5173
        // 운영: 실제 도메인
        configuration.setAllowedOriginPatterns(List.of("*\n")); // 또는 구체적인 URL 지정

        // PUT, DELETE 포함 모든 HTTP 메소드 허용
        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // Authorization 헤더 포함 모든 헤더 허용
        configuration.setAllowedHeaders(List.of("*\n"));

        // 인증 정보(쿠키, Authorization 헤더) 포함 허용
        configuration.setAllowCredentials(true);

        // preflight 요청 결과를 1시간 동안 캐싱
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();   //비밀번호 암호화
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception
    {
        return authConfig.getAuthenticationManager();
    }
}
```
## `src/main/java/com/codehows/taelimbe/user/constant/Role.java`
```java
package com.codehows.taelimbe.user.constant;

public enum Role {
    USER(1),
    MANAGER(2),
    ADMIN(3);

    private final int level;

    Role(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
```
## `src/main/java/com/codehows/taelimbe/user/controller/LoginController.java`
```java
package com.codehows.taelimbe.user.controller;

import com.codehows.taelimbe.user.constant.Role;
import com.codehows.taelimbe.user.dto.LoginDTO;
import com.codehows.taelimbe.user.dto.LoginResponseDTO;
import com.codehows.taelimbe.user.entity.User;
import com.codehows.taelimbe.user.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.authentication.AuthenticationManager;

@Controller
@RequiredArgsConstructor
public class LoginController {

    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO loginDto) {
        UsernamePasswordAuthenticationToken token = 
                new UsernamePasswordAuthenticationToken(loginDto.getId(), loginDto.getPw());

        Authentication authentication = authenticationManager.authenticate(token);

        // 1. 인증된 사용자의 권한을 확인합니다. ADMIN: 3, MANAGER: 2, USER: 1
        String roleName = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", "")) // ADMIN, MANAGER, USER
                .findFirst()
                .orElse("USER"); // 기본값 USER

        // enum으로 변환 → 숫자 level 꺼내기
        int roleLevel = Role.valueOf(roleName).getLevel();

        // 2. 인증된 사용자 객체에서 storeId를 추출합니다.
        Long storeId = null;
        Object principal = authentication.getPrincipal();

        if (principal instanceof User) {
            User authenticatedUser = (User) principal;

            // User 엔티티는 Store 엔티티를 가지고 있으므로, Store에서 storeId를 가져옵니다.
            if (authenticatedUser.getStore() != null) {
                storeId = authenticatedUser.getStore().getStoreId();
            }
        }
        // storeId가 null이면 0L 또는 적절한 기본값으로 설정 (LoinReponseDTO에 맞게 Integer 타입 요구에 맞춤)
        Long finalStoreId = storeId != null ? storeId : 0L;

        // 3. JWT 토큰을 발급합니다.
        String jwtToken = jwtService.generateToken(authentication.getName());

        // 4. 응답에 포함할 DTO를 생성합니다.
        LoginResponseDTO response = new LoginResponseDTO(jwtToken, roleLevel, finalStoreId);

        return ResponseEntity.ok()
                .body(response);
//                  .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
//                   .build();
    }
}
```
## `src/main/java/com/codehows/taelimbe/user/controller/UserController.java`
```java
package com.codehows.taelimbe.user.controller;

import com.codehows.taelimbe.user.dto.UserDTO;
import com.codehows.taelimbe.store.entity.Store;
import com.codehows.taelimbe.user.entity.User;
import com.codehows.taelimbe.store.repository.StoreRepository;
import com.codehows.taelimbe.user.repository.UserRepository;
import com.codehows.taelimbe.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody @Valid UserDTO userDto) {
        try {
            Store store = storeRepository.findByStoreId(userDto.getStoreId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 매장입니다."));

            User user = User.createUser(userDto, passwordEncoder, store);
            userService.saveUser(user);
            return ResponseEntity.ok("회원가입 성공");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    // 중복확인 눌렀때
    @GetMapping("/check_loginid")
    public ResponseEntity<?> checkLoginId(@RequestParam String id) {
        boolean exists = userRepository.existsById(id);
        return ResponseEntity.ok().body(Map.of("exists", exists));
    }

    // 직원 수정
    @PutMapping("/{userId}")
    @ResponseBody // JSON 응답을 위해 추가
    public ResponseEntity<UserDTO> updateStore( // 메서드 이름 수정 및 ResponseEntity<StoreDTO> 반환
                                                 @PathVariable Long userId,
                                                 @RequestBody UserDTO dto
    ) {
        UserDTO updatedDto = userService.updateUser(userId, dto);
        return ResponseEntity.ok(updatedDto);
    }

    // 직원 삭제
    @DeleteMapping("/{userId}")
    public ResponseEntity<String> deleteEmployee(@PathVariable Long userId) {
        try {
            // 서비스 계층에 삭제 로직 위임
            userService.deleteUser(userId);

            // 성공적으로 삭제되었음을 알리는 메시지 반환 (프론트엔드에서 alert에 사용 가능)
            return ResponseEntity.ok("직원이 성공적으로 삭제되었습니다.");

            // 또는 데이터 반환 없이 204 No Content 반환
            // return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {
            // 직원을 찾을 수 없을 때 (예: userId가 유효하지 않은 경우)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            // 그 외 서버 오류
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("직원 삭제 중 서버 오류가 발생했습니다.");
        }
    }
}
```
## `src/main/java/com/codehows/taelimbe/user/dto/UserDTO.java`
```java
package com.codehows.taelimbe.user.dto;

import com.codehows.taelimbe.user.constant.Role;
import com.codehows.taelimbe.user.entity.User;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import java.util.Base64;

@Getter
@Setter
public class UserDTO {

    private Long userId;
    @NotBlank(message = "ID는 필수 입력 값입니다.")
    private String id;

    // 수정시 비밀번호 안변경하면 null로 보내야해서 NotNull 사용X
    @Length(min=8, max=16, message = "비밀번호는 8자 이상,  16자 이하로 입력해주세요.")
    private String pw;

    @NotBlank(message = "이름은 필수 입력 값입니다.")
    private String name;

    @NotNull(message = "전화 번호는 필수 입력 값입니다.")
    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "전화번호는 하이픈(-)을 포함한 올바른 형식(예: 010-1234-5678)으로 입력해주세요.")
    private String phone;

    @NotEmpty(message = "이메일은 필수 입력 값입니다.")
    @Email(message = "이메일 형식으로 입력해주세요.")
    private String email;

    @NotNull(message = "권한은 필수 선택 값입니다.")
    private Role role;

    @NotNull(message = "업체 선택은 필수 선택 값입니다.")
    private Long storeId;

    public static UserDTO from(User user) {
        UserDTO dto = new UserDTO();
        dto.setUserId(user.getUserId());
        dto.setId(user.getId());
        dto.setPw(user.getPw());
        dto.setName(user.getName());
        dto.setPhone(user.getPhone());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setStoreId(user.getStore().getStoreId());
        return dto;
    }

    private static String decode(String encoded) {
        if (encoded == null) return null;
        return new String(Base64.getDecoder().decode(encoded));
    }

}
```
## `src/main/java/com/codehows/taelimbe/user/dto/UserResponseDTO.java`
```java
package com.codehows.taelimbe.user.dto;

import com.codehows.taelimbe.user.constant.Role;
import com.codehows.taelimbe.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponseDTO {

    private Long userId;
    private String id;
    private String name;
    private String phone;
    private String email;
    private Role role;
    private Long storeId;

    public static UserResponseDTO fromEntity(User user) {
        return UserResponseDTO.builder()
                .userId(user.getUserId())
                .id(user.getId())
                .name(user.getName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .role(user.getRole())
                // store 객체에서 storeId를 추출하여 DTO에 직접 매핑
                .storeId(user.getStore() != null ? user.getStore().getStoreId() : null)
                .build();
    }

}
```
## `src/main/java/com/codehows/taelimbe/user/entity/User.java`
```java
package com.codehows.taelimbe.user.entity;

import com.codehows.taelimbe.store.entity.Store;
import com.codehows.taelimbe.user.constant.Role;
import com.codehows.taelimbe.user.dto.UserDTO;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "id", length = 20, unique = true, nullable = false)
    private String id;

    @Column(name = "pw", length = 255, nullable = false)
    private String pw;

    @Column(name = "name", length = 20, nullable = false)
    private String name;

    @Column(name = "phone", length = 20, nullable = false)
    private String phone;

    @Column(name = "email", length = 50, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    public static User createUser(UserDTO dto, PasswordEncoder encoder, Store store) {
        return User.builder()
                .id(dto.getId())
                .pw(encoder.encode(dto.getPw()))
                .name(dto.getName())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .role(dto.getRole())
                .store(store)
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.role.toString()));
    }

    @Override
    public String getPassword() {
        return this.pw;
    }

    @Override
    public String getUsername() {
        return this.id;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
```
## `src/main/java/com/codehows/taelimbe/user/repository/UserRepository.java`
```java
package com.codehows.taelimbe.user.repository;

import com.codehows.taelimbe.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsById(String id);
    Optional<User> findById(String id);

    // 💡 Fetch Join을 사용하여 User를 로드할 때 Store 정보도 즉시 로드합니다.
    @Query("SELECT u FROM User u JOIN FETCH u.store WHERE u.id = :id")
    Optional<User> findByIdWithStore(String id);

    List<User> findByStore_StoreId(Long storeId);

}
```
## `src/main/java/com/codehows/taelimbe/user/service/CustomUserDetailsService.java`
```java
package com.codehows.taelimbe.user.service;

import com.codehows.taelimbe.user.entity.User;
import com.codehows.taelimbe.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String id) throws UsernameNotFoundException {
        // username = 사용자가 입력한 loginId (예: "user01")
        User user = userRepository.findByIdWithStore(id)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + id));

        return user;
    }

}
```
## `src/main/java/com/codehows/taelimbe/user/service/JwtService.java`
```java
package com.codehows.taelimbe.user.service;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    // 서버와 클라이언트가 주고 받는 토큰 ==> HTTP Header 내 Authorization 헤더값에 저장
    // 예) Authorization Bearer <토큰값>
    private static final String PREFIX = "Bearer ";

    private final long expirationTime;
    private final SecretKey signingKey;

    // 생성자를 통해 고정 키 주입
    public JwtService(
            @Value("${jwt.secret-key}") String secretKeyString,
            @Value("${jwt.expiration}") long expirationTime) {
        // 고정된 시크릿 키를 SecretKey 객체로 변환
        this.signingKey = Keys.hmacShaKeyFor(secretKeyString.getBytes(StandardCharsets.UTF_8));
        this.expirationTime = expirationTime;
    }

    // loginId(ID)를 받아서 JWT 생성
    public String generateToken(String loginId) {
        return Jwts.builder()
                .setSubject(loginId)
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // JWT를 받아서 id(ID)를 반환
    public String parseToken(HttpServletRequest request) {
        // 요청 헤더에서 Authorization 헤더값을 가져옴
        // 예) header = Bearer <토큰값>
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(PREFIX)) {
            try {
                JwtParser parser = Jwts.parserBuilder()
                        .setSigningKey(signingKey)
                        .build();

                String id = parser.parseClaimsJws(header.replace(PREFIX, ""))
                        .getBody()
                        .getSubject();

                return id;
            } catch (Exception e) {
                // 토큰 파싱 실패 시 null 반환
                return null;
            }
        }
        return null;
    }
}
```
## `src/main/java/com/codehows/taelimbe/user/service/UserService.java`
```java
package com.codehows.taelimbe.user.service;

import com.codehows.taelimbe.store.entity.Store;
import com.codehows.taelimbe.store.repository.StoreRepository;
import com.codehows.taelimbe.user.dto.UserDTO;
import com.codehows.taelimbe.user.entity.User;
import com.codehows.taelimbe.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final PasswordEncoder passwordEncoder;

    public void saveUser(User user){
        validateDuplicateUser(user);
        userRepository.save(user);
    }

    public void validateDuplicateUser(User user)
    {
        boolean loginIdExists = userRepository.existsById(user.getId());
        if (loginIdExists)
        {
            throw new IllegalStateException ("이미 사용 중인 아이디입니다.");
        }
    }

    @Transactional
    public UserDTO updateUser(Long userId, UserDTO dto) {
        // 1. 기존 User 엔티티 조회
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "업데이트 대상 직원(UserId: " + userId + ")을 찾을 수 없습니다."
                ));

        // 2. 아이디 변경 (중복 확인)
        if (dto.getId() != null && !dto.getId().equals(target.getId())) {
            // 중복 확인
            if (userRepository.existsById(dto.getId())) {
                throw new IllegalStateException("이미 사용 중인 아이디입니다.");
            }
            target.setId(dto.getId());
        }

        // 3. 비밀번호 변경 (입력된 경우만)
        if (dto.getPw() != null && !dto.getPw().isEmpty()) {
            String encodedPassword = passwordEncoder.encode(dto.getPw());
            target.setPw(encodedPassword);
        }

        // 4. 이름, 전화번호, 이메일 업데이트
        if (dto.getName() != null && !dto.getName().isEmpty()) {
            target.setName(dto.getName());
        }

        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) {
            target.setPhone(dto.getPhone());
        }

        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) {
            target.setEmail(dto.getEmail());
        }

        // 5. Store (매장) 업데이트 처리
        if (dto.getStoreId() != null) {
            Store store = storeRepository.findById(dto.getStoreId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "매장(StoreId: " + dto.getStoreId() + ")을 찾을 수 없습니다."
                    ));
            target.setStore(store);
        }

        // 6. Role (권한) 업데이트 처리
        if (dto.getRole() != null) {
            target.setRole(dto.getRole());
        }

        // 7. 업데이트된 엔티티 저장
        User updated = userRepository.save(target);

        // 8. 업데이트된 엔티티를 DTO로 변환하여 반환
        return UserDTO.from(updated);
    }

    @Transactional
    public void deleteUser(Long userId) {
        userRepository.findById(userId)
                .ifPresentOrElse(
                        user -> userRepository.delete(user),
                        () -> { throw new IllegalArgumentException("해당 ID의 직원을 찾을 수 없습니다: " + userId); }
                );
    }
}
```