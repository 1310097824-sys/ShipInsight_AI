package com.gsmv.report;

import com.gsmv.report.dto.DashboardSummary;
import com.gsmv.report.dto.EcosystemAnalyticsPoint;
import com.gsmv.report.dto.NameValuePoint;
import com.gsmv.report.dto.AisRecordMapPoint;
import com.gsmv.report.dto.ReportExportSnapshot;
import com.gsmv.report.dto.VesselDistributionPoint;
import com.gsmv.report.export.ReportExcelExporter;
import com.gsmv.report.export.ReportPdfExporter;
import com.gsmv.report.mapper.ReportMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private final ReportMapper reportMapper;

    public ReportService(ReportMapper reportMapper) {
        this.reportMapper = reportMapper;
    }

    public DashboardSummary dashboardSummary() {
        return new DashboardSummary(
                reportMapper.countActiveSpecies(),
                reportMapper.countObservations(),
                reportMapper.countEcosystems(),
                reportMapper.countActiveUsers(),
                reportMapper.countRecentObservations()
        );
    }

    public List<NameValuePoint> riskDistribution() {
        return reportMapper.riskDistribution();
    }

    public List<NameValuePoint> operationalStatusDistribution() {
        return reportMapper.iucnStatusDistribution();
    }

    public List<NameValuePoint> protectionLevelDistribution() {
        return reportMapper.protectionLevelDistribution();
    }

    public List<NameValuePoint> speciesPhylumDistribution() {
        return reportMapper.speciesPhylumDistribution();
    }

    public List<NameValuePoint> speciesClassDistribution() {
        return reportMapper.speciesClassDistribution();
    }

    public List<NameValuePoint> aisRecordTrend(int days) {
        return reportMapper.observationTrend(sanitizeDays(days));
    }

    public List<NameValuePoint> aisRecordActivityByUser(int days) {
        return reportMapper.observationActivityByUser(sanitizeDays(days));
    }

    public List<EcosystemAnalyticsPoint> shippingZoneStats() {
        return reportMapper.ecosystemAnalytics();
    }

    public List<VesselDistributionPoint> vesselDistributionPoints() {
        return reportMapper.speciesDistributionPoints();
    }

    public List<AisRecordMapPoint> aisRecordMapPoints() {
        return reportMapper.observationMapPoints();
    }

    public byte[] exportExcel(int days) {
        return ReportExcelExporter.export(buildSnapshot(days));
    }

    public byte[] exportPdf(int days) {
        return ReportPdfExporter.export(buildSnapshot(days));
    }

    private ReportExportSnapshot buildSnapshot(int days) {
        int safeDays = sanitizeDays(days);
        return new ReportExportSnapshot(
                dashboardSummary(),
                riskDistribution(),
                operationalStatusDistribution(),
                speciesPhylumDistribution(),
                speciesClassDistribution(),
                aisRecordTrend(safeDays),
                aisRecordActivityByUser(safeDays),
                shippingZoneStats(),
                vesselDistributionPoints(),
                aisRecordMapPoints()
        );
    }

    private int sanitizeDays(int days) {
        return Math.min(Math.max(days, 1), 365);
    }
}
