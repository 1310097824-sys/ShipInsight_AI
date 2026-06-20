package com.gsmv.vessel.dto;

public record VesselTypeOption(
        Long id,
        Long parentId,
        String code,
        String name,
        String description
) {
}
