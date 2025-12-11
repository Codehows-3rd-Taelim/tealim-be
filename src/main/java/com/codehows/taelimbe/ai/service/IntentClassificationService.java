package com.codehows.taelimbe.ai.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.codehows.taelimbe.langchain.Agent;
import dev.langchain4j.service.TokenStream;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class IntentClassificationService {

    private static final Logger log = LoggerFactory.getLogger(IntentClassificationService.class);

    private final Agent intentAgent; // streaming 기반 Agent 여도 동기 수집으로 처리

    // 1차 키워드 필터
    private static final List<String> REPORT_KEYWORDS = List.of(
            "보고서", "리포트", "정리", "요약", "분석",
            "어제", "오늘", "그제", "최근",
            "이번주", "지난주", "7일", "7개월", "7주",
            "이번 달", "지난 달", "이번달", "지난달",
            "~", "부터", "까지"
    );

    // 날짜 범위 감지: 2022-11-22~2025-11-22  또는 2022.11.22 to 2025.11.22 등
    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile(
            "\\d{4}[-./]\\d{1,2}[-./]\\d{1,2}\\s*(~|to|TO)\\s*\\d{4}[-./]\\d{1,2}[-./]\\d{1,2}"
    );

    private boolean containsDateRange(String msg) {
        if (msg == null) return false;
        return DATE_RANGE_PATTERN.matcher(msg).find(); // find() 사용해서 문자열 일부만 일치해도 OK
    }

    public boolean isLikelyReportByKeyword(String message) {
        if (message == null || message.isBlank()) return false;

        String cleaned = message.trim().toLowerCase();

        if (cleaned.length() < 2) return false;
        // ㅋㅋ, ㅎ 같은 노이즈 필터
        if (cleaned.matches(".*[ㅋㅎㅜㅠ]+.*")) return false;
        if (cleaned.matches(".*(바보|멍청).*")) return false;

        return REPORT_KEYWORDS.stream().anyMatch(cleaned::contains);
    }

    /**
     * 통합 분류 엔트리 (AgentService에서 이 메서드 호출)
     * - 날짜범위+키워드 우선, 그 다음 키워드, 그 외 LLM 판정
     */
    public String classify(String message) {
        String cleaned = message == null ? "" : message.trim();

        // 1) 날짜 + 키워드 => 무조건 REPORT
        if (containsDateRange(cleaned) && isLikelyReportByKeyword(cleaned)) {
            log.debug("IntentClassification: date+keyword => REPORT (msg='{}')", cleaned);
            return "REPORT";
        }

        // 2) 키워드만 있어도 강력히 REPORT
        if (isLikelyReportByKeyword(cleaned)) {
            log.debug("IntentClassification: keyword => REPORT (msg='{}')", cleaned);
            return "REPORT";
        }

        // 3) 그 외는 LLM으로 정밀 판단
        return classifyByLLM(cleaned);
    }

    /**
     * 스트리밍 Agent로부터 토큰을 동기적으로 수집하여 전체 문자열을 반환한다.
     * 스트리밍 기반 Agent를 intentAgent로 쓰고 있으므로, TokenStream을 수집해서 결과를 얻는다.
     */
    public String classifyByLLM(String message) {
        if (message == null) message = "";

        String prompt = """
            당신은 사용자의 메시지가 '업무 분석 또는 데이터 기반 보고서 생성' 요청인지 판단하는 매우 엄격한 필터입니다.

            [REPORT 예]
            - '보고서', '리포트', '요약', '분석', '정리', '통계' 포함
            - 날짜 범위가 명시된 경우 (예: 2022-01-01 ~ 2022-02-01)
            - '정리해줘', '요약해줘', '만들어줘' 같은 요청형 문장

            [CHAT 예]
            - 인사, 잡담, 감정 표현, 욕설, 짧은 무의미 텍스트 등

            출력은 정확히 하나의 단어만: REPORT 또는 CHAT
            (다른 설명을 포함하지 마세요)

            사용자 입력:
            "%s"
            """.formatted(message);

        StringBuilder sb = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);

        try {
            TokenStream tokenStream = intentAgent.chat(prompt, "intent-filter"); // streaming TokenStream 반환

            tokenStream
                    .onNext(token -> {
                        // 토큰을 누적
                        sb.append(token);
                    })
                    .onComplete(response -> {
                        latch.countDown();
                    })
                    .onError(e -> {
                        log.error("Intent classification tokenStream error", e);
                        latch.countDown();
                    })
                    .start();

            // 최대 대기 시간: 5초 (환경에 따라 늘릴 수 있음)
            boolean finished = latch.await(5, TimeUnit.SECONDS);
            if (!finished) {
                log.warn("Intent classification timed out; falling back to CHAT (message='{}')", message);
            }

        } catch (Exception e) {
            log.error("Exception during intent classification streaming", e);
        }

        String raw = sb.toString().trim();
        if (raw.isEmpty()) {
            // 스트림으로 못받았으면 안전하게 CHAT
            return "CHAT";
        }

        // 모델이 extra 텍스트를 붙일 가능성 대비: REPORT 또는 CHAT 단어 포함 판정
        String upper = raw.toUpperCase();
        log.debug("IntentClassification LLM raw result: {}", raw);

        if (upper.contains("REPORT")) return "REPORT";
        if (upper.contains("CHAT")) return "CHAT";

        // 그 외엔 안전하게 CHAT
        return "CHAT";
    }
}
