package com.codehows.taelimbe.sync;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncResultDTO {

    private Integer storeCount;
    private Integer robotCount;
    private Integer reportCount;
    private Integer totalCount;

    private Boolean success;
    private String errorMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime syncTime;
}