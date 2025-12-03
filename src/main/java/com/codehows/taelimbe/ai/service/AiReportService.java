package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.ai.dto.AiReportDTO;
import com.codehows.taelimbe.ai.repository.AiReportRepository;
import com.codehows.taelimbe.user.constant.Role;
import com.codehows.taelimbe.user.entity.User;
import com.codehows.taelimbe.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiReportService {

    private final AiReportRepository aiReportRepository;
    private final UserRepository userRepository;

    // 현재 로그인한 유저 정보 가져오기
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
    }

    // 전체 리포트 조회 (권한에 따라 필터링)
    public List<AiReportDTO> getAllReports() {
        User currentUser = getCurrentUser();

        // ADMIN: 모든 리포트 조회
        if (currentUser.getRole() == Role.ADMIN) {
            return aiReportRepository.findAllByOrderByCreatedAtDesc()
                    .stream()
                    .map(AiReportDTO::from)
                    .collect(Collectors.toList());
        } else {
            // MANAGER, EMPLOYEE: 자기 매장 리포트만 조회
            Long storeId = currentUser.getStore().getStoreId();
            return aiReportRepository.findByStoreIdOrderByCreatedAtDesc(storeId)
                    .stream()
                    .map(AiReportDTO::from)
                    .collect(Collectors.toList());
        }

    }
}