package com.gsmv.report.dto;

public record EcosystemAnalyticsPoint(
        Long zoneId,
        String zoneName,
        String zoneType,
        long recordCount,
        long linkedVesselCount
) {
}
