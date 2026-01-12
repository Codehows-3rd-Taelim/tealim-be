package com.codehows.taelimbe.qna.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class QnaRequest {
    private String title;
    private String questionText;
}
