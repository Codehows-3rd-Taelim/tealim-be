package com.codehows.taelimbe.langchain.tools;

import com.codehows.taelimbe.ai.config.ToolArgsContextHolder;
import com.codehows.taelimbe.pudureport.dto.PuduReportDTO;
import com.codehows.taelimbe.pudureport.service.PuduReportService;
import com.codehows.taelimbe.store.constant.DeleteStatus;
import com.codehows.taelimbe.store.entity.Store;
import com.codehows.taelimbe.store.repository.StoreRepository;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportTools {

    private final StoreRepository storeRepository;
    private final PuduReportService puduReportService;

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
    - 기간이 두 개 이상의 연도에 걸쳐 있더라도 반드시 하나의 연속된 날짜 범위(startDate, endDate)로 변환하세요.
        - 예:
          - "25년 12월부터 26년 1월" →
            startDate = 2025-12-01
            endDate   = 2026-01-31

    날짜는 반드시 YYYY-MM-DD 형식으로 전달해야 합니다.
    storeId가 null이면 전매장 데이터를 조회합니다.
    """)
    public String getReport(String startDate, String endDate) {

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

        // 건수만 확인 (실제 집계는 AiReportService에서 DB 쿼리로 처리)
        List<PuduReportDTO> reportData = puduReportService.getReport(startDate, endDate);
        int count = reportData != null ? reportData.size() : 0;

        if (count == 0) {
            log.warn("[AI TOOL CALL] 결과 없음: {} ~ {}", startDate, endDate);
        }

        return "조회 완료: " + count + "건";
    }

    @Tool("""
    매장 단위 청소 로봇 보고서 데이터를 조회합니다.

    - USER: fixedStoreId만 허용.
    - ADMIN: resolveStore 결과 storeId 사용.

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
    - 기간이 두 개 이상의 연도에 걸쳐 있더라도 반드시 하나의 연속된 날짜 범위(startDate, endDate)로 변환하세요.
        - 예:
          - "25년 12월부터 26년 1월" →
            startDate = 2025-12-01
            endDate   = 2026-01-31

    날짜는 반드시 YYYY-MM-DD 형식으로 전달해야 합니다.
    storeId가 null이면 전매장 데이터를 조회합니다.
    """)
    public String getStoreReport(
            String startDate,
            String endDate,
            Long storeId
    ) {
        ToolArgsContextHolder.setToolArgs("startDate", startDate);
        ToolArgsContextHolder.setToolArgs("endDate", endDate);

        boolean isAdmin = Boolean.parseBoolean(
                ToolArgsContextHolder.getToolArgs("isAdmin")
        );

        Long resolvedStoreId;

        if (isAdmin) {
            resolvedStoreId = storeId;
        } else {
            resolvedStoreId = Long.valueOf(
                    ToolArgsContextHolder.getToolArgs("fixedStoreId")
            );
        }

        Store store = storeRepository.findById(resolvedStoreId).orElseThrow();

        ToolArgsContextHolder.setToolArgs("scope", "STORE");
        ToolArgsContextHolder.setToolArgs("storeName", store.getShopName());
        ToolArgsContextHolder.setToolArgs("resolvedStoreId", String.valueOf(resolvedStoreId));

        // 건수만 확인 (실제 집계는 AiReportService에서 DB 쿼리로 처리)
        List<PuduReportDTO> reports =
                puduReportService.getReportByStoreId(resolvedStoreId, startDate, endDate);
        int count = reports != null ? reports.size() : 0;

        return "조회 완료: " + count + "건";
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

        if (bestScore < 30) return null;

        List<Store> candidates = stores.stream()
                .filter(s -> similarityScore(shopNameCandidate, s.getShopName()) >= 30)
                .toList();

        for (Store s : candidates) {
            if (s.getShopName().contains("CC1")) {
                return s.getStoreId();
            }
        }

        return best.getStoreId();
    }

    private int similarityScore(String input, String target) {
        if (input == null || target == null) return 0;

        String a = normalize(input);
        String b = normalize(target);

        int score = 0;

        if (b.contains(a)) score += 100;
        if (a.contains(b)) score += 80;

        for (String token : a.split(" ")) {
            if (token.length() < 2) continue;
            if (b.contains(token)) score += 20;
        }

        return score;
    }

    private String normalize(String s) {
        return s
                .toLowerCase()
                .replaceAll("(주식회사|병원|공장|센터)", "")
                .replaceAll("\\s+", "");
    }
}
