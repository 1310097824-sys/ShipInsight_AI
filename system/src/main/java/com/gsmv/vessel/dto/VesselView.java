package com.gsmv.vessel.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VesselView(
        Long id,
        String vesselName,
        String mmsi,
        String imo,
        String callSign,
        Long vesselTypeId,
        String vesselTypeName,
        String vesselTypePath,
        String flagState,
        String operatorName,
        BigDecimal lengthM,
        BigDecimal widthM,
        BigDecimal draftM,
        String riskLevel,
        String navigationStatus,
        String usualRegion,
        String routeArea,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
