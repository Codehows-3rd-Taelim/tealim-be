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

    // 중복확인 눌렀을때
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