package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.ai.repository.AiReportMetaProjection;
import com.codehows.taelimbe.ai.entity.AiReport;
import com.codehows.taelimbe.ai.repository.AiReportRepository;
import com.codehows.taelimbe.ai.repository.RawReportProjection;
import com.codehows.taelimbe.user.constant.Role;
import com.codehows.taelimbe.user.entity.User;
import com.codehows.taelimbe.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
    public List<AiReportMetaProjection> getAllReports() {
        User currentUser = getCurrentUser();
        Role role = currentUser.getRole();

        if (role == Role.ADMIN) {
            // 관리자 → 전체 보고서
            return aiReportRepository.findAllMetaOrderByCreatedAtDesc();
        }

        if (role == Role.MANAGER) {
            // 매장 담당자 → 같은 매장 + ADMIN 제외
            Long storeId = currentUser.getStore().getStoreId();
            return aiReportRepository.findMetaByStoreExcludingAdmin(storeId);
        }

        // 일반 사용자(USER) → 본인이 작성한 보고서만
        return aiReportRepository.findMetaByUserId(currentUser.getUserId());
    }

    public RawReportProjection getRawReport(Long reportId) {
        return aiReportRepository.findRawReportById(reportId)
                .orElseThrow(() -> new RuntimeException("보고서를 찾을 수 없습니다. ID: " + reportId));
    }



    @Transactional
    public AiReport saveReport(
            Long userId,
            String conversationId,
            String prompt,
            String output,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        AiReport report = AiReport.builder()
                .conversationId(conversationId)
                .startTime(startTime)
                .endTime(endTime)
                .createdAt(LocalDateTime.now())
                .rawMessage(prompt)
                .rawReport(output)
                .user(userRepository.findById(userId).orElseThrow(() -> new RuntimeException("사용자 없음")))
                .build();

        return aiReportRepository.save(report);  // 저장된 객체 반환
    }

}