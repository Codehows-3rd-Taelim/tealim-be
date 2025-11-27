package com.codehows.taelimbe.dto;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDTO
{
    private String jwtToken;

    private Integer roleLevel;
}

