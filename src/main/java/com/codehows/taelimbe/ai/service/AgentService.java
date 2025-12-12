package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.ai.dto.AiReportDTO;
import com.codehows.taelimbe.ai.dto.ChatPromptRequest;
import com.codehows.taelimbe.ai.repository.MapFailStatsProjection;
import com.codehows.taelimbe.ai.repository.MapStatsProjection;
import com.codehows.taelimbe.ai.repository.ReportSummaryProjection;
import com.codehows.taelimbe.ai.entity.AiReport;
import com.codehows.taelimbe.ai.util.DateRangeParser;
import com.codehows.taelimbe.langchain.Agent;
import com.codehows.taelimbe.report.repository.ReportRepository;
import com.codehows.taelimbe.robot.entity.Robot;
import com.codehows.taelimbe.robot.repository.RobotRepository;
import com.codehows.taelimbe.store.entity.Store;
import com.codehows.taelimbe.user.entity.User;
import com.codehows.taelimbe.user.repository.UserRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

/**
 * AI 에이전트와의 대화 로직을 캡슐화하는 서비스 클래스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentService {
    private final SseService sseService;
    private final AiChatService aiChatService;

    @Qualifier("reportAgent")
    private final Agent reportAgent;

    @Qualifier("chatAgent")
    private final Agent chatAgent;

    private final AiReportService aiReportService;
    private final UserRepository userRepository;
    private final RobotRepository robotRepository;
    private final ReportRepository reportRepository;
    private final IntentClassificationService intentService;

    /**
     * 현재 로그인 유저 조회
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        return userRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
    }

    @Async
    public void process(String conversationId, String message, Long userId) {

        // 1) 사용자 메시지 저장
        aiChatService.saveUserMessage(conversationId, userId, message);

        // 2) TokenStream 가져오기
        TokenStream stream = chatAgent.chat(message, conversationId);

        StringBuilder aiBuilder = new StringBuilder();

        // 3) 토큰 스트리밍 시작
        stream.onNext(token -> {
                    aiBuilder.append(token);
                    sseService.send(conversationId, token);
                })
                .onComplete(finalResponse -> {
                    aiChatService.saveAiMessage(conversationId, userId, aiBuilder.toString());
                })
                .onError(e -> {
                    log.error("AI 스트림 오류", e);
                })
                .start();  
    }

    public SseEmitter report(ChatPromptRequest req, Long userId) {

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        // 현재 스레드에 사용자 이름을 설정하여, 도구 호출 등에서 사용자 컨텍스트를 활용할 수 있도록 합니다.
        // 대화 ID가 요청에 포함되어 있지 않다면 새로운 ID를 생성합니다.
        String convId = (req.getConversationId() == null || req.getConversationId().isBlank())
                ? UUID.randomUUID().toString()
                : req.getConversationId();

        // 날짜 범위 추출
        LocalDateTime[] range = DateRangeParser.extractDateRange(req.getMessage());
        LocalDateTime startTime = range[0];
        LocalDateTime endTime = range[1];

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        // 보고서 관리 기간
        String periodText = startTime.format(fmt) + " ~ " + endTime.format(fmt);

        // 보고서 작성일 (현재 날짜)
        String generatedDate = now.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"));

        // 현재 로그인 유저 가져오기
        User currentUser = getCurrentUser();
        Store store = currentUser.getStore();

        // 고객사명
        String customerName = store.getShopName();

        // 매장(storeId)에 연결된 모든 로봇의 모델(productCode) 조회
        List<String> deviceNames = robotRepository
                .findAllByStore_StoreId(store.getStoreId())
                .stream()
                .map(Robot::getProductCode) // "CC1", "MT1"
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        String deviceNameText = deviceNames.isEmpty()
                ? "청소로봇"
                : String.join(", ", deviceNames);

        try {
            String userMsg = req.getMessage();

            // 1차 키워드 필터
            if (!intentService.isLikelyReportByKeyword(userMsg)) {
                emitter.send(SseEmitter.event()
                        .name("reportInfo")
                        .data("보고서 요청이 아닌 것으로 판단되었습니다. (키워드 없음)"));
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
                return emitter;
            }

            // 2차 LLM 의도 분류
            String result = intentService.classifyByLLM(userMsg);

            if (!"REPORT".equals(result)) {
                emitter.send(SseEmitter.event()
                        .name("reportInfo")
                        .data("보고서 요청이 아닌 것으로 판단되었습니다. (분류: " + result + ")"));
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
                return emitter;
            }

        } catch (Exception e) {
            emitter.completeWithError(new RuntimeException("분류 중 오류 발생"));
            return emitter;
        }



        ReportSummaryProjection summary = reportRepository
                .summarizeReportByTimeRange(startTime, endTime)
                .orElse(null);

        // 실패 / 중단 통계 계산
        List<MapFailStatsProjection> failStats =
                reportRepository.findFailStatsByDateRange(startTime, endTime);

        // 실패 총합
        long failedCount = failStats.stream()
                .mapToLong(MapFailStatsProjection::getFailCount)
                .sum();

        // 실패율 가장 높은 층
        String mostFailedMapName = failStats.stream()
                .max((a, b) -> Long.compare(a.getFailCount(), b.getFailCount()))
                .map(MapFailStatsProjection::getMapName)
                .orElse("없음");


        List<MapStatsProjection> mapStats =
                reportRepository.summarizeMapStatsByTimeRange(startTime, endTime);

        List<Object[]> statusCounts =
                reportRepository.countStatusByTimeRange(startTime, endTime);



        long totalCleanTime = 0L;
        double totalTaskArea = 0.0;
        double totalCleanArea = 0.0;
        long totalTaskCount = 0L;
        long totalCostWater = 0L;
        long totalCostBattery = 0L;

        boolean summaryHasData = false;

        if (summary != null) {
            Number tct = summary.getTotalCleanTime();
            Number tta = summary.getTotalTaskArea();
            Number tca = summary.getTotalCleanArea();
            Number ttc = summary.getTotalTaskCount();
            Number tcw = summary.getTotalCostWater();
            Number tcb = summary.getTotalCostBattery();

            totalCleanTime = (tct != null) ? tct.longValue() : 0L;
            totalTaskArea = (tta != null) ? tta.doubleValue() : 0.0;
            totalCleanArea = (tca != null) ? tca.doubleValue() : 0.0;
            totalTaskCount = (ttc != null) ? ttc.longValue() : 0L;
            totalCostWater = (tcw != null) ? tcw.longValue() : 0L;
            totalCostBattery = (tcb != null) ? tcb.longValue() : 0L;


            summaryHasData = (totalTaskCount > 0L);
        }

        if (!summaryHasData) {
            try {
                emitter.send(
                        SseEmitter.event()
                                .name("reportInfo")
                                .data("요청하신 기간 (" + periodText + ") 동안 로봇 운영 데이터가 없어 보고서를 생성할 수 없습니다.")
                );
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }


        StringBuilder dataForAi = new StringBuilder();


        double avgEfficiency = 0.0;
        if (totalCleanTime > 0) {
            double hours = totalCleanTime / 3600.0; // 초→시간 변환
            avgEfficiency = hours > 0 ? (totalCleanArea / hours) : 0.0;
        }

// 장비 운영 요약
        dataForAi.append("### 1. 장비 운영 요약 데이터 (총괄 수치)\n");
        dataForAi.append(String.format("총 청소 작업시간: %.2f 시간 (%.2f 분)\n",
                totalCleanTime / 3600.0, totalCleanTime / 60.0));
        dataForAi.append(String.format("총 계획 청소 면적: %.2f m²\n", totalTaskArea));
        dataForAi.append(String.format("총 실제 청소 면적: %.2f m²\n", totalCleanArea));
        dataForAi.append(String.format("총 작업 수: %d 회\n", totalTaskCount));
        dataForAi.append(String.format("총 물 소비량: %.2f L\n", totalCostWater / 1000.0));
        dataForAi.append(String.format("평균 업무 효율성: %.2f m²/h\n", avgEfficiency));
        dataForAi.append(String.format("총 전력 소모량: %.2f kWh\n\n", totalCostBattery / 1000.0));

        // 2. 층별 작업 현황 데이터
        dataForAi.append("### 2. 층별 작업 현황 데이터 (세부 수치)\n");
        dataForAi.append("| 구분 | 작업 횟수 (회) | 총 청소면적 (m²) | 총 전력 소비 (kWh) | 총 물 사용량 (L) |\n");
        dataForAi.append("| :--- | :--- | :--- | :--- | :--- |\n");
        for (MapStatsProjection stats : mapStats) {
            dataForAi.append(String.format("| %s | %d | %.2f | %.2f | %.2f |\n",
                    stats.getMapName(),
                    stats.getTaskCount(),
                    stats.getCleanArea(),
                    // Long costBattery (kWh로 가정)
                    stats.getCostBattery() / 1000.0,
                    // Long costWater (ml -> L 변환)
                    stats.getCostWater() / 1000.0));
        }
        dataForAi.append("\n");

        //유지관리 이력 (Placeholder) - AI가 채우도록 가이드
        dataForAi.append("### 3.  유지관리 이력 (점검 및 특이사항)\n");
        dataForAi.append("(실제 유지보수 이력 데이터가 현재 시스템에 존재하지 않아, AI가 내용 없음으로 처리하거나 일반적인 문구를 사용해야 함)\n\n");

        //작업 실패 및 중단 현황
        dataForAi.append("### 4. 작업 실패 및 중단 현황\n");
        dataForAi.append(String.format("임무 취소/중단 횟수: 총 %d 회\n", failedCount));
        dataForAi.append(String.format("주요 취소/중단 발생 층: %s\n", mostFailedMapName));
        dataForAi.append("임무 중단 및 이상 유무: " + (failedCount > 0 ? failedCount + "회 발생" : "0회 (이상 없음)") + "\n\n");


        // 3. 프롬프트 템플릿 정의 및 최종 Prompt 생성
        PromptTemplate template = PromptTemplate.from("""
        당신은 AI 산업용 청소로봇({{deviceNames}})의 관리 보고서 전문가입니다.
        
        아래 제공된 운영 데이터를 기반으로,
        '{{customerName}}' 매장의 공식 비즈니스 문서 스타일의 '청소로봇 관리 보고서'를 작성하세요.
        
        보고서의 시작은 반드시 아래와 같이 **Markdown 제목(Headirng)** 형식으로 시작하세요:
                            # AI 산업용 청소로봇 {{deviceNames}} 관리 보고서
                            ## 작성일: {{generatedDate}}
        
        # 보고서 기본 정보
        - 고객사: {{customerName}}
        - 장비명: AI 산업용 청소로봇 {{deviceNames}}
        - 제조사: PUDU ROBOTICS
        - 관리 기간: {{period}}

        
        # 데이터 입력 (실제 운영 데이터)
        {{question}}
        
        # 출력 형식 요구사항 (반드시 준수)
        1. 보고서 기본 정보 (표)
        2. 1. 장비 운영 요약 (표, 총 횟수/시간/면적, 전력/물 소비량 포함)
        3. 2. 층별 작업 현황 (표, 층별 횟수/면적/전력/물 소비량 포함)
        4. 3. 유지관리 이력 (표, 일자/구분/작업 내용/결과 포함)
        5. 4. 작업 실패 및 중단 현황 (요약 형식, 취소 횟수 및 원인 포함)
        6. 5. 향후 점검 계획 및 권장사항
           - 주요 성과 (계획 초과 달성 및 다층 운영 안정화 중점)
           - 문제점 및 위험 요소
           - 개선 및 최적화 권장사항 (잦은 취소율 관리 중점)
        
        출력은 반드시 Markdown 형식으로만 작성하세요.
    """);


        Prompt prompt = template.apply(
                Map.of(
                        "customerName", customerName,
                        "deviceNames", deviceNameText,
                        "period", periodText,
                        "generatedDate", generatedDate,
                        "question", dataForAi.toString()
                )
        );

        // SSE 스트리밍 시작
        createEmitter(
                emitter,
                convId,
                reportAgent,
                prompt.text(),
                req.getMessage(), // 사용자가 입력한 메시지(원본)
                startTime,
                endTime
        );

        return emitter;
    }


    /**
     * SSE 공통 스트리밍 처리
     */
    @Async("taskExecutor")
    protected void createEmitter(
            SseEmitter emitter,
            String convId,
            Agent agent,
            String prompt,
            String userMessage,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {

        StringBuilder fullText = new StringBuilder();

        try {

            TokenStream tokenStream = agent.chat(prompt, convId);
            User currentUser = getCurrentUser();

            // 대화 ID 먼저 전송
            emitter.send(
                    SseEmitter.event()
                            .name("conversationId")
                            .data(convId)
            );

            // 스트림 시작
            tokenStream
                    .onNext(token -> {
                        try {
                            fullText.append(token);
                            emitter.send(SseEmitter.event().data(token));
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    })
                    .onComplete(response -> {
                        try {
                            // DB 저장
                            AiReport savedReport = aiReportService.saveReport(
                                    currentUser.getUserId(),
                                    convId,
                                    userMessage,
                                    fullText.toString(),
                                    startTime,
                                    endTime
                            );

                            AiReportDTO dto = AiReportDTO.from(savedReport);

                            emitter.send(
                                    SseEmitter.event()
                                            .name("savedReport")
                                            .data(dto)
                            );

                            emitter.send(
                                    SseEmitter.event()
                                            .name("done")
                                            .data("[DONE]")
                            );

                        } catch (Exception e) {
                            emitter.completeWithError(e);
                        } finally {
                            emitter.complete();
                        }
                    })
                    .onError(e -> {
                        log.error("TokenStream error: ", e);
                        emitter.completeWithError(e);
                    })
                    .start();

        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }
}