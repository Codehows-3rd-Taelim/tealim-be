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
    """)
    public ReportResult getReport(String startDate, String endDate) {

        if (startDate == null || endDate == null) {
            log.warn("[AI TOOL CALL] 기간 미입력: startDate={}, endDate={}", startDate, endDate);
            throw new IllegalArgumentException("⚠️ 기간을 명확히 입력해주세요.");
        }

        boolean isAdmin = Boolean.parseBoolean(
                ToolArgsContextHolder.getToolArgs("isAdmin")
        );

        if (!isAdmin) {
            throw new SecurityException("전매장 보고서는 관리자만 조회 가능합니다.");
        }

        ToolArgsContextHolder.setToolArgs("startDate", startDate);
        ToolArgsContextHolder.setToolArgs("endDate", endDate);

        log.info("[AI TOOL CALL] getReport({}, {})", startDate, endDate);

        List<PuduReportDTO> reportData = puduReportService.getReport(startDate, endDate);

        if (reportData == null || reportData.isEmpty()) {
            log.warn("[AI TOOL CALL] 결과 없음: {} ~ {}", startDate, endDate);
        }

        return new ReportResult(gson.toJson(reportData), startDate, endDate);
    }

    @Tool("""
    매장 단위 청소 로봇 보고서 데이터를 조회합니다.
    
    - ADMIN: shopName 기반 전체 매장 조회 가능
    - USER: 본인 storeId만 조회 가능
    """)
    public ReportResult getStoreReport(
            String startDate,
            String endDate,
            String shopName
    ) {
        ToolArgsContextHolder.setToolArgs("startDate", startDate);
        ToolArgsContextHolder.setToolArgs("endDate", endDate);

        UserPrincipal principal = SecurityUtils.getPrincipal();

        Long resolvedStoreId;

        if (principal.isAdmin() && shopName == null) {
            throw new IllegalArgumentException("관리자는 매장명을 지정하거나 전체 조회를 선택해야 합니다.");
        }

        if (principal.isAdmin()) {
            // 관리자: shopName → storeId 고정
            Store store = storeRepository.findByShopNameAndDelYn(shopName, DeleteStatus.N)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 매장입니다."));

            resolvedStoreId = store.getStoreId();
        } else {
            resolvedStoreId = principal.storeId();
        }

        ToolArgsContextHolder.setToolArgs("targetStoreId", resolvedStoreId.toString());

        List<PuduReportDTO> reports =
                puduReportService.getReportByStoreId(
                        startDate,
                        endDate,
                        resolvedStoreId
                );

        return new ReportResult(gson.toJson(reports), startDate, endDate);
    }
}