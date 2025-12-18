package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.ai.agent.ReportAgent;
import com.codehows.taelimbe.ai.dto.AiReportDTO;
import com.codehows.taelimbe.ai.dto.ChatPromptRequest;
import com.codehows.taelimbe.ai.entity.AiReport;
import com.codehows.taelimbe.ai.repository.AiReportRepository;
import com.codehows.taelimbe.ai.repository.RawReportProjection;
import com.codehows.taelimbe.user.entity.User;
import com.codehows.taelimbe.user.repository.UserRepository;
import com.codehows.taelimbe.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AiReportService {

    private final AiReportRepository aiReportRepository;
    private final UserRepository userRepository;
    private final SseService sseService;
    private final ReportAgent reportAgent;

    // 보고서 생성 시작
    public String startGenerateReport(ChatPromptRequest req, UserPrincipal user) {
        String conversationId = UUID.randomUUID().toString();
        generateAsync(conversationId, req.getMessage(), user);
        return conversationId;
    }

    // SSE 연결
    public SseEmitter connectSse(String conversationId) {
        return sseService.createEmitter(conversationId);
    }

    // 비동기 보고서 생성
    @Async
    protected void generateAsync(String conversationId, String message, UserPrincipal user) {
        if (message == null || message.isBlank()) {
            sseService.sendEvent(conversationId, "error", "⚠️ 기간을 명확히 입력해주세요.");
            sseService.complete(conversationId);
            return;
        }

        try {
            StringBuilder aiResult = new StringBuilder();

            reportAgent.report(message, LocalDate.now().toString(), LocalDate.now().toString())
                    .onNext(token -> {
                        aiResult.append(token);  // ← 꼭 추가
                        sseService.send(conversationId, token);
                    })
                    .onComplete(res -> {
                        AiReport saved = saveReport(user, conversationId, message, aiResult.toString());
                        sseService.sendEvent(conversationId, "savedReport", AiReportDTO.from(saved));
                        sseService.sendEvent(conversationId, "done", "done");
                        sseService.complete(conversationId);
                    })
                    .onError(e -> {
                        sseService.sendEvent(conversationId, "error", "보고서 생성 중 오류 발생: " + e.getMessage());
                        sseService.completeWithError(conversationId, e);
                    })
                    .start();
        } catch (Exception e) {
            sseService.sendEvent(conversationId, "error", "보고서 생성 중 예외 발생");
            sseService.completeWithError(conversationId, e);
        }
    }

    private AiReport saveReport(UserPrincipal user, String conversationId, String prompt, String result) {
        User entity = userRepository.findById(user.userId()).orElseThrow();
        return aiReportRepository.save(AiReport.builder()
                .conversationId(conversationId)
                .rawMessage(prompt)
                .rawReport(result)
                .startTime(LocalDate.now())
                .endTime(LocalDate.now())
                .createdAt(LocalDateTime.now())
                .user(entity)
                .build());
    }

    // Raw 보고서 조회
    public RawReportProjection getRawReport(Long reportId) {
        return aiReportRepository.findRawReportById(reportId)
                .orElseThrow(() -> new RuntimeException("보고서 없음"));
    }

    // 권한 기반 페이지네이션
    @Transactional(readOnly = true)
    public Page<AiReportDTO> getReportPage(UserPrincipal principal,
                                           int page, int size,
                                           String searchText,
                                           LocalDate startDate, LocalDate endDate) {
        User user = userRepository.findById(principal.userId()).orElseThrow();

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return aiReportRepository.findMetaPageByRole(
                        user.getUserId(),
                        user.getStore() != null ? user.getStore().getStoreId() : null,
                        user.getRole().name(),
                        searchText,
                        startDate,
                        endDate,
                        pageable
                )
                .map(AiReportDTO::fromProjection);
    }
}
