package com.codehows.taelimbe.controller;

import com.codehows.taelimbe.dto.UserDto;
import com.codehows.taelimbe.entity.Store;
import com.codehows.taelimbe.entity.User;
import com.codehows.taelimbe.repository.StoreRepository;
import com.codehows.taelimbe.repository.UserRepository;
import com.codehows.taelimbe.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/signup")
@RequiredArgsConstructor
public class SignUpController {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;

    @PostMapping
    public ResponseEntity<?> signup(@RequestBody @Valid UserDto userDto) {
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


}