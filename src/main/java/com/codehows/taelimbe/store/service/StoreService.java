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
        int newCount = 0;
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

                    // 신규인지 판단해서 store 객체 준비
                    Store store;
                    if (existing.isPresent()) {
                        store = existing.get();        // 기존 store
                    } else {
                        store = new Store();           // 신규 store
                        newCount++;                    // 신규 카운트 증가
                    }

                    // 공통 업데이트
                    store.setShopId(shopId);
                    store.setShopName(shopName);
                    store.setIndustry(industry);

                    // 저장
                    storeRepository.save(store);

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

        System.out.println("New Stores Created: " + newCount + "\n");
        return newCount;
    }

    public List<Store> findAllStores() {
        return storeRepository.findAll();
    }

}