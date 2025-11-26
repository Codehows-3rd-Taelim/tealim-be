package com.codehows.taelimbe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponseDTO
{
    private String jwtToken;

    private Integer roleLevel;
}
