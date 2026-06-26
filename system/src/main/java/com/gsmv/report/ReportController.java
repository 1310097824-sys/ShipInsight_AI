package com.gsmv.report;

import com.gsmv.common.ApiResponse;
import com.gsmv.report.dto.DashboardSummary;
import com.gsmv.report.dto.EcosystemAnalyticsPoint;
import com.gsmv.report.dto.NameValuePoint;
import com.gsmv.report.dto.AisRecordMapPoint;
import com.gsmv.report.dto.VesselDistributionPoint;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('REPORT_READ')")
    public ApiResponse<DashboardSummary> summary() {
        return ApiResponse.success(reportService.dashboardSummary());
    }

    @GetMapping("/risk-level")
    @PreAuthorize("hasAuthority('REPORT_READ')")
    public ApiResponse<List<NameValuePoint>> riskDistribution() {
        return ApiResponse.success(reportService.riskDistribution());
    }

    @GetMapping("/operational-status")
    @PreAuthorize("hasAuthority('REPORT_READ')")
    public ApiResponse<List<NameValuePoint>> operationalStatusDistribution() {
        return ApiResponse.success(reportService.operationalStatusDistribution());
    }

    @GetMapping("/vessel-type-distribution")
    @PreAuthorize("hasAuthority('REPORT_READ')")
    public ApiResponse<List<NameValuePoint>> vesselTypeDistribution(@RequestParam(defaultValue = "phylum") String level) {
        if ("phylum".equals(level)) {
            return ApiResponse.success(reportService.speciesPhylumDistribution());
        }
        return ApiResponse.success(reportService.speciesClassDistribution());
    }

    @GetMapping("/ais-record-trend")
    @PreAuthorize("hasAuthority('REPORT_READ')")
    public ApiResponse<List<NameValuePoint>> aisRecordTrend(@RequestParam(defaultValue = "30") int days) {
        return ApiResponse.success(reportService.aisRecordTrend(days));
    }

    @GetMapping("/ais-record-activity")
    @PreAuthorize("hasAuthority('REPORT_READ')")
    public ApiResponse<List<NameValuePoint>> aisRecordActivity(@RequestParam(defaultValue = "30") int days) {
        return ApiResponse.success(reportService.aisRecordActivityByUser(days));
    }

    @GetMapping("/shipping-zone-stats")
    @PreAuthorize("hasAuthority('REPORT_READ')")
    public ApiResponse<List<EcosystemAnalyticsPoint>> shippingZoneStats() {
        return ApiResponse.success(reportService.shippingZoneStats());
    }

    @GetMapping("/vessel-distribution")
    @PreAuthorize("hasAuthority('REPORT_READ')")
    public ApiResponse<List<VesselDistributionPoint>> vesselDistribution() {
        return ApiResponse.success(reportService.vesselDistributionPoints());
    }

    @GetMapping("/ais-record-map")
    @PreAuthorize("hasAuthority('REPORT_READ')")
    public ApiResponse<List<AisRecordMapPoint>> aisRecordMap() {
        return ApiResponse.success(reportService.aisRecordMapPoints());
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasAuthority('REPORT_READ')")
    public ResponseEntity<byte[]> exportExcel(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, buildDisposition("shipinsight-report-" + fileDate() + ".xlsx"))
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(reportService.exportExcel(days));
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasAuthority('REPORT_READ')")
    public ResponseEntity<byte[]> exportPdf(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, buildDisposition("shipinsight-report-" + fileDate() + ".pdf"))
                .contentType(MediaType.APPLICATION_PDF)
                .body(reportService.exportPdf(days));
    }

    private String buildDisposition(String fileName) {
        return ContentDisposition.attachment().filename(fileName).build().toString();
    }

    private String fileDate() {
        return FILE_DATE_FORMAT.format(LocalDate.now());
    }
}
