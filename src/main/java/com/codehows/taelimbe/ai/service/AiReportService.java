package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.ai.agent.ReportAgent;
import com.codehows.taelimbe.ai.config.ToolArgsContextHolder;
import com.codehows.taelimbe.ai.dto.AiReportDTO;
import com.codehows.taelimbe.ai.dto.ChatPromptRequest;
import com.codehows.taelimbe.ai.entity.AiReport;
import com.codehows.taelimbe.ai.repository.AiReportRepository;
import com.codehows.taelimbe.ai.repository.RawReportProjection;
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
            UserPrincipal principal
    ) {
        log.info("🚀 보고서 생성 시작 - conversationId: {}", conversationId);

        User user = userRepository.findById(principal.userId())
                .orElseThrow();

        // role 판단
        boolean isAdmin = user.getRole().equals("ADMIN");
        ToolArgsContextHolder.setToolArgs("isAdmin", String.valueOf(isAdmin));

        String originalMessage = req.getMessage(); // DB저장용
        String aiMessage = originalMessage; // AI 전달용
        String shopName = null;
        String scope;

        // USER / MANAGER는 본인 매장만
        if (!isAdmin) {
            shopName = user.getStore().getShopName();
            scope = shopName;

            ToolArgsContextHolder.setToolArgs("fixedShopName", shopName);

            aiMessage = String.format(
                    "%s 매장에 대한 보고서를 생성하세요.\n\n사용자 요청: %s",
                    shopName,
                    originalMessage
            );

            log.info("USER 요청 → 매장명 강제 주입: {}", shopName);
        } else {
            scope = "전매장";
        }

        ToolArgsContextHolder.setToolArgs("scope", scope);

        generateAsync(conversationId, originalMessage, aiMessage, principal);
    }

    // 2. SSE 연결
    public SseEmitter connectSse(String conversationId, UserPrincipal user) {
        return sseService.createEmitter(conversationId);
    }

    // 3. 실제 AI 보고서 생성 (비동기)
    @Async
    public void generateAsync(String conversationId, String originalMessage, String aiMessage, UserPrincipal user) {

        if (aiMessage == null || aiMessage.isBlank()) {
            sseService.sendOnceAndComplete(
                    conversationId,
                    "fail",
                    Map.of("message", "보고서 요청 내용이 비어 있습니다.")
            );
            notificationService.notify(user.userId(), NotificationType.AI_REPORT_FAILED, "보고서 요청 내용이 비어 있습니다.");
            return;
        }

        try {
            String generatedDate = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"));
            String currentDate = LocalDate.now().toString();

            String scope = ToolArgsContextHolder.getToolArgs("scope");
            String shopName = ToolArgsContextHolder.getToolArgs("fixedShopName");
            if (shopName == null && !"전매장".equals(scope)) {
                shopName = scope;
            }

            StringBuilder aiResult = new StringBuilder();

            reportAgent.report(aiMessage, currentDate, generatedDate, scope, shopName)
                    .onNext(token -> {
                        aiResult.append(token);
//                        // 토큰 스트리밍 유지 (UI에서 안 쓰면 무시)
//                        sseService.sendEvent(conversationId, "token", token);
                    })
                    .onComplete(res -> {

                        String fullText = aiResult.toString();

                        // FAIL 판단
                        if (isFailResponse(fullText)) {
                            String failMessage = normalizeFailMessage(fullText);

                            log.warn("AI report fail detected. conversationId={}, message={}",
                                    conversationId, failMessage);

                            sseService.sendOnceAndComplete(
                                    conversationId,
                                    "fail",
                                    Map.of("message", failMessage)
                            );

                            notificationService.notify(
                                    user.userId(),
                                    NotificationType.AI_REPORT_FAILED,
                                    "AI가 보고서를 생성할 수 없어요. 입력을 다시 확인해 주세요."
                            );
                            return;
                        }

                        // 정상 플로우
                        String startDate = ToolArgsContextHolder.getToolArgs("startDate");
                        String endDate = ToolArgsContextHolder.getToolArgs("endDate");

                        AiReport saved = saveReport(
                                user,
                                conversationId,
                                originalMessage,
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

                        notificationService.notify(user.userId(), NotificationType.AI_REPORT_SUCCESS, "AI 보고서 생성이 완료되었습니다");
                    })
                    .onError(e -> {
                        log.error("AI Report Error", e);

                        sseService.sendOnceAndComplete(
                                conversationId,
                                "fail",
                                Map.of(
                                        "message", "보고서 생성 중 오류 발생",
                                        "detail", e.getMessage()
                                )
                        );

                        // 실패 알림
                        notificationService.notify(user.userId(), NotificationType.AI_REPORT_FAILED, "AI 보고서 생성에 실패했어요. 잠시 후 다시 시도해 주세요.");

                    })
                    .start();

        } catch (Exception e) {
            log.error("AI Report Exception", e);

            sseService.sendOnceAndComplete(
                    conversationId,
                    "fail",
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

    // fail 판단
    private boolean isFailResponse(String text) {
        return text.contains("할 수 없습니다")
                || text.contains("현재 사용 가능한 도구")
                || text.contains("대신")
                || text.contains("할까요?")
                || text.endsWith("?");
    }

    private String normalizeFailMessage(String text) {
        return text
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
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

    public void deleteReport(Long reportId, UserPrincipal user) {
        AiReport report = aiReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("보고서 없음"));

        // 본인 보고서만 삭제 가능
        if (!report.getUser().getUserId().equals(user.userId())) {
            throw new RuntimeException("삭제 권한 없음");
        }

        aiReportRepository.delete(report);
    }
}