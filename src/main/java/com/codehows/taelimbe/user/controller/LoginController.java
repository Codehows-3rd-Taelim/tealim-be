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

        // 2. 인증된 사용자 객체에서 storeId, userId를 추출합니다.
        Long storeId = null, userId = null;
        Object principal = authentication.getPrincipal();

        if (principal instanceof User) {
            User authenticatedUser = (User) principal;

            userId = authenticatedUser.getUserId();
            System.out.println("userId :  " + userId);

            // User 엔티티는 Store 엔티티를 가지고 있으므로, Store에서 storeId를 가져옵니다.
            if (authenticatedUser.getStore() != null) {
                storeId = authenticatedUser.getStore().getStoreId();
            }
        }
        // storeId가 null이면 0L 또는 적절한 기본값으로 설정 (LoinReponseDTO에 맞게 Integer 타입 요구에 맞춤)
        Long finalStoreId = storeId != null ? storeId : 0L;

        // 3. JWT 토큰을 발급합니다.
        String jwtToken = jwtService.generateToken(authentication.getName(), userId);


        // 4. 응답에 포함할 DTO를 생성합니다.
        LoginResponseDTO response = new LoginResponseDTO(jwtToken, roleLevel, finalStoreId, userId);

        return ResponseEntity.ok()
                .body(response);
//                  .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
//                   .build();
    }
}
