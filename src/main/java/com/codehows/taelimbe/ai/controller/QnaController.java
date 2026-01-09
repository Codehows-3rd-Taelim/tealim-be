package com.codehows.taelimbe.ai.controller;

import com.codehows.taelimbe.ai.constant.QnaStatus;
import com.codehows.taelimbe.ai.dto.CreateQnaRequest;
import com.codehows.taelimbe.ai.dto.QnaDTO;
import com.codehows.taelimbe.ai.dto.UpdateAnswerRequest;
import com.codehows.taelimbe.ai.service.QnaService;
import com.codehows.taelimbe.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/qna")
public class QnaController {

    private final QnaService qnaService;

    // qna 전체 조회
    @GetMapping
    public List<QnaDTO> listAll(Authentication authentication) {
        UserPrincipal user =
                (UserPrincipal) authentication.getPrincipal();

        return qnaService.findAll(user)
                .stream()
                .map(QnaDTO::new)
                .toList();
    }

    // 미처리 질문 조회
    @GetMapping("/unresolved")
    public List<QnaDTO> listUnresolved(Authentication authentication) {
        UserPrincipal user =
                (UserPrincipal) authentication.getPrincipal();

        return qnaService.findByResolved(false, user)
                .stream()
                .map(QnaDTO::new)
                .toList();
    }

    // 처리 완료 질문 조회
    @GetMapping("/resolved")
    public List<QnaDTO> listResolved(Authentication authentication) {
        UserPrincipal user =
                (UserPrincipal) authentication.getPrincipal();

        return qnaService.findByResolved(true, user)
                .stream()
                .map(QnaDTO::new)
                .toList();
    }

    // QnA 적용 완료된 질문 조회
    @GetMapping("/applied")
    public List<QnaDTO> listApplied(Authentication authentication) {
        UserPrincipal user =
                (UserPrincipal) authentication.getPrincipal();

        return qnaService.findByStatus(QnaStatus.APPLIED, user)
                .stream()
                .map(QnaDTO::new)
                .toList();
    }

    // qna 임베딩
    @PostMapping("/{qnaId}/apply")
    public ResponseEntity<Void> apply(
            @PathVariable Long qnaId,
            @RequestBody UpdateAnswerRequest request
    ) {
        qnaService.apply(qnaId, request.getAnswer());
        return ResponseEntity.ok().build();
    }

    // 질문 소프트 삭제
    @DeleteMapping("/{qnaId}")
    public ResponseEntity<Void> questionDelete(@PathVariable Long qnaId) {
        qnaService.questionDelete(qnaId);
        return ResponseEntity.noContent().build();
    }

    // displayAnswer 저장
    @PostMapping("/{qnaId}/display-answer")
    public ResponseEntity<Void> saveDisplayAnswer(
            @PathVariable Long qnaId,
            @RequestBody UpdateAnswerRequest request,
            Authentication authentication) {
        UserPrincipal user =
                (UserPrincipal) authentication.getPrincipal();

        if (!user.isAdmin()) {
            return ResponseEntity.status(403).build();
        }

        qnaService.saveDisplayAnswer(qnaId, request.getAnswer());
        return ResponseEntity.ok().build();
    }

    // displayAnswer 수정
    @PutMapping("/{qnaId}/display-answer")
    public ResponseEntity<Void> updateDisplayAnswer(
            @PathVariable Long qnaId,
            @RequestBody UpdateAnswerRequest request,
            Authentication authentication) {
        UserPrincipal user =
                (UserPrincipal) authentication.getPrincipal();

        if (!user.isAdmin()) {
            return ResponseEntity.status(403).build();
        }

        qnaService.updateDisplayAnswer(qnaId, request.getAnswer());
        return ResponseEntity.ok().build();
    }

    // displayAnswer 삭제 (미처리 상태로 변경)
    @DeleteMapping("/{qnaId}/display-answer")
    public ResponseEntity<Void> deleteDisplayAnswer(
            @PathVariable Long qnaId,
            Authentication authentication) {
        UserPrincipal user =
                (UserPrincipal) authentication.getPrincipal();

        if (!user.isAdmin()) {
            return ResponseEntity.status(403).build();
        }

        qnaService.deleteDisplayAnswer(qnaId);
        return ResponseEntity.noContent().build();
    }

    // 챗봇 답변 삭제
    @DeleteMapping("/{qnaId}/applied-answer")
    public ResponseEntity<Void> deleteAppliedAnswer(
            @PathVariable Long qnaId,
            Authentication authentication
    ) {
        UserPrincipal user =
                (UserPrincipal) authentication.getPrincipal();

        if (!user.isAdmin()) {
            return ResponseEntity.status(403).build();
        }

        qnaService.deleteAppliedAnswer(qnaId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<Long> createQuestion(
            @RequestBody CreateQnaRequest request,
            Authentication authentication
    ) {
        UserPrincipal principal =
                (UserPrincipal) authentication.getPrincipal();

        Long qnaId = qnaService.createQuestion(
                request.getQuestionText(),
                principal.userId()
        );

        return ResponseEntity.ok(qnaId);
    }


}
