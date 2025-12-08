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


    @GetMapping("/list")
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