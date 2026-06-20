package com.gsmv.vessel.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VesselRow(
        Long id,
        String vesselName,
        String mmsi,
        String imo,
        String callSign,
        Long vesselTypeId,
        String vesselTypeName,
        String vesselTypeCode,
        Long vesselTypeParentId,
        String flagState,
        String operatorName,
        String ownerName,
        BigDecimal lengthM,
        BigDecimal widthM,
        BigDecimal draftM,
        BigDecimal grossTonnage,
        BigDecimal deadweightTonnage,
        String riskLevel,
        String navigationStatus,
        String homePort,
        String usualRegion,
        String routeArea,
        String note,
        String sourceText,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
