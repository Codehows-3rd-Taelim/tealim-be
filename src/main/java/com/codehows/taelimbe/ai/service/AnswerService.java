package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.ai.dto.AnswerDTO;
import com.codehows.taelimbe.ai.entity.Answer;
import com.codehows.taelimbe.ai.entity.Question;
import com.codehows.taelimbe.ai.repository.AnswerRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final QuestionService questionService;
    private final EmbeddingService embeddingService;

    public AnswerDTO getAnswer(Long questionId) {
        Answer answer = answerRepository.findById(questionId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Answer not found: " + questionId)
                );
        return new AnswerDTO(answer);
    }


    @Transactional
    public void setAnswer(Long questionId, String answerText) {

        Question question = questionService.get(questionId);


        String embedSourceText =
                "Q: " + question.getUserQuestionText() + "\n" +
                        "A: " + answerText;

        answerRepository.findById(questionId)
                .ifPresentOrElse(
                        existing -> existing.update(answerText, embedSourceText),
                        () -> answerRepository.save(
                                Answer.create(questionId, answerText, embedSourceText)
                        )
                );

        //  임베딩 호출
        embeddingService.overwrite(questionId, embedSourceText);

        // 상태 변경
        question.resolve();
    }
}
