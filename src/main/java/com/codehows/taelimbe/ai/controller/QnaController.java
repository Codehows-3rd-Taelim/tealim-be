package com.codehows.taelimbe.ai.controller;

import com.codehows.taelimbe.ai.constant.QnaStatus;
import com.codehows.taelimbe.ai.dto.QnaDTO;
import com.codehows.taelimbe.ai.dto.UpdateAnswerRequest;
import com.codehows.taelimbe.ai.service.QnaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/qna")
public class QnaController {

    private final QnaService qnaService;

    /* =========================
     * 조회 (관리 화면)
     * ========================= */

    // 전체
    @GetMapping
    public List<QnaDTO> listAll() {
        return qnaService.findAll()
                .stream()
                .map(QnaDTO::new)
                .toList();
    }

    // 운영 기준: 미처리
    @GetMapping("/unresolved")
    public List<QnaDTO> listUnresolved() {
        return qnaService.findByResolved(false)
                .stream()
                .map(QnaDTO::new)
                .toList();
    }

    // 운영 기준: 처리 완료
    @GetMapping("/resolved")
    public List<QnaDTO> listResolved() {
        return qnaService.findByResolved(true)
                .stream()
                .map(QnaDTO::new)
                .toList();
    }

    // 시스템 기준: QnA 적용 완료 (검색/챗봇 대상)
    @GetMapping("/applied")
    public List<QnaDTO> listApplied() {
        return qnaService.findByStatus(QnaStatus.APPLIED)
                .stream()
                .map(QnaDTO::new)
                .toList();
    }

    // 운영 종료 + QnA 미사용 (파일/정책 처리)
    @GetMapping("/resolved/without-qna")
    public List<QnaDTO> listResolvedWithoutQna() {
        return qnaService.findResolvedWithoutQna()
                .stream()
                .map(QnaDTO::new)
                .toList();
    }

    /* =========================
     * 답변 편집 (관리자 액션)
     * ========================= */

    // 답변 초안(editingAnswer) 저장
    @PutMapping("/{qnaId}/answer")
    public ResponseEntity<Void> updateAnswer(
            @PathVariable Long qnaId,
            @RequestBody UpdateAnswerRequest request
    ) {
        qnaService.updateEditingAnswer(qnaId, request.getAnswer());
        return ResponseEntity.ok().build();
    }

    /* =========================
     * QnA 적용
     * ========================= */

    // editingAnswer → appliedAnswer + 임베딩
    @PostMapping("/{qnaId}/apply")
    public ResponseEntity<Void> apply(@PathVariable Long qnaId) {
        qnaService.apply(qnaId);
        return ResponseEntity.ok().build();
    }

    /* =========================
     * 운영 액션
     * ========================= */

    // 파일/정책 처리로 QnA 미사용 종료
    @PostMapping("/{qnaId}/resolve-without-qna")
    public ResponseEntity<Void> resolveWithoutQna(@PathVariable Long qnaId) {
        qnaService.resolveWithoutQna(qnaId);
        return ResponseEntity.ok().build();
    }

    // 질문 완전 삭제 (Embed + Milvus 포함)
    @DeleteMapping("/{qnaId}")
    public ResponseEntity<Void> delete(@PathVariable Long qnaId) {
        qnaService.delete(qnaId);
        return ResponseEntity.noContent().build();
    }
}
