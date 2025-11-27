package com.codehows.taelimbe.constant;

public enum Role {
    USER(2),
    MANAGER(1),
    ADMIN(0);

    private final int level;

    Role(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
