package com.codehows.taelimbe.controller;

import com.codehows.taelimbe.constant.Role;
import com.codehows.taelimbe.dto.LoginDTO;
import com.codehows.taelimbe.dto.LoginResponseDTO;
import com.codehows.taelimbe.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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

        // ✅ 1. 인증된 사용자의 권한을 확인합니다.
        // 권한 문자열 가져옴 (ex: "ROLE_ADMIN")
        // ADMIN: 0, MANAGER: 1, USER: 2
        String roleName = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", "")) // ADMIN, MANAGER, USER
                .findFirst()
                .orElse("USER"); // 기본값 USER

        // enum으로 변환 → 숫자 level 꺼내기
        int roleLevel = Role.valueOf(roleName).getLevel();
        // ✅ 2. JWT 토큰을 발급합니다.
        String jwtToken = jwtService.generateToken(authentication.getName());

        // ✅ 3. 응답에 포함할 DTO를 생성합니다.
        LoginResponseDTO response = new LoginResponseDTO(jwtToken, roleLevel);

        return ResponseEntity.ok()
                .body(response);
//                  .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
//                   .build();
    }
}
