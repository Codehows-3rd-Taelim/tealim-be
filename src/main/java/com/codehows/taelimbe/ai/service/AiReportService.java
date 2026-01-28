package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.ai.agent.ReportAgent;
import com.codehows.taelimbe.ai.config.ToolArgsContextHolder;
import com.codehows.taelimbe.ai.dto.AiReportDTO;
import com.codehows.taelimbe.ai.dto.ChatPromptRequest;
import com.codehows.taelimbe.ai.dto.ReportStatistics;
import com.codehows.taelimbe.ai.entity.AiReport;
import com.codehows.taelimbe.ai.repository.AiReportRepository;
import com.codehows.taelimbe.ai.repository.RawReportProjection;
import com.codehows.taelimbe.user.entity.User;
import com.codehows.taelimbe.user.repository.UserRepository;
import com.codehows.taelimbe.user.security.UserPrincipal;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
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

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AiReportService {

    private final SseService sseService;
    private final ReportAgent reportAgent;
    private final AiReportRepository aiReportRepository;
    private final UserRepository userRepository;
    private final ReportStatisticsService reportStatisticsService;
    private final ReportMarkdownBuilder reportMarkdownBuilder;
    private final StreamingChatModel streamingChatModel;

    // Tool 미호출 재시도 최대 횟수
    private static final int MAX_RETRY = 3;

    // 1. 보고서 생성 시작
    public void startGenerateReport(
            String conversationId,
            ChatPromptRequest req,
            UserPrincipal principal
    ) {
        log.info("보고서 생성 시작 - conversationId: {}", conversationId);

        User user = userRepository.findById(principal.userId()).orElseThrow();

        String modifiedMessage = req.getMessage();

        Long storeId = null;
        String storeName = null;

        if (!principal.isAdmin()) {
            storeId = user.getStore().getStoreId();
            storeName = user.getStore().getShopName();
            modifiedMessage += "\n\n[매장명: " + storeName + "]";
        }

        generateAsync(
                conversationId,
                req.getMessage(),
                modifiedMessage,
                principal,
                storeId,
                storeName,
                0
        );
    }

    // 2. SSE 연결
    public SseEmitter connectSse(String conversationId, UserPrincipal user) {
        return sseService.createEmitter(conversationId);
    }

    // 3. 실제 AI 보고서 생성 (비동기)
    @Async
    public void generateAsync(
            String conversationId,
            String originalMessage,
            String aiMessage,
            UserPrincipal user,
            Long storeId,
            String storeName,
            int retryCount
    ) {

        ToolArgsContextHolder.bind(conversationId);

        ToolArgsContextHolder.setToolArgs("isAdmin", String.valueOf(user.isAdmin()));

        if (!user.isAdmin()) {
            ToolArgsContextHolder.setToolArgs("fixedStoreId", String.valueOf(storeId));
            ToolArgsContextHolder.setToolArgs("storeName", storeName);
        }

        if (aiMessage == null || aiMessage.isBlank()) {
            sseService.sendOnceAndComplete(
                    conversationId,
                    "fail",
                    Map.of("message", "보고서 요청 내용이 비어 있습니다.")
            );
            return;
        }

        try {
            String generatedDate = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"));
            String currentDate = LocalDate.now().toString();

            StringBuilder aiResult = new StringBuilder();

            // Phase 1: Tool 호출 (날짜 파싱 + 데이터 조회)
            reportAgent.report(aiMessage, currentDate, generatedDate, conversationId)
                    .onPartialResponse(chunk -> { /* Phase 1 출력 무시 */ })
                    .onCompleteResponse(res -> {
                        ToolArgsContextHolder.bind(conversationId);

                        // Tool 미호출 감지 → 자동 재호출
                        if (!isToolCalled()) {
                            if (retryCount < MAX_RETRY) {
                                log.warn("Tool not called. Retrying... ({}/{})",
                                        retryCount + 1, MAX_RETRY);

                                generateAsync(
                                        conversationId,
                                        originalMessage,
                                        aiMessage,
                                        user,
                                        storeId,
                                        storeName,
                                        retryCount + 1
                                );
                                return;
                            }

                            // 재시도 실패
                            sseService.sendOnceAndComplete(
                                    conversationId,
                                    "fail",
                                    Map.of("message", "데이터 조회 도구가 호출되지 않았습니다.")
                            );
                            return;
                        }

                        // Phase 2-3: DB 집계 쿼리로 통계 계산
                        String startDate = ToolArgsContextHolder.getToolArgs("startDate");
                        String endDate = ToolArgsContextHolder.getToolArgs("endDate");
                        String scope = ToolArgsContextHolder.getToolArgs("scope");
                        Long resolvedStoreId = "STORE".equals(scope)
                                ? Long.valueOf(ToolArgsContextHolder.getToolArgs("resolvedStoreId"))
                                : null;

                        LocalDateTime startDt = LocalDate.parse(startDate).atStartOfDay();
                        LocalDateTime endDt = LocalDate.parse(endDate).plusDays(1).atStartOfDay();

                        ReportStatistics stats = reportStatisticsService.compute(startDt, endDt, resolvedStoreId);

                        if (stats.getTotalJobCount() == 0) {
                            sseService.sendOnceAndComplete(
                                    conversationId,
                                    "fail",
                                    Map.of("message",
                                            String.format("%s ~ %s 기간에 작업 데이터가 없습니다.", startDate, endDate))
                            );
                            ToolArgsContextHolder.clear(conversationId);
                            return;
                        }

                        // Phase 4: 마크다운 조립 (플레이스홀더 포함)
                        String scopeSuffix = resolveScopeSuffix();
                        String template = reportMarkdownBuilder.build(
                                stats, generatedDate, startDate, endDate, scopeSuffix);

                        // Phase 5: LLM 인사이트 생성
                        generateInsight(conversationId, stats, template,
                                originalMessage, user, startDate, endDate);
                    })
                    .onError(e -> {
                        log.error("AI Report Error", e);
                        ToolArgsContextHolder.clear(conversationId);

                        sseService.sendOnceAndComplete(
                                conversationId,
                                "fail",
                                Map.of(
                                        "message", "보고서 생성 중 오류 발생",
                                        "detail", e.getMessage()
                                )
                        );
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

    /**
     * Phase 5: LLM에 통계 요약을 전달하여 인사이트 + 분석/권장사항만 생성
     */
    private void generateInsight(String conversationId, ReportStatistics stats,
                                  String template, String originalMessage,
                                  UserPrincipal user, String startDate, String endDate) {

        String summaryText = stats.toSummaryText();

        String prompt = """
                아래는 청소로봇 운영 통계 요약입니다. 두 가지 텍스트를 생성하세요.
                반드시 "---SEPARATOR---" 구분자로 나누어 출력하세요.

                [첫 번째] AI 운영 인사이트 요약 (2~3문장)
                - 수치를 직접 언급하지 말고, 운영 패턴과 가능성을 서술
                - "~일 수 있습니다", "~으로 보입니다" 등 가능성 표현 사용
                - 작업 횟수와 청소 범위의 관계, 작업 시간과 반복 가능성, 물 사용 여부와 구역 설정, 스케줄 설정 가능성 중 선택

                ---SEPARATOR---

                [두 번째] 분석 및 권장사항
                ### 작업 효율성
                - 평균 청소 시간, 평균 청소 면적, 시간당 청소 효율에 대한 분석

                ### 주의사항
                - 취소율 10% 이상이면 원인 분석 권장
                - 배터리 소모 평균 50% 이상이면 충전 스케줄 점검 권장
                - 물 소비량 비정상적으로 적으면 물 공급 시스템 점검 권장

                ### 개선 제안
                - 데이터 기반 구체적 개선안

                통계 요약:
                """ + summaryText;

        StringBuilder insightResult = new StringBuilder();

        streamingChatModel.chat(
                List.of(UserMessage.from(prompt)),
                new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        insightResult.append(partialResponse);
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse completeResponse) {
                        ToolArgsContextHolder.bind(conversationId);

                        String insightText = insightResult.toString();

                        // Phase 6: 플레이스홀더 교체
                        String insight = "";
                        String analysis = "";

                        if (insightText.contains("---SEPARATOR---")) {
                            String[] parts = insightText.split("---SEPARATOR---", 2);
                            insight = parts[0].trim();
                            analysis = parts.length > 1 ? parts[1].trim() : "";
                        } else {
                            // 구분자 없으면 전체를 인사이트로 사용
                            insight = insightText.trim();
                        }

                        String finalReport = template
                                .replace(ReportMarkdownBuilder.INSIGHT_PLACEHOLDER, insight)
                                .replace(ReportMarkdownBuilder.ANALYSIS_PLACEHOLDER, analysis);

                        // 저장 + SSE 전송
                        AiReport saved = saveReport(
                                user, conversationId, originalMessage,
                                finalReport, startDate, endDate);

                        sseService.sendOnceAndComplete(
                                conversationId,
                                "savedReport",
                                AiReportDTO.from(saved)
                        );

                        ToolArgsContextHolder.clear(conversationId);
                    }

                    @Override
                    public void onError(Throwable error) {
                        log.error("Insight generation error", error);
                        ToolArgsContextHolder.bind(conversationId);

                        // 인사이트 생성 실패해도 통계 보고서는 전송
                        String finalReport = template
                                .replace(ReportMarkdownBuilder.INSIGHT_PLACEHOLDER,
                                        "인사이트 생성에 실패했습니다.")
                                .replace(ReportMarkdownBuilder.ANALYSIS_PLACEHOLDER,
                                        "분석 생성에 실패했습니다.");

                        AiReport saved = saveReport(
                                user, conversationId, originalMessage,
                                finalReport, startDate, endDate);

                        sseService.sendOnceAndComplete(
                                conversationId,
                                "savedReport",
                                AiReportDTO.from(saved)
                        );

                        ToolArgsContextHolder.clear(conversationId);
                    }
                }
        );
    }

    // Tool 호출 여부 판단
    private boolean isToolCalled() {
        return ToolArgsContextHolder.getToolArgs("startDate") != null
                && ToolArgsContextHolder.getToolArgs("endDate") != null;
    }

    // scope suffix 결정
    private String resolveScopeSuffix() {
        String scope = ToolArgsContextHolder.getToolArgs("scope");
        String storeName = ToolArgsContextHolder.getToolArgs("storeName");

        if ("ALL".equals(scope)) {
            return "(전매장)";
        } else if ("STORE".equals(scope) && storeName != null) {
            return "(" + storeName + ")";
        }
        return "";
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

    public void deleteReport(Long reportId, UserPrincipal user) {
        AiReport report = aiReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("보고서 없음"));

        if (!report.getUser().getUserId().equals(user.userId())) {
            throw new RuntimeException("삭제 권한 없음");
        }

        aiReportRepository.delete(report);
    }
}
