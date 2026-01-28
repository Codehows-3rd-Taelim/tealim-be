package com.codehows.taelimbe.auth.utils;

import java.io.Serializable;

public record UserPrincipal(
        Long userId,
        String username,
        boolean isAdmin,
        Long storeId
) implements Serializable {
}