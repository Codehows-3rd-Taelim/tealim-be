package com.codehows.taelimbe.ai.embedding.controller;

import com.codehows.taelimbe.qna.dto.UpdateAnswerRequest;
import com.codehows.taelimbe.ai.embedding.service.QnaEmbedService;
import com.codehows.taelimbe.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/qna/embed")
public class QnaEmbedController {

    private final QnaEmbedService qnaEmbedService;


    // qna 임베딩
    @PostMapping("/{qnaId}/apply")
    public ResponseEntity<Void> apply(
            @PathVariable Long qnaId,
            @RequestBody UpdateAnswerRequest request
    ) {
        qnaEmbedService.apply(qnaId, request.getAnswer());
        return ResponseEntity.ok().build();
    }



    // 챗봇 답변 삭제
    @DeleteMapping("/{qnaId}/apply")
    public ResponseEntity<Void> deleteAppliedAnswer(
            @PathVariable Long qnaId,
            Authentication authentication
    ) {
        UserPrincipal user =
                (UserPrincipal) authentication.getPrincipal();

        if (!user.isAdmin()) {
            return ResponseEntity.status(403).build();
        }

        qnaEmbedService.deleteAppliedAnswer(qnaId);
        return ResponseEntity.noContent().build();
    }
}
