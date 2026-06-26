package com.gsmv.ecosystem.dto;

import jakarta.validation.constraints.NotBlank;

public record ShippingZoneSaveRequest(
        @NotBlank(message = "生态系统名称不能为空") String name,
        String type,
        String description
) {
}
