package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.ai.agent.ReportAgent;
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
    private final ReportTools reportTools;

    // 1. 보고서 생성 시작 (SSE 연결X)
    public String startGenerateReport(ChatPromptRequest req, UserPrincipal user) {

        String conversationId = UUID.randomUUID().toString();

        // 비동기로 AI 실행
        generateAsync(conversationId, req.getMessage(), user);

        return conversationId;
    }

    // 2. SSE 연결
    public SseEmitter connectSse(String conversationId) {
        return sseService.createEmitter(conversationId);
    }

    // 3. 실제 AI 보고서 생성 (비동기)
    @Async
    protected void generateAsync(String conversationId, String message, UserPrincipal user) {

        // 입력 검증
        if (message == null || message.isBlank()) {
            sseService.sendEvent(conversationId, "error", "⚠️ 기간을 명확히 입력해주세요.");
            sseService.complete(conversationId);
            return; // DB 저장하지 않고 종료
        }

        try {
            String generatedDate = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"));

            String currentDate = LocalDate.now().toString();

            log.info("[AI Report] 보고서 생성 시작 - 사용자 요청: {}", message);

            // AI Agent가 알아서 날짜를 판단하고 Tool을 호출하도록 함
            StringBuilder aiResult = new StringBuilder();
            StringBuilder extractedDates = new StringBuilder();

            reportAgent.report(message, currentDate, generatedDate)
                    .onNext(token -> {
                        aiResult.append(token);
                        sseService.send(conversationId, token);
                    })
                    .onComplete(res -> {
                        // AI가 사용한 날짜를 추출 (Tool 호출 로그에서)
                        // 기본값으로 오늘 날짜 사용
                        String startDate = LocalDate.now().toString();
                        String endDate = LocalDate.now().toString();

                        // TODO: AI가 실제로 사용한 날짜를 추출하는 로직 추가 가능
                        // 현재는 기본값으로 저장

                        AiReport saved = saveReport(user, conversationId, message,
                                aiResult.toString(), startDate, endDate);

                        sseService.sendEvent(conversationId, "savedReport", AiReportDTO.from(saved));
                        sseService.sendEvent(conversationId, "done", "done");
                        sseService.complete(conversationId);

                        log.info("[AI Report] 보고서 생성 완료 - ID: {}", saved.getAiReportId());
                    })
                    .onError(e -> {
                        log.error("AI Report Error", e);
                        sseService.sendEvent(conversationId, "error", "보고서 생성 중 오류 발생: " + e.getMessage());
                        sseService.completeWithError(conversationId, e);
                    })
                    .start();
        } catch (IllegalArgumentException e) {
            // 기간이 명확하지 않은 경우
            sseService.sendEvent(conversationId, "error", e.getMessage());
            sseService.complete(conversationId);
        } catch (Exception e) {
            log.error("AI Report Exception", e);
            sseService.sendEvent(conversationId, "error", "보고서 생성 중 예외 발생");
            sseService.completeWithError(conversationId, e);
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
    public List<AiReportDTO> getAllReports(UserPrincipal principal) {

        User user = userRepository
                .findById(principal.userId())
                .orElseThrow();

        if (user.getRole() == Role.ADMIN) {
            return aiReportRepository.findAllByOrderByCreatedAtDesc()
                    .stream()
                    .map(AiReportDTO::from)
                    .toList();
        }

        Long storeId = user.getStore().getStoreId();

        return aiReportRepository.findByStoreIdOrderByCreatedAtDesc(storeId)
                .stream()
                .map(AiReportDTO::from)
                .toList();
    }

    public RawReportProjection getRawReport(Long reportId) {
        return aiReportRepository.findRawReportById(reportId)
                .orElseThrow(() -> new RuntimeException("보고서 없음"));
    }
}