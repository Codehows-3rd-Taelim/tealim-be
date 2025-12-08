# 1208_0920_robotStoreSync.md

## `src\main\java\com\codehows\taelimbe\robot\controller\RobotController.java`
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

## `src\main\java\com\codehows\taelimbe\robot\dto\RobotDTO.java`
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

## `src\main\java\com\codehows\taelimbe\robot\dto\RobotSyncRequestDTO.java`
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

## `src\main\java\com\codehows\taelimbe\robot\entity\Robot.java`
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

## `src\main\java\com\codehows\taelimbe\robot\repository\RobotRepository.java`
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

## `src\main\java\com\codehows\taelimbe\robot\service\RobotService.java`
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
     * Pudu API에서 샵의 모든 로봇 목록을 조회
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

## `src\main\java\com\codehows\taelimbe\store\constant\DeleteStatus.java`
```java
package com.codehows.taelimbe.store.constant;

public enum DeleteStatus {
    Y, N
}
```

## `src\main\java\com\codehows\taelimbe\store\constant\IndustryType.java`
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

## `src\main\java\com\codehows\taelimbe\store\controller\StoreController.java`
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

## `src\main\java\com\codehows\taelimbe\store\dto\StoreDTO.java`
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

## `src\main\java\com\codehows\taelimbe\store\entity\Industry.java`
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

## `src\main\java\com\codehows\taelimbe\store\entity\Store.java`
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

## `src\main\java\com\codehows\taelimbe\store\repository\IndustryRepository.java`
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

## `src\main\java\com\codehows\taelimbe\store\repository\StoreRepository.java`
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

## `src\main\java\com\codehows\taelimbe\store\service\StoreService.java`
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

## `src\main\java\com\codehows\taelimbe\sync\SyncController.java`
```java
package com.codehows.taelimbe.config;

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

    // 수동 동기화용
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
}
```

## `src\main\java\com\codehows\taelimbe\sync\SyncResultDTO.java`
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

## `src\main\java\com\codehows\taelimbe\sync\SyncScheduler.java`
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

    /**
     * 매일 0:00, 3:00, 6:00, 9:00, 12:00, 15:00, 18:00, 21:00에 매장 정보 동기화
     */
    @Scheduled(cron = "0 0 0/3 * * *", zone = "Asia/Seoul")
    public void syncStoresScheduled() {
        System.out.println("\n[SCHEDULER] Starting Store Sync at " + LocalDateTime.now());
        try {
            int count = storeService.syncAllStores();
            System.out.println("[SCHEDULER] Store Sync Completed: " + count + " stores");
        } catch (Exception e) {
            System.out.println("[SCHEDULER] Store Sync Failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 매일 0:30, 3:30, 6:30, 9:30, 12:30, 15:30, 18:30, 21:30에 로봇 정보 동기화
     */
    @Scheduled(cron = "0 30 0/3 * * *", zone = "Asia/Seoul")
    public void syncRobotsScheduled() {
        System.out.println("\n[SCHEDULER] Starting Robot Sync at " + LocalDateTime.now());
        try {
            int count = robotService.syncAllStoresRobots();
            System.out.println("[SCHEDULER] Robot Sync Completed: " + count + " robots");
        } catch (Exception e) {
            System.out.println("[SCHEDULER] Robot Sync Failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 매일 1:00, 4:00, 7:00, 10:00, 13:00, 16:00, 19:00, 22:00에 Report 동기화 (지난 3시간)
     */
    @Scheduled(cron = "0 0 1/3 * * *", zone = "Asia/Seoul")
    public void syncReportsScheduled() {
        System.out.println("\n[SCHEDULER] Starting Report Sync at " + LocalDateTime.now());
        try {
            // 현재 시간 기준 지난 3시간
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusHours(3);

            TimeRangeSyncRequestDTO req = TimeRangeSyncRequestDTO.builder()
                    .startTime(startTime)
                    .endTime(endTime)
                    .timezoneOffset(0)
                    .build();

            int count = puduReportService.syncAllStoresByTimeRange(req);
            System.out.println("[SCHEDULER] Report Sync Completed: " + count + " reports");
        } catch (Exception e) {
            System.out.println("[SCHEDULER] Report Sync Failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```