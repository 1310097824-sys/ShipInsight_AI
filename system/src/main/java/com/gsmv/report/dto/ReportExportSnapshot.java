package com.gsmv.report.dto;

import java.util.List;

public record ReportExportSnapshot(
        DashboardSummary summary,
        List<NameValuePoint> riskDistribution,
        List<NameValuePoint> operationalStatusDistribution,
        List<NameValuePoint> speciesPhylumDistribution,
        List<NameValuePoint> speciesClassDistribution,
        List<NameValuePoint> aisRecordTrend,
        List<NameValuePoint> aisRecordActivityByUser,
        List<EcosystemAnalyticsPoint> shippingZoneStats,
        List<VesselDistributionPoint> vesselDistributionPoints,
        List<AisRecordMapPoint> aisRecordMapPoints
) {
}
