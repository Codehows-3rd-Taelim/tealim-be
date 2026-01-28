package com.codehows.taelimbe.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class RemarkDTO {
    private final LocalDateTime startTime;
    private final String remark;
}
