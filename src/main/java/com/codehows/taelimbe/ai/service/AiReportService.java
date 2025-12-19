package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.ai.agent.ReportAgent;
import com.codehows.taelimbe.ai.config.ToolArgsContextHolder;
import com.codehows.taelimbe.ai.dto.AiReportDTO;
import com.codehows.taelimbe.ai.dto.ChatPromptRequest;
import com.codehows.taelimbe.ai.entity.AiReport;
import com.codehows.taelimbe.ai.repository.AiReportRepository;
import com.codehows.taelimbe.ai.repository.RawReportProjection;
import com.codehows.taelimbe.langchain.tools.ReportTools;
import com.codehows.taelimbe.notification.constant.NotificationType;
import com.codehows.taelimbe.notification.service.NotificationService;
import com.codehows.taelimbe.user.entity.User;
import com.codehows.taelimbe.user.repository.UserRepository;
import com.codehows.taelimbe.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AiReportService {

    private final SseService sseService;
    private final ReportAgent reportAgent;
    private final AiReportRepository aiReportRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;


    // 1. 보고서 생성 시작
    public void startGenerateReport(
            String conversationId,
            ChatPromptRequest req,
            UserPrincipal user
    ) {
        log.info("🚀 보고서 생성 시작 - conversationId: {}", conversationId);
        generateAsync(conversationId, req.getMessage(), user);
    }

    // 2. SSE 연결
    public SseEmitter connectSse(String conversationId, UserPrincipal user) {
        return sseService.createEmitter(conversationId);
    }

    // 3. 실제 AI 보고서 생성 (비동기)
    @Async
    public void generateAsync(String conversationId, String message, UserPrincipal user) {

        if (message == null || message.isBlank()) {
            sseService.sendOnceAndComplete(
                    conversationId,
                    "error",
                    Map.of("message", "보고서 요청 내용이 비어 있습니다.")
            );
            notificationService.notifyAiReportFailed(user.userId(), "보고서 요청 내용이 비어 있습니다.");
            return;
        }

        try {
            String generatedDate = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"));

            String currentDate = LocalDate.now().toString();

            StringBuilder aiResult = new StringBuilder();
            StringBuilder extractedDates = new StringBuilder();

            reportAgent.report(message, currentDate, generatedDate)
                    .onNext(token -> {
                        aiResult.append(token);
//                        // 토큰 스트리밍 유지 (UI에서 안 쓰면 무시)
//                        sseService.sendEvent(conversationId, "token", token);
                    })
                    .onComplete(res -> {

                        String startDate = ToolArgsContextHolder.getToolArgs("startDate");
                        String endDate = ToolArgsContextHolder.getToolArgs("endDate");

                        AiReport saved = saveReport(
                                user,
                                conversationId,
                                message,
                                aiResult.toString(),
                                startDate,
                                endDate
                        );

                        // 여기서 한 번만 보내고 종료
                        sseService.sendOnceAndComplete(
                                conversationId,
                                "savedReport",
                                AiReportDTO.from(saved)
                        );

                        notificationService.notifyAiReportDone(user.userId(), conversationId);
                    })
                    .onError(e -> {
                        log.error("AI Report Error", e);

                        sseService.sendOnceAndComplete(
                                conversationId,
                                "error",
                                Map.of(
                                        "message", "보고서 생성 중 오류 발생",
                                        "detail", e.getMessage()
                                )
                        );

                        notificationService.notifyAiReportFailed(user.userId(), "AI 보고서 생성 실패");
                    })
                    .start();

        } catch (Exception e) {
            log.error("AI Report Exception", e);

            sseService.sendOnceAndComplete(
                    conversationId,
                    "error",
                    "보고서 생성 중 예외 발생"
            );
        }
    }

    // 보고서 저장
    private AiReport saveReport(UserPrincipal user, String conversationId, String prompt, String result,
                                String startDate, String endDate) {

        User entity = userRepository.findById(user.userId()).orElseThrow();

        return aiReportRepository.save(
                AiReport.builder()
                        .conversationId(conversationId)
                        .rawMessage(prompt)
                        .rawReport(result)
                        .startTime(startDate != null ? LocalDate.parse(startDate) : null)
                        .endTime(endDate != null ? LocalDate.parse(endDate) : null)
                        .createdAt(LocalDateTime.now())
                        .user(entity)
                        .build()
        );
    }

    // 보고서 목록 조회
    public List<AiReportDTO> getAllReports(UserPrincipal user) {

        return aiReportRepository.findAllByUser_UserIdOrderByCreatedAtDesc(user.userId())
                .stream()
                .map(AiReportDTO::from)
                .toList();
    }

    public RawReportProjection getRawReport(Long reportId) {
        return aiReportRepository.findRawReportById(reportId)
                .orElseThrow(() -> new RuntimeException("보고서 없음"));
    }
}