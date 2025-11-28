package com.codehows.taelimbe.controller;

import com.codehows.taelimbe.dto.UserResponseDTO;
import com.codehows.taelimbe.entity.Store;
import com.codehows.taelimbe.entity.User;
import com.codehows.taelimbe.service.CheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class CheckController {

    private final CheckService checkService;

    /**
     * @ResponseBody를 사용하면 @Controller에서도 JSON 응답을 반환할 수 있습니다.
     * /store?storeId=1 : storeId가 1인 매장만 조회
     * /store          : 모든 매장 조회
     *
     * @param storeId 선택적 매개변수 (Long 타입, 없을 경우 null)
     * @return 조회된 Store 엔티티 목록 (JSON)
     */
    @GetMapping("/store")
    @ResponseBody
    public ResponseEntity<List<Store>> checkStore(
            @RequestParam(value = "storeId", required = false) Long storeId) {

        // 비즈니스 로직을 서비스 계층으로 위임합니다.
        List<Store> stores = checkService.findStores(storeId);

        // HTTP 200 OK와 함께 조회된 매장 목록을 JSON으로 반환
        return ResponseEntity.ok(stores);
    }

    @GetMapping("/store/user")
    @ResponseBody
    public ResponseEntity<List<UserResponseDTO>> checkUser(
            @RequestParam(value = "storeId", required = false) Long storeId) {

        List<User> users = checkService.findUsers(storeId);

        // 💡 User 엔티티 목록을 UserResponseDTO 목록으로 변환
        List<UserResponseDTO> userDTOs = users.stream()
                .map(UserResponseDTO::fromEntity) // DTO의 fromEntity 메서드 사용
                .collect(Collectors.toList());

        // HTTP 200 OK와 함께 DTO 목록을 JSON으로 반환
        return ResponseEntity.ok(userDTOs);
    }
}