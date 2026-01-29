package com.codehows.taelimbe.ai.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StatusCountDTO {
    private final Integer status;
    private final long count;
}
