package com.gsmv.ai.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gsmv.ai.AiModelGateway;
import com.gsmv.ai.rag.RagKnowledgeService;
import com.gsmv.ai.rag.RagSearchHit;
import com.gsmv.ai.report.dto.AiReportDtos;
import com.gsmv.ai.report.export.AiReportPdfExporter;
import com.gsmv.ai.report.mapper.AiReportMapper;
import com.gsmv.ai.report.model.AiReport;
import com.gsmv.ais.AisService;
import com.gsmv.ais.dto.AisDatasetDateStat;
import com.gsmv.ais.dto.AisRankingStat;
import com.gsmv.ais.dto.AisRecordView;
import com.gsmv.ais.dto.AisRiskSummary;
import com.gsmv.audit.service.AuditService;
import com.gsmv.common.ErrorCode;
import com.gsmv.common.PageResponse;
import com.gsmv.common.exception.BusinessException;
import com.gsmv.common.exception.NotFoundException;
import com.gsmv.security.CurrentUser;
import com.gsmv.security.SecurityUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AiReportService {

    private final AiReportMapper aiReportMapper;
    private final AisService aisService;
    private final AiModelGateway aiModelGateway;
    private final RagKnowledgeService ragKnowledgeService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public AiReportService(
            AiReportMapper aiReportMapper,
            AisService aisService,
            AiModelGateway aiModelGateway,
            RagKnowledgeService ragKnowledgeService,
            AuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.aiReportMapper = aiReportMapper;
        this.aisService = aisService;
        this.aiModelGateway = aiModelGateway;
        this.ragKnowledgeService = ragKnowledgeService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AiReportDtos.AiReportDetailView generate(AiReportDtos.GenerateReportRequest request) {
        CurrentUser currentUser = SecurityUtils.requireCurrentUser();
        AiReportDtos.GenerateReportRequest safeRequest = request == null
                ? new AiReportDtos.GenerateReportRequest(null, null, null, null)
                : request;
        int requestedDays = sanitizeDays(safeRequest.days());
        RequestedReportWindow requestedWindow = validateRequestedWindow(
                safeRequest.observedFrom(),
                safeRequest.observedTo(),
                requestedDays
        );
        String reportType = normalizeReportType(safeRequest.reportType(), requestedWindow.days(), requestedWindow.custom());
        AisReportContext context = loadAisContext(requestedWindow);
        int reportDays = context.days();
        List<RagSearchHit> ragHits = retrieveReportEvidence(reportType, context);
        GeneratedReport generated = generateWithAi(reportType, context, ragHits);

        AiReport report = new AiReport();
        report.setReportType(reportType);
        report.setDays(reportDays);
        report.setPeriodStart(context.metrics().periodStart());
        report.setPeriodEnd(context.metrics().periodEnd());
        report.setTitle(generated.title());
        report.setSummary(generated.summary());
        report.setHighlightsJson(writeJson(generated.highlights()));
        report.setRisksJson(writeJson(generated.risks()));
        report.setRecommendationsJson(writeJson(generated.recommendations()));
        report.setEvidenceJson(writeJson(generated.evidence()));
        report.setMetricsJson(writeJson(context.metrics()));
        report.setCreatedBy(currentUser.userId());
        aiReportMapper.insert(report);
        if (ragKnowledgeService != null) {
            ragKnowledgeService.syncAiReport(report.getId());
        }

        if (auditService != null) {
            auditService.record(currentUser.userId(), "AI", "GENERATE_AIS_REPORT", "AI_AIS_REPORT", report.getId(), true,
                    "{\"days\":" + reportDays
                            + ",\"reportType\":\"" + escapeJson(reportType) + "\""
                            + ",\"periodStart\":\"" + escapeJson(formatDateTime(context.metrics().periodStart())) + "\""
                            + ",\"periodEnd\":\"" + escapeJson(formatDateTime(context.metrics().periodEnd())) + "\"}");
        }
        return getDetail(report.getId());
    }

    public PageResponse<AiReportDtos.AiReportView> list(int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        List<AiReportDtos.AiReportView> items = aiReportMapper.findPage(safeSize, offset).stream()
                .map(this::toView)
                .toList();
        return new PageResponse<>(items, aiReportMapper.count(), safePage, safeSize);
    }

    public AiReportDtos.AiReportDetailView getDetail(Long id) {
        AiReport report = aiReportMapper.findById(id);
        if (report == null) {
            throw new NotFoundException("AI 分析报告不存在");
        }
        return toDetail(report);
    }

    public byte[] exportPdf(Long id) {
        return AiReportPdfExporter.export(getDetail(id));
    }

    private AisReportContext loadAisContext(RequestedReportWindow requestedWindow) {
        List<AisDatasetDateStat> allDateStats = aisService.datasetDateStats(null, null, null);
        RequestedReportWindow window = requestedWindow;
        if (!requestedWindow.custom()) {
            LocalDateTime periodEnd = latestDatasetEnd(allDateStats);
            if (periodEnd == null) {
                periodEnd = LocalDateTime.now();
            }
            LocalDateTime periodStart = periodEnd.minusDays(requestedWindow.days());
            window = new RequestedReportWindow(periodStart, periodEnd, requestedWindow.days(), false);
        }
        LocalDateTime periodStart = window.periodStart();
        LocalDateTime periodEnd = window.periodEnd();
        List<AisDatasetDateStat> windowDateStats = aisService.datasetDateStats(null, periodStart, periodEnd);
        AisRiskSummary riskSummary = aisService.riskSummary(null, periodStart, periodEnd);
        List<AisRankingStat> topImporters = aisService.importerStats(null, periodStart, periodEnd, 5);
        PageResponse<AisRecordView> recentPage = aisService.list(null, periodStart, periodEnd, 1, 8);
        List<AisRecordView> recentRecords = recentPage.items() == null ? List.of() : recentPage.items();

        long totalRecords = riskSummary.total();
        if (totalRecords == 0) {
            totalRecords = windowDateStats.stream().mapToLong(AisDatasetDateStat::recordCount).sum();
        }
        if (totalRecords == 0) {
            totalRecords = recentPage.total();
        }
        long riskSignalCount = riskSummary.lowSpeedCount() + riskSummary.stoppedCount() + riskSummary.abnormalNoteCount();
        List<AiReportDtos.AiReportDateStat> topDates = windowDateStats.stream()
                .sorted(Comparator.comparingLong(AisDatasetDateStat::recordCount).reversed())
                .limit(7)
                .map(item -> new AiReportDtos.AiReportDateStat(item.datasetDate(), item.recordCount()))
                .toList();
        List<AiReportDtos.AiReportRankingStat> importerMetrics = topImporters.stream()
                .limit(5)
                .map(item -> new AiReportDtos.AiReportRankingStat(item.label(), item.recordCount()))
                .toList();
        String latestDatasetDate = allDateStats.stream()
                .map(AisDatasetDateStat::datasetDate)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(periodEnd.toLocalDate().toString());
        AiReportDtos.AiReportMetrics metrics = new AiReportDtos.AiReportMetrics(
                periodStart,
                periodEnd,
                latestDatasetDate,
                totalRecords,
                riskSummary.uniqueVesselCount(),
                riskSummary.lowSpeedCount(),
                riskSummary.stoppedCount(),
                riskSummary.abnormalNoteCount(),
                riskSignalCount,
                topDates,
                importerMetrics
        );
        return new AisReportContext(metrics, recentRecords, window.days(), rangeLabel(window));
    }

    private GeneratedReport generateWithAi(
            String reportType,
            AisReportContext context,
            List<RagSearchHit> ragHits
    ) {
        String promptContext = buildContext(context, ragHits);
        try {
            JsonNode node = aiModelGateway.deepSeekJson(List.of(
                    AiModelGateway.message("system", """
                            你是 ShipInsight 的 AIS 船舶交通态势分析员。请基于系统统计数据生成简洁、可复核、可归档的中文交通态势报告。
                            只返回 JSON，不要 Markdown。不要编造系统数据之外的船舶数量、日期、风险或结论。
                            """),
                    AiModelGateway.message("user", """
                            报告类型：%s
                            统计范围：%s
                            系统数据：
                            %s

                            请返回 JSON：
                            {
                              "title": "",
                              "summary": "",
                              "highlights": [],
                              "risks": [],
                              "recommendations": [],
                              "evidence": []
                            }
                            每个数组控制在 3 到 6 条，内容要能被交通态势研判人员直接复核。
                            """.formatted(reportTypeLabel(reportType), context.rangeLabel(), promptContext))
            ));
            return new GeneratedReport(
                    firstNonBlank(text(node, "title"), defaultTitle(reportType, context.rangeLabel())),
                    firstNonBlank(text(node, "summary"), fallbackSummary(context.metrics(), context.rangeLabel())),
                    nonEmptyList(node.path("highlights"), fallbackHighlights(context.metrics())),
                    nonEmptyList(node.path("risks"), fallbackRisks(context.metrics())),
                    nonEmptyList(node.path("recommendations"), fallbackRecommendations(context.metrics())),
                    nonEmptyList(node.path("evidence"), fallbackEvidence(context, ragHits))
            );
        } catch (RuntimeException ignored) {
            return new GeneratedReport(
                    defaultTitle(reportType, context.rangeLabel()),
                    fallbackSummary(context.metrics(), context.rangeLabel()),
                    fallbackHighlights(context.metrics()),
                    fallbackRisks(context.metrics()),
                    fallbackRecommendations(context.metrics()),
                    fallbackEvidence(context, ragHits)
            );
        }
    }

    private List<RagSearchHit> retrieveReportEvidence(String reportType, AisReportContext context) {
        if (ragKnowledgeService == null) {
            return List.of();
        }
        try {
            String query = "AIS 交通态势 " + reportTypeLabel(reportType)
                    + " " + context.rangeLabel() + " 风险 航运节点 异常 船舶动态 "
                    + "记录数 " + context.metrics().totalRecords();
            return ragKnowledgeService.retrieveForScenario(RagKnowledgeService.SCENARIO_REPORT, query, 6);
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private String buildContext(AisReportContext context, List<RagSearchHit> ragHits) {
        AiReportDtos.AiReportMetrics metrics = context.metrics();
        List<String> lines = new ArrayList<>();
        lines.add("统计窗口=" + formatDateTime(metrics.periodStart()) + " 至 " + formatDateTime(metrics.periodEnd()));
        lines.add("最新数据集日期=" + firstNonBlank(metrics.latestDatasetDate(), "暂无"));
        lines.add("AIS记录总数=" + metrics.totalRecords() + "，唯一船舶数=" + metrics.uniqueVesselCount());
        lines.add("风险信号=" + metrics.riskSignalCount() + "，低速=" + metrics.lowSpeedCount()
                + "，停泊/近静止=" + metrics.stoppedCount() + "，异常备注=" + metrics.abnormalNoteCount());
        lines.add("AIS日期峰值=" + summarizeDateStats(metrics.topDates()));
        lines.add("导入人排行=" + summarizeRankingStats(metrics.topImporters()));
        if (!context.recentRecords().isEmpty()) {
            lines.add("近期AIS样本=" + context.recentRecords().stream().limit(6).map(this::describeAisRecord).toList());
        }
        if (!ragHits.isEmpty()) {
            lines.add("RAG证据=" + ragHits.stream().limit(5)
                    .map(item -> firstNonBlank(item.title(), "未命名证据") + "：" + firstNonBlank(item.summary(), "无摘要"))
                    .toList());
        }
        return String.join("\n", lines);
    }

    private List<String> fallbackHighlights(AiReportDtos.AiReportMetrics metrics) {
        List<String> highlights = new ArrayList<>();
        if (metrics.totalRecords() == 0) {
            highlights.add("统计窗口内未检索到 AIS 记录，当前报告仅保留生成条件和空样本状态。");
            return highlights;
        }
        highlights.add("统计窗口覆盖 " + formatCount(metrics.totalRecords()) + " 条 AIS 记录，涉及约 "
                + formatCount(metrics.uniqueVesselCount()) + " 艘唯一船舶。");
        metrics.topDates().stream().findFirst().ifPresent(item ->
                highlights.add("窗口内 AIS 记录峰值日期为 " + item.datasetDate() + "，当天记录 "
                        + formatCount(item.recordCount()) + " 条。"));
        metrics.topImporters().stream().findFirst().ifPresent(item ->
                highlights.add("导入量最高的录入人为 " + item.label() + "，窗口内记录 "
                        + formatCount(item.recordCount()) + " 条。"));
        highlights.add("最新可用 AIS 数据集日期为 " + firstNonBlank(metrics.latestDatasetDate(), "暂无") + "。");
        return highlights;
    }

    private List<String> fallbackRisks(AiReportDtos.AiReportMetrics metrics) {
        List<String> risks = new ArrayList<>();
        if (metrics.totalRecords() == 0) {
            risks.add("统计窗口内没有可分析 AIS 样本，暂不输出交通趋势或异常风险判断。");
            return risks;
        }
        if (metrics.riskSignalCount() > 0) {
            risks.add("窗口内发现 " + formatCount(metrics.riskSignalCount()) + " 条风险信号，需要优先复核低速、停泊/近静止和异常备注记录。");
            if (metrics.abnormalNoteCount() > 0) {
                risks.add("异常备注命中 " + formatCount(metrics.abnormalNoteCount()) + " 条，建议抽样核对原始记录和关联船舶档案。");
            }
            if (metrics.stoppedCount() > 0) {
                risks.add("停泊或近静止记录 " + formatCount(metrics.stoppedCount()) + " 条，需结合航线地图确认是否处于港区、锚地或异常水域。");
            }
        } else {
            risks.add("未发现低速、停泊/近静止或备注异常等显性 AIS 风险信号。");
            risks.add("仍建议对高流量日期和关键航运节点做抽样复核，避免数据延迟或导入缺口影响判断。");
        }
        return risks;
    }

    private List<String> fallbackRecommendations(AiReportDtos.AiReportMetrics metrics) {
        List<String> recommendations = new ArrayList<>();
        recommendations.add("优先复核统计窗口内记录峰值日期的 AIS 明细，确认导入批次、时间范围和坐标质量。");
        recommendations.add("将风险信号记录与船舶档案、航线地图联动核查，形成可追溯的异常复核清单。");
        if (metrics.riskSignalCount() > 0) {
            recommendations.add("对低速、停泊/近静止和异常备注记录按 MMSI 聚合，优先处理重复出现的船舶。");
        } else {
            recommendations.add("维持现有日报/周报节奏，并继续监控最新数据集日期是否连续更新。");
        }
        return recommendations;
    }

    private List<String> fallbackEvidence(AisReportContext context, List<RagSearchHit> ragHits) {
        AiReportDtos.AiReportMetrics metrics = context.metrics();
        List<String> evidence = new ArrayList<>(List.of(
                "统计窗口：" + formatDateTime(metrics.periodStart()) + " 至 " + formatDateTime(metrics.periodEnd()),
                "AIS 记录总数：" + formatCount(metrics.totalRecords()),
                "唯一船舶数：" + formatCount(metrics.uniqueVesselCount()),
                "风险信号合计：" + formatCount(metrics.riskSignalCount()),
                "最新数据集日期：" + firstNonBlank(metrics.latestDatasetDate(), "暂无")
        ));
        if (!metrics.topDates().isEmpty()) {
            evidence.add("AIS 日期峰值：" + summarizeDateStats(metrics.topDates()));
        }
        if (!metrics.topImporters().isEmpty()) {
            evidence.add("导入人排行：" + summarizeRankingStats(metrics.topImporters()));
        }
        context.recentRecords().stream().limit(3)
                .map(record -> "AIS 样本：" + describeAisRecord(record))
                .forEach(evidence::add);
        ragHits.stream().limit(3)
                .map(item -> "RAG 证据：" + firstNonBlank(item.title(), "未命名证据")
                        + "（相似度 " + String.format(Locale.ROOT, "%.2f", item.score()) + "）")
                .forEach(evidence::add);
        return evidence;
    }

    private String fallbackSummary(AiReportDtos.AiReportMetrics metrics, String rangeLabel) {
        if (metrics.totalRecords() == 0) {
            return rangeLabel + "统计窗口内暂未检索到 AIS 记录，报告保留时间窗、生成条件和空样本状态，建议先核查 ClickHouse 数据集与导入任务。";
        }
        return rangeLabel + "内，系统基于 " + formatCount(metrics.totalRecords()) + " 条 AIS 记录、"
                + formatCount(metrics.uniqueVesselCount()) + " 艘唯一船舶生成交通态势报告；窗口内风险信号合计 "
                + formatCount(metrics.riskSignalCount()) + " 条。";
    }

    private String defaultTitle(String reportType, String rangeLabel) {
        return "ShipInsight " + reportTypeLabel(reportType) + "（" + rangeLabel + "）";
    }

    private String reportTypeLabel(String reportType) {
        return switch (reportType) {
            case "MONTHLY" -> "AIS 月报";
            case "WEEKLY" -> "AIS 周报";
            default -> "AIS 专题报告";
        };
    }

    private AiReportDtos.AiReportView toView(AiReport report) {
        return new AiReportDtos.AiReportView(
                report.getId(),
                report.getReportType(),
                report.getDays() == null ? 30 : report.getDays(),
                report.getPeriodStart(),
                report.getPeriodEnd(),
                report.getTitle(),
                report.getSummary(),
                report.getCreatedBy(),
                report.getCreatorName(),
                report.getCreatedAt()
        );
    }

    private AiReportDtos.AiReportDetailView toDetail(AiReport report) {
        return new AiReportDtos.AiReportDetailView(
                report.getId(),
                report.getReportType(),
                report.getDays() == null ? 30 : report.getDays(),
                report.getPeriodStart(),
                report.getPeriodEnd(),
                report.getTitle(),
                report.getSummary(),
                readStringList(report.getHighlightsJson()),
                readStringList(report.getRisksJson()),
                readStringList(report.getRecommendationsJson()),
                readStringList(report.getEvidenceJson()),
                readMetrics(report),
                report.getCreatedBy(),
                report.getCreatorName(),
                report.getCreatedAt()
        );
    }

    private AiReportDtos.AiReportMetrics readMetrics(AiReport report) {
        if (!StringUtils.hasText(report.getMetricsJson())) {
            return null;
        }
        try {
            return objectMapper.readValue(report.getMetricsJson(), AiReportDtos.AiReportMetrics.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private int sanitizeDays(Integer days) {
        int value = days == null ? 30 : days;
        return Math.min(Math.max(value, 1), 365);
    }

    private String normalizeReportType(String reportType, int days, boolean customRange) {
        if (!StringUtils.hasText(reportType)) {
            if (customRange) {
                return "CUSTOM";
            }
            return days <= 7 ? "WEEKLY" : days <= 31 ? "MONTHLY" : "CUSTOM";
        }
        String normalized = reportType.trim().toUpperCase(Locale.ROOT);
        if (!List.of("WEEKLY", "MONTHLY", "CUSTOM").contains(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "报告类型仅支持 WEEKLY、MONTHLY、CUSTOM", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private RequestedReportWindow validateRequestedWindow(
            LocalDateTime observedFrom,
            LocalDateTime observedTo,
            int fallbackDays
    ) {
        if (observedFrom == null && observedTo == null) {
            return new RequestedReportWindow(null, null, fallbackDays, false);
        }
        if (observedFrom == null || observedTo == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择完整的开始和结束时间", HttpStatus.BAD_REQUEST);
        }
        if (observedFrom.isAfter(observedTo)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "开始时间不能晚于结束时间", HttpStatus.BAD_REQUEST);
        }
        return new RequestedReportWindow(observedFrom, observedTo, calculateWindowDays(observedFrom, observedTo), true);
    }

    private int calculateWindowDays(LocalDateTime periodStart, LocalDateTime periodEnd) {
        long days = ChronoUnit.DAYS.between(periodStart.toLocalDate(), periodEnd.toLocalDate()) + 1;
        return (int) Math.min(Math.max(days, 1), Integer.MAX_VALUE);
    }

    private String rangeLabel(RequestedReportWindow window) {
        if (window.custom()) {
            return formatDateTime(window.periodStart()) + " 至 " + formatDateTime(window.periodEnd());
        }
        return "近 " + window.days() + " 天";
    }

    private LocalDateTime latestDatasetEnd(List<AisDatasetDateStat> stats) {
        if (stats == null || stats.isEmpty()) {
            return null;
        }
        for (AisDatasetDateStat stat : stats) {
            if (!StringUtils.hasText(stat.datasetDate())) {
                continue;
            }
            try {
                return LocalDate.parse(stat.datasetDate()).atTime(LocalTime.of(23, 59, 59));
            } catch (DateTimeParseException ignored) {
                // Continue to the next available dataset date.
            }
        }
        return null;
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("").trim();
    }

    private List<String> nonEmptyList(JsonNode node, List<String> fallback) {
        List<String> values = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                String value = item.asText("").trim();
                if (StringUtils.hasText(value)) {
                    values.add(value);
                }
            }
        }
        return values.isEmpty() ? fallback : values;
    }

    private List<String> readStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() { });
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI 报告保存失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String summarizeDateStats(List<AiReportDtos.AiReportDateStat> stats) {
        if (stats == null || stats.isEmpty()) {
            return "暂无";
        }
        return stats.stream()
                .limit(5)
                .map(item -> item.datasetDate() + "=" + formatCount(item.recordCount()))
                .toList()
                .toString();
    }

    private String summarizeRankingStats(List<AiReportDtos.AiReportRankingStat> stats) {
        if (stats == null || stats.isEmpty()) {
            return "暂无";
        }
        return stats.stream()
                .limit(5)
                .map(item -> firstNonBlank(item.label(), "未记录录入人") + "=" + formatCount(item.recordCount()))
                .toList()
                .toString();
    }

    private String describeAisRecord(AisRecordView record) {
        return firstNonBlank(record.vesselName(), record.mmsi(), "未命名船舶")
                + formatInline("MMSI", record.mmsi())
                + formatInline("时间", formatDateTime(record.baseDateTime()))
                + formatInline("航速", formatDecimal(record.sog()))
                + formatInline("状态", record.status() == null ? "" : String.valueOf(record.status()))
                + formatInline("来源", record.sourceFile());
    }

    private String formatInline(String label, String value) {
        return StringUtils.hasText(value) ? "；" + label + " " + value : "";
    }

    private String formatDecimal(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return String.format(Locale.ROOT, "%.1f", value.doubleValue());
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.toString().replace('T', ' ');
    }

    private String formatCount(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record AisReportContext(
            AiReportDtos.AiReportMetrics metrics,
            List<AisRecordView> recentRecords,
            int days,
            String rangeLabel
    ) {
    }

    private record RequestedReportWindow(
            LocalDateTime periodStart,
            LocalDateTime periodEnd,
            int days,
            boolean custom
    ) {
    }

    private record GeneratedReport(
            String title,
            String summary,
            List<String> highlights,
            List<String> risks,
            List<String> recommendations,
            List<String> evidence
    ) {
    }
}
