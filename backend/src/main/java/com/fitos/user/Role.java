package com.fitos.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Role {
    ROLE_ADMIN,
    ROLE_MANAGER,
    ROLE_SALES,
    ROLE_TRAINER,
    ROLE_MEMBER;

    @JsonValue
    public String simpleName() {
        return name().replace("ROLE_", "");
    }

    @JsonCreator
    public static Role from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }
        return Role.valueOf(normalized);
    }
}
