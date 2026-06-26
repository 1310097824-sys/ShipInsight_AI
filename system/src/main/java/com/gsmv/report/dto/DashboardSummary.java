package com.gsmv.report.dto;

public record DashboardSummary(
        long totalVesselProfiles,
        long totalAisRecords,
        long totalShippingZones,
        long totalUsers,
        long recentAisRecordCount
) {
}
