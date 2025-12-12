package com.codehows.taelimbe.ai.controller;

import com.codehows.taelimbe.ai.dto.AiReportDTO;
import com.codehows.taelimbe.ai.dto.ChatPromptRequest;
import com.codehows.taelimbe.ai.repository.AiReportMetaProjection;

import com.codehows.taelimbe.ai.repository.RawReportProjection;
import com.codehows.taelimbe.ai.service.AgentService;
import com.codehows.taelimbe.ai.service.AiReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ai/Report")
@RequiredArgsConstructor
public class AiReportController {

    private final AiReportService aiReportService;
    private final AgentService agentService;
    @GetMapping
    @ResponseBody
    public ResponseEntity<List<AiReportDTO>> getAllReports() {
        List<AiReportMetaProjection> reportsProjection = aiReportService.getAllReports();

        List<AiReportDTO> reportsDTO = reportsProjection.stream()
                .map(AiReportDTO::fromProjection)
                .collect(Collectors.toList());
        return ResponseEntity.ok(reportsDTO);
    }

    //raw 리포트만 따로 가져오게
    @GetMapping("/{reportId}/rawReport")
    @ResponseBody
    public ResponseEntity<RawReportProjection> getRawReport(@PathVariable Long reportId) {
        RawReportProjection rawReportProjection = aiReportService.getRawReport(reportId);
        return ResponseEntity.ok(rawReportProjection);
    }

    //SSE 구성할때 post로 하면 요청이 끝나야 응답을 주는데 get을 쓰면 길게 열어놓고 계속 보내는 게 가능해짐
    //서버가 실시간으로 데이터 스트리밍해서 전달 내가 보낸 메시지랑 대화 ID 받아서 실시간 보고서 만들어줌
    //Post 사용하면 POST로 보고서 생성 요청 → 요청 정보 DB에 저장하고 클라이언트에서 GET /sse 로 연결해서 스트리밍만 받기
    //이렇게 두번 하기 때문에 GET한 번으로 요청,응답 동시에 하는게 좋다.
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter report(
            @RequestParam("message") String message,
            @RequestParam("conversationId") String conversationId
    ) {
        ChatPromptRequest request = new ChatPromptRequest(message, conversationId);
        return agentService.report(request);
    }
}