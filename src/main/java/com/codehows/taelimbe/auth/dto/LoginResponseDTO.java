package com.codehows.taelimbe.auth.dto;

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

    private Long storeId;
    
    private Long userId;
}
