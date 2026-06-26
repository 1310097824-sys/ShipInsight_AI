package com.gsmv.ai.report.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.List;

public final class AiReportDtos {

    private AiReportDtos() {
    }

    public record GenerateReportRequest(
            String reportType,
            @Min(value = 1, message = "统计天数不能小于 1") @Max(value = 365, message = "统计天数不能大于 365") Integer days,
            LocalDateTime observedFrom,
            LocalDateTime observedTo
    ) {
    }

    public record AiReportView(
            Long id,
            String reportType,
            int days,
            LocalDateTime periodStart,
            LocalDateTime periodEnd,
            String title,
            String summary,
            Long createdBy,
            String creatorName,
            LocalDateTime createdAt
    ) {
    }

    public record AiReportDetailView(
            Long id,
            String reportType,
            int days,
            LocalDateTime periodStart,
            LocalDateTime periodEnd,
            String title,
            String summary,
            List<String> highlights,
            List<String> risks,
            List<String> recommendations,
            List<String> evidence,
            AiReportMetrics metrics,
            Long createdBy,
            String creatorName,
            LocalDateTime createdAt
    ) {
    }

    public record AiReportMetrics(
            LocalDateTime periodStart,
            LocalDateTime periodEnd,
            String latestDatasetDate,
            long totalRecords,
            long uniqueVesselCount,
            long lowSpeedCount,
            long stoppedCount,
            long abnormalNoteCount,
            long riskSignalCount,
            List<AiReportDateStat> topDates,
            List<AiReportRankingStat> topImporters
    ) {
    }

    public record AiReportDateStat(
            String datasetDate,
            long recordCount
    ) {
    }

    public record AiReportRankingStat(
            String label,
            long recordCount
    ) {
    }
}
