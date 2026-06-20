package com.gsmv.vessel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record VesselSaveRequest(
        @NotBlank(message = "船名不能为空") String vesselName,
        String mmsi,
        String imo,
        String callSign,
        Long vesselTypeId,
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
        @NotNull(message = "档案状态不能为空") Integer status
) {
}
