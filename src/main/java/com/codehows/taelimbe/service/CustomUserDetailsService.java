package com.codehows.taelimbe.service;

import com.codehows.taelimbe.entity.User;
import com.codehows.taelimbe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String id) throws UsernameNotFoundException {
        // username = 사용자가 입력한 loginId (예: "user01")
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + id));

        // ✅ 권한 설정 (DB의 role 값 앞에 "ROLE_" 붙임)
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole()));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getId())           // 로그인 ID
                .password(user.getPw())           // DB에 저장된 암호화된 비밀번호
                .authorities(authorities)         // 권한 목록
                .build();
    }
}