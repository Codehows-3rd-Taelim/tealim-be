package com.codehows.taelimbe.ai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AnswerSetRequest {

    private Long questionId;
    private String answerText;
}
