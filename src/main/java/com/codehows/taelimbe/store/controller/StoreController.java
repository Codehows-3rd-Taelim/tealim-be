package com.codehows.taelimbe.store.controller;

import com.codehows.taelimbe.store.dto.PaginationDTO;
import com.codehows.taelimbe.store.dto.StoreDTO;
import com.codehows.taelimbe.store.entity.Industry;
import com.codehows.taelimbe.store.repository.IndustryRepository;
import com.codehows.taelimbe.user.dto.UserResponseDTO;
import com.codehows.taelimbe.store.entity.Store;
import com.codehows.taelimbe.user.entity.User;
import com.codehows.taelimbe.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
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
    public ResponseEntity<PaginationDTO<StoreDTO>> getStore(
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size) {

        Page<StoreDTO> storePage = storeService.findStoresPage(page - 1, size);

        PaginationDTO<StoreDTO> response = new PaginationDTO<>();
        response.setContent(storePage.getContent());
        response.setPage(storePage.getNumber() + 1);
        response.setSize(storePage.getSize());
        response.setTotalPages(storePage.getTotalPages());
        response.setTotalElements(storePage.getTotalElements());

        return ResponseEntity.ok(response);
    }
    // 매장 직원 불러오기
    @GetMapping("/user")
    public ResponseEntity<List<UserResponseDTO>> getStoreUser(
            @RequestParam(value = "storeId", required = false) Long storeId) {

        List<User> users;

        if (storeId != null) {
            users = storeService.findUsersByStore(storeId);
        } else {
            users = storeService.findAllUsers();
        }

        List<UserResponseDTO> userDTOs = users.stream()
                .map(UserResponseDTO::fromEntity)
                .toList();

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
    @PostMapping("/sync")
    public ResponseEntity<String> syncAllStores() {
        int count = storeService.syncAllStores();
        return ResponseEntity.ok(count + "개 Store 저장/업데이트 완료");
    }

}