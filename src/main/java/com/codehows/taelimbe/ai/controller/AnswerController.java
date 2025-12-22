package com.codehows.taelimbe.ai.controller;

import com.codehows.taelimbe.ai.dto.AnswerDTO;
import com.codehows.taelimbe.ai.dto.AnswerSetRequest;
import com.codehows.taelimbe.ai.dto.QnaEmbeddingRequest;
import com.codehows.taelimbe.ai.service.AnswerService;
import com.codehows.taelimbe.ai.service.EmbeddingService;
import com.codehows.taelimbe.ai.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
@RequestMapping("/questions")
public class AnswerController {

    private final AnswerService answerService;
    private final QuestionService questionService;

    @GetMapping("/{questionId}/answer")
    public AnswerDTO getAnswer(@PathVariable Long questionId) {
        return answerService.getAnswer(questionId);
    }

    @PostMapping("/{questionId}/answer")
    public ResponseEntity<Void> createAnswer(
            @PathVariable Long questionId,
            @RequestBody AnswerSetRequest request
    ) {
        answerService.setAnswer(questionId, request.getAnswerText());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{questionId}/answer")
    public ResponseEntity<Void> updateAnswer(
            @PathVariable Long questionId,
            @RequestBody AnswerSetRequest request
    ) {
        answerService.setAnswer(questionId, request.getAnswerText());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/qna")
    public CompletableFuture<Void> embedQna(@RequestBody QnaEmbeddingRequest req) {
        return questionService.embedQnaAndResolve(
                req.getQuestion(),
                req.getAnswer(),
                req.getQuestionId()
        );
    }

}
