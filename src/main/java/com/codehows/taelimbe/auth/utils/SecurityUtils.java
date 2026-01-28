package com.codehows.taelimbe.auth.utils;

import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    public static UserPrincipal getPrincipal() {
        return (UserPrincipal) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }
}
