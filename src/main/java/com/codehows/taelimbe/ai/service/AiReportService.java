package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.ai.agent.ReportAgent;
import com.codehows.taelimbe.ai.config.ToolArgsContextHolder;
import com.codehows.taelimbe.ai.dto.AiReportDTO;
import com.codehows.taelimbe.ai.dto.ChatPromptRequest;
import com.codehows.taelimbe.ai.dto.ReportResult;
import com.codehows.taelimbe.ai.entity.AiReport;
import com.codehows.taelimbe.ai.repository.AiReportRepository;
import com.codehows.taelimbe.ai.repository.RawReportProjection;
import com.codehows.taelimbe.langchain.tools.ReportTools;
import com.codehows.taelimbe.user.constant.Role;
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
    public String startGenerateReport(ChatPromptRequest req, UserPrincipal user) {

        String conversationId = UUID.randomUUID().toString();

        sseService.createEmitter(conversationId);

        log.info("🚀 보고서 생성 시작 - conversationId: {}", conversationId);
        // 비동기로 AI 실행
        generateAsync(conversationId, req.getMessage(), user);

        return conversationId;
    }

    // 2. SSE 연결
    public SseEmitter connectSse(String conversationId, UserPrincipal user) {
        return sseService.createEmitter(conversationId);
    }

    // 3. 실제 AI 보고서 생성 (비동기)
    @Async
    public void generateAsync(String conversationId, String message, UserPrincipal user) {

        // 입력 검증
        if (message == null || message.isBlank()) {
            sseService.sendEvent(conversationId, "error", "보고서 요청 내용이 비어 있습니다.");
            sseService.complete(conversationId);

            notificationService.notifyAiReportFailed(user.userId(), "보고서 요청 내용이 비어 있습니다.");
            return; // DB 저장하지 않고 종료
        }

        try {
            String generatedDate = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"));

            String currentDate = LocalDate.now().toString();

            log.info("[AI Report] 보고서 생성 시작 - 사용자 요청: {}", message);

            // AI Agent가 알아서 날짜를 판단하고 Tool을 호출하도록 함
            StringBuilder aiResult = new StringBuilder();

            reportAgent.report(message, currentDate, generatedDate)
                    // 1. 토큰 단위 스트리밍
                    .onNext(token -> {
                        aiResult.append(token);
                        sseService.sendEvent(conversationId, "token", token); //실시간 전송
                    })
                    // 2. 완료 시점 처리
                    .onComplete(res -> { // res는 Response<AiMessage>
                        // Tool 호출에서 설정한 startDate, endDate 가져오기
                        String startDate = ToolArgsContextHolder.getToolArgs("startDate");
                        String endDate = ToolArgsContextHolder.getToolArgs("endDate");

                        // ReportResult 직접 생성
                        ReportResult result = new ReportResult(aiResult.toString(), startDate, endDate);

                        AiReport saved = saveReport(user, conversationId, message,
                                result.rawReport(), result.startDate(), result.endDate());

                        // 3. 최종 보고서 SSE 전송
                        sseService.sendEvent(
                                conversationId,
                                "savedReport",
                                AiReportDTO.from(saved)
                        );

                        //4. 완료 이벤트
                        sseService.sendEvent(conversationId, "done", "done");
                        sseService.complete(conversationId);
                        notificationService.notifyAiReportDone(user.userId(), conversationId);
                        log.info("[AI Report] 보고서 생성 완료 - ID: {}", saved.getAiReportId());
                    })
                    .onError(e -> {
                        log.error("AI Report Error", e);
                        sseService.sendEvent(conversationId, "error", Map.of(
                                "message", "보고서 생성 중 오류 발생: " + e.getMessage(),
                                "type", "AI_REPORT_ERROR"
                        ));
                        sseService.completeWithError(conversationId, e);

                        // 실패 알림
                        notificationService.notifyAiReportFailed(user.userId(), "AI 보고서 생성에 실패했습니다.");
                    })
                    .start();

        } catch (Exception e) {
            log.error("AI Report Exception", e);
            sseService.sendEvent(conversationId, "error", "보고서 생성 중 예외 발생");
            sseService.completeWithError(conversationId, e);

            notificationService.notifyAiReportFailed(user.userId(), e.getMessage());
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