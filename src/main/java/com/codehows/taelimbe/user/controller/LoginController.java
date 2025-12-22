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
        try {
            UsernamePasswordAuthenticationToken token =
                    new UsernamePasswordAuthenticationToken(loginDto.getId(), loginDto.getPw());

            Authentication authentication =
                    authenticationManager.authenticate(token);

            // ↓↓↓ 기존 코드 그대로 ↓↓↓

            String roleName = authentication.getAuthorities().stream()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .findFirst()
                    .orElse("USER");

            int roleLevel = Role.valueOf(roleName).getLevel();

            Long storeId = null, userId = null;
            Object principal = authentication.getPrincipal();

            if (principal instanceof User user) {
                userId = user.getUserId();
                if (user.getStore() != null) {
                    storeId = user.getStore().getStoreId();
                }
            }

            String jwtToken = jwtService.generateToken(authentication.getName(), userId);
            LoginResponseDTO response =
                    new LoginResponseDTO(jwtToken, roleLevel, storeId != null ? storeId : 0L, userId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity
                    .status(401)
                    .body("아이디 또는 비밀번호가 잘못 되었습니다. \n 아이디와 비밀번호를 정확히 입력해 주세요");
        }
    }
}
