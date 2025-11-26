package com.codehows.taelimbe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponseDto
{
    private String jwtToken;

    private boolean isAdmin;
}
