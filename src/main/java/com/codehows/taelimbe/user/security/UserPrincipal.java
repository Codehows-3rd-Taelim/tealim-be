package com.codehows.taelimbe.user.security;

import java.io.Serializable;

public record UserPrincipal(
        Long userId,
        String username
) implements Serializable {
}