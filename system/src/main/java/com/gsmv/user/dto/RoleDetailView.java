package com.gsmv.user.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RoleDetailView(
        Long id,
        String code,
        String name,
        String description,
        List<PermissionOption> permissions,
        int userCount,
        LocalDateTime createdAt
) {}
