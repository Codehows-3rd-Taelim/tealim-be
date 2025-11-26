package com.codehows.taelimbe.controller;

import com.codehows.taelimbe.dto.LoginDto;
import com.codehows.taelimbe.dto.LoginResponseDto;
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
    public ResponseEntity<?> login(@RequestBody LoginDto loginDto) {
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(loginDto.getLoginId(), loginDto.getPassword());

        Authentication authentication = authenticationManager.authenticate(token);

        // ✅ 1. 인증된 사용자의 권한을 확인합니다.
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN")); // DB에 저장된 관리자 역할 이름과 일치해야 함

        // ✅ 2. JWT 토큰을 발급합니다.
        String jwtToken = jwtService.generateToken(authentication.getName());

        // ✅ 3. 응답에 포함할 DTO를 생성합니다.
        LoginResponseDto response = new LoginResponseDto(jwtToken, isAdmin);

        return ResponseEntity.ok()
                .body(response);
//                  .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
//                   .build();
    }
}
