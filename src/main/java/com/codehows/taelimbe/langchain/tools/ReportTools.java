package com.codehows.taelimbe.langchain.tools;

import com.codehows.taelimbe.pudureport.dto.PuduReportDTO;
import com.codehows.taelimbe.pudureport.service.PuduReportService;
import com.google.gson.*;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI 에이전트가 사용할 수 있는 커스텀 도구들을 정의하는 클래스입니다.
 * `@Component` 어노테이션을 통해 Spring 컨테이너에 의해 관리되는 빈으로 등록됩니다.
 * `@RequiredArgsConstructor`는 Lombok 어노테이션으로, final 필드에 대한 생성자를 자동으로 생성하여 의존성 주입을 용이하게 합니다.
 */
@Component
@RequiredArgsConstructor

public class ReportTools {

    // CleaningDataService를 주입받아 청소 보고서 관련 비즈니스 로직을 수행합니다.
    private final PuduReportService puduReportService;
    // LangChainConfig에서 빈으로 등록된 Gson 인스턴스를 주입받습니다.
    private final Gson gson;

    /**
     * 지정된 날짜 범위 내의 청소 데이터를 페이징하여 조회하고 요약 보고서를 생성합니다.
     * 이 메서드는 AI 에이전트에 의해 '날짜별 청소 보고서 조회'와 같은 자연어 요청이 있을 때 호출될 수 있습니다.
     *
     * `@Tool` 어노테이션은 이 메서드가 AI 에이전트가 호출할 수 있는 도구임을 LangChain4j에 알립니다.
     * 어노테이션의 값은 도구의 설명을 제공하며, 이는 AI 모델이 도구를 언제, 어떻게 사용해야 할지 결정하는 데 도움을 줍니다.
     *
     * @param startDate 조회 시작 날짜 (YYYY-MM-DD 형식)
     * @param endDate   조회 종료 날짜 (YYYY-MM-DD 형식)
     * @return 페이징된 청소 데이터 요약 보고서 문자열 (JSON 형식)
     */
    //@Tool 메서드는 직접 호출되지 않고, LangChain4j가 런타임에 리플렉션으로 동적 호출하기 때문에 사용위치가 지금 당장은 안뜬다.
    @Tool("지정된 기간 동안의 청소 데이터를 페이징하여 가져옵니다.")
    public String getReport(String startDate, String endDate) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("Current user: " + username); // 사용자 이름을 콘솔에 출력 (디버깅용)

        // CleaningDataService를 통해 지정된 기간의 페이징된 청소 데이터를 조회합니다.
        List<PuduReportDTO> reportData = puduReportService.getReport(startDate, endDate);
        
        // 조회된 청소 데이터 페이지를 JSON 문자열로 변환하여 반환합니다。
        // 이 JSON에는 데이터 목록뿐만 아니라 총 페이지 수, 전체 항목 수 등의 페이징 정보가 포함됩니다.
        return gson.toJson(reportData);
    }
}
