package com.codehows.taelimbe.langchain.tools;

import com.codehows.taelimbe.ai.config.ToolArgsContextHolder;
import com.codehows.taelimbe.ai.dto.ReportResult;
import com.codehows.taelimbe.pudureport.dto.PuduReportDTO;
import com.codehows.taelimbe.pudureport.service.PuduReportService;
import com.codehows.taelimbe.store.constant.DeleteStatus;
import com.codehows.taelimbe.store.entity.Store;
import com.codehows.taelimbe.store.repository.StoreRepository;
import com.codehows.taelimbe.user.security.SecurityUtils;
import com.codehows.taelimbe.user.security.UserPrincipal;
import com.google.gson.*;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * AI 에이전트가 사용할 수 있는 커스텀 도구들을 정의하는 클래스입니다.
 * `@Component` 어노테이션을 통해 Spring 컨테이너에 의해 관리되는 빈으로 등록됩니다.
 * `@RequiredArgsConstructor`는 Lombok 어노테이션으로, final 필드에 대한 생성자를 자동으로 생성하여 의존성 주입을 용이하게 합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReportTools {

    private final StoreRepository storeRepository;
    // CleaningDataService를 주입받아 청소 보고서 관련 비즈니스 로직을 수행합니다.
    private final PuduReportService puduReportService;
    // LangChainConfig에서 빈으로 등록된 Gson 인스턴스를 주입받습니다.
    private final Gson gson;

    @Tool("""
    (ADMIN 전용)
    관리자가 요청한 기간에 해당하는 청소 로봇 운영 데이터를 조회합니다.
    
    ⚠️ 날짜 규칙:
    - "어제" → 어제 날짜.
    - "오늘" → 오늘 날짜.
    - "지난주" → 지난주 월~일.
    - "저번주" → 지난주 월~일.
    - "이번주" → 이번주 월~일.
    - "이번달" → 이번 달 1일~말일.
    - 연도가 없는 경우:
        - "n월" → **올해 n월** 데이터 조회 (예: "12월" → 2025-12-01 ~ 2025-12-31)
    - 연도 + 월이 있는 경우:
        - "YYYY년 n월" → 해당 연도 n월 데이터 조회 (예: "2024년 10월" → 2024-10-01 ~ 2024-10-31)
    - "최근 7일" → 오늘 기준 7일 전 ~ 오늘.
    
    날짜는 반드시 YYYY-MM-DD 형식으로 전달해야 합니다.
    storeId가 null이면 전매장 데이터를 조회합니다.
    """)
    public ReportResult getReport(String startDate, String endDate) {

        boolean isAdmin = Boolean.parseBoolean(
                ToolArgsContextHolder.getToolArgs("isAdmin")
        );

        if (!isAdmin) {
            throw new SecurityException("전매장 보고서는 관리자만 조회 가능합니다.");
        }

        ToolArgsContextHolder.setToolArgs("startDate", startDate);
        ToolArgsContextHolder.setToolArgs("endDate", endDate);
        ToolArgsContextHolder.setToolArgs("scope", "ALL");
        ToolArgsContextHolder.setToolArgs("storeName", null);

        log.info("[AI TOOL CALL] getReport({}, {})", startDate, endDate);

        List<PuduReportDTO> reportData = puduReportService.getReport(startDate, endDate);

        if (reportData == null || reportData.isEmpty()) {
            log.warn("[AI TOOL CALL] 결과 없음: {} ~ {}", startDate, endDate);
        }

        return new ReportResult(gson.toJson(reportData), startDate, endDate);
    }

    @Tool("""
    매장 단위 청소 로봇 보고서 데이터를 조회합니다.
    
    - USER: fixedStoreId만 허용.
    - ADMIN: resolveStore 결과 storeId 사용.
    """)
    public ReportResult getStoreReport(
            String startDate,
            String endDate,
            Long storeId
    ) {
        ToolArgsContextHolder.setToolArgs("startDate", startDate);
        ToolArgsContextHolder.setToolArgs("endDate", endDate);

        UserPrincipal principal = SecurityUtils.getPrincipal();

        Long resolvedStoreId;

        if (principal.isAdmin()) {
            resolvedStoreId = storeId;
        } else {
            // USER는 무조건 본인 매장
            resolvedStoreId = Long.valueOf(
                    ToolArgsContextHolder.getToolArgs("fixedStoreId")
            );
        }

        Store store = storeRepository.findById(resolvedStoreId).orElseThrow();

        ToolArgsContextHolder.setToolArgs("scope", "STORE");
        ToolArgsContextHolder.setToolArgs("storeName", store.getShopName());

        List<PuduReportDTO> reports =
                puduReportService.getReportByStoreId(
                        startDate,
                        endDate,
                        resolvedStoreId
                );

        return new ReportResult(gson.toJson(reports), startDate, endDate);
    }

    @Tool("""
    사용자 입력에서 추출한 매장명 후보를
    현재 등록된 매장 목록과 비교하여
    가장 유사한 매장의 storeId를 반환합니다.
    판단 불가 시 null 반환.
    """)
    public Long resolveStore(String shopNameCandidate) {

        if (shopNameCandidate == null || shopNameCandidate.isBlank()) {
            return null;
        }

        List<Store> stores = storeRepository.findAllByDelYn(DeleteStatus.N);

        Store best = null;
        int bestScore = 0;

        for (Store store : stores) {
            int score = similarityScore(shopNameCandidate, store.getShopName());
            if (score > bestScore) {
                bestScore = score;
                best = store;
            }
        }

        // 임계값: 30이면 웬만한 의미 일치 다 잡힘
        if (bestScore < 30) return null; // 의미 있는 후보 없음

        // 후보 다수일 경우 CC1 포함 우선
        List<Store> candidates = stores.stream()
                .filter(s -> similarityScore(shopNameCandidate, s.getShopName()) >= 30)
                .toList();

        for (Store s : candidates) {
            if (s.getShopName().contains("CC1")) {
                return s.getStoreId(); // CC1 포함 매장 우선 반환
            }
        }

        return best.getStoreId(); // CC1 없는 경우 최고점 반환
    }

    private int similarityScore(String input, String target) {
        if (input == null || target == null) return 0;

        String a = normalize(input);
        String b = normalize(target);

        int score = 0;

        // 완전 포함
        if (b.contains(a)) score += 100;
        if (a.contains(b)) score += 80;

        // 토큰 단위 비교
        for (String token : a.split(" ")) {
            if (token.length() < 2) continue;
            if (b.contains(token)) score += 20;
        }

        return score;
    }

    private String normalize(String s) {
        return s
                .toLowerCase()
                .replaceAll("(주식회사|병원|공장|센터)", "") // 접미어 제거
                .replaceAll("\\s+", "");
    }
}