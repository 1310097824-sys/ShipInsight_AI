package com.gsmv.report.dto;

import java.math.BigDecimal;

public record VesselDistributionPoint(
        Long vesselId,
        String vesselName,
        String displayName,
        String profileName,
        BigDecimal locationLat,
        BigDecimal locationLng,
        String routeDescription,
        String riskLevel,
        String operationalStatus
) {
}
