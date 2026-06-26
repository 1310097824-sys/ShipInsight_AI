package com.gsmv.ai.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gsmv.ai.AiModelGateway;
import com.gsmv.ai.rag.RagKnowledgeService;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class AiReportServiceTests {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private AiReportMapper mapper;
    private AisService aisService;
    private AiModelGateway gateway;
    private RagKnowledgeService ragKnowledgeService;
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        mapper = mock(AiReportMapper.class);
        aisService = mock(AisService.class);
        gateway = mock(AiModelGateway.class);
        ragKnowledgeService = mock(RagKnowledgeService.class);
        auditService = mock(AuditService.class);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(testJwt()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void generateUsesAisMetricsAndFallbackWhenAiFails() {
        AtomicReference<AiReport> saved = new AtomicReference<>();
        doAnswer(invocation -> {
            AiReport report = invocation.getArgument(0);
            report.setId(42L);
            saved.set(report);
            return null;
        }).when(mapper).insert(any(AiReport.class));
        when(mapper.findById(42L)).thenAnswer(invocation -> {
            AiReport report = saved.get();
            report.setCreatorName("tester");
            report.setCreatedAt(LocalDateTime.of(2026, 6, 23, 10, 0));
            return report;
        });
        when(gateway.deepSeekJson(anyList())).thenThrow(new RuntimeException("AI unavailable"));
        when(ragKnowledgeService.retrieveForScenario(anyString(), anyString(), anyInt())).thenReturn(List.of());
        when(aisService.datasetDateStats(
                ArgumentMatchers.<String>isNull(),
                ArgumentMatchers.<LocalDateTime>isNull(),
                ArgumentMatchers.<LocalDateTime>isNull()
        )).thenReturn(List.of(
                new AisDatasetDateStat("2025-04-30", 800),
                new AisDatasetDateStat("2025-04-29", 500)
        ));
        when(aisService.datasetDateStats(
                ArgumentMatchers.<String>isNull(),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of(
                new AisDatasetDateStat("2025-04-30", 800),
                new AisDatasetDateStat("2025-04-29", 500)
        ));
        when(aisService.riskSummary(ArgumentMatchers.<String>isNull(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new AisRiskSummary(1300, 12, 4, 3, 33));
        when(aisService.importerStats(ArgumentMatchers.<String>isNull(), any(LocalDateTime.class), any(LocalDateTime.class), eq(5)))
                .thenReturn(List.of(new AisRankingStat("tester", 900)));
        when(aisService.list(ArgumentMatchers.<String>isNull(), any(LocalDateTime.class), any(LocalDateTime.class), eq(1), eq(8)))
                .thenReturn(new PageResponse<>(List.of(sampleRecord()), 1300, 1, 8));

        AiReportService service = service();
        AiReportDtos.AiReportDetailView detail = service.generate(new AiReportDtos.GenerateReportRequest("MONTHLY", 30, null, null));

        assertEquals(42L, detail.id());
        assertNotNull(detail.metrics());
        assertEquals(1300, detail.metrics().totalRecords());
        assertEquals(33, detail.metrics().uniqueVesselCount());
        assertEquals(19, detail.metrics().riskSignalCount());
        assertEquals("2025-04-30", detail.metrics().latestDatasetDate());
        assertTrue(detail.title().contains("ShipInsight"));
        assertTrue(detail.summary().contains("1,300"));
        assertTrue(detail.evidence().stream().anyMatch(item -> item.contains("AIS 记录总数")));
        assertEquals(LocalDateTime.of(2025, 4, 30, 23, 59, 59), saved.get().getPeriodEnd());
        assertEquals(LocalDateTime.of(2025, 3, 31, 23, 59, 59), saved.get().getPeriodStart());
        assertTrue(saved.get().getMetricsJson().contains("totalRecords"));
        verify(ragKnowledgeService).syncAiReport(42L);
    }

    @Test
    void generateUsesCustomObservedRangeWhenProvided() {
        LocalDateTime observedFrom = LocalDateTime.of(2025, 4, 20, 6, 0);
        LocalDateTime observedTo = LocalDateTime.of(2025, 4, 23, 18, 30);
        AtomicReference<AiReport> saved = new AtomicReference<>();
        doAnswer(invocation -> {
            AiReport report = invocation.getArgument(0);
            report.setId(43L);
            saved.set(report);
            return null;
        }).when(mapper).insert(any(AiReport.class));
        when(mapper.findById(43L)).thenAnswer(invocation -> {
            AiReport report = saved.get();
            report.setCreatorName("tester");
            report.setCreatedAt(LocalDateTime.of(2026, 6, 23, 10, 0));
            return report;
        });
        when(gateway.deepSeekJson(anyList())).thenThrow(new RuntimeException("AI unavailable"));
        when(ragKnowledgeService.retrieveForScenario(anyString(), anyString(), anyInt())).thenReturn(List.of());
        when(aisService.datasetDateStats(
                ArgumentMatchers.<String>isNull(),
                ArgumentMatchers.<LocalDateTime>isNull(),
                ArgumentMatchers.<LocalDateTime>isNull()
        )).thenReturn(List.of(new AisDatasetDateStat("2025-05-01", 1000)));
        when(aisService.datasetDateStats(ArgumentMatchers.<String>isNull(), eq(observedFrom), eq(observedTo)))
                .thenReturn(List.of(new AisDatasetDateStat("2025-04-23", 320)));
        when(aisService.riskSummary(ArgumentMatchers.<String>isNull(), eq(observedFrom), eq(observedTo)))
                .thenReturn(new AisRiskSummary(320, 8, 2, 1, 11));
        when(aisService.importerStats(ArgumentMatchers.<String>isNull(), eq(observedFrom), eq(observedTo), eq(5)))
                .thenReturn(List.of(new AisRankingStat("custom-user", 320)));
        when(aisService.list(ArgumentMatchers.<String>isNull(), eq(observedFrom), eq(observedTo), eq(1), eq(8)))
                .thenReturn(new PageResponse<>(List.of(sampleRecord()), 320, 1, 8));

        AiReportDtos.AiReportDetailView detail = service().generate(
                new AiReportDtos.GenerateReportRequest("CUSTOM", 90, observedFrom, observedTo)
        );

        assertEquals(43L, detail.id());
        assertEquals(4, detail.days());
        assertEquals(observedFrom, detail.periodStart());
        assertEquals(observedTo, detail.periodEnd());
        assertEquals(observedFrom, detail.metrics().periodStart());
        assertEquals(observedTo, detail.metrics().periodEnd());
        assertEquals(320, detail.metrics().totalRecords());
        assertEquals("2025-05-01", detail.metrics().latestDatasetDate());
        assertEquals(observedFrom, saved.get().getPeriodStart());
        assertEquals(observedTo, saved.get().getPeriodEnd());
        assertEquals(4, saved.get().getDays());
        assertTrue(detail.title().contains("2025-04-20 06:00"));
        verify(aisService).datasetDateStats(ArgumentMatchers.<String>isNull(), eq(observedFrom), eq(observedTo));
        verify(aisService).riskSummary(ArgumentMatchers.<String>isNull(), eq(observedFrom), eq(observedTo));
        verify(aisService).importerStats(ArgumentMatchers.<String>isNull(), eq(observedFrom), eq(observedTo), eq(5));
        verify(aisService).list(ArgumentMatchers.<String>isNull(), eq(observedFrom), eq(observedTo), eq(1), eq(8));
    }

    @Test
    void generateRejectsInvalidReportTypeBeforeAisQuery() {
        AiReportService service = service();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.generate(new AiReportDtos.GenerateReportRequest("DAILY", 30, null, null))
        );

        assertEquals(ErrorCode.BAD_REQUEST, exception.getCode());
        verifyNoInteractions(aisService);
    }

    @Test
    void generateRejectsIncompleteCustomRangeBeforeAisQuery() {
        AiReportService service = service();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.generate(new AiReportDtos.GenerateReportRequest(
                        "CUSTOM",
                        30,
                        LocalDateTime.of(2025, 4, 20, 0, 0),
                        null
                ))
        );

        assertEquals(ErrorCode.BAD_REQUEST, exception.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verifyNoInteractions(aisService);
    }

    @Test
    void generatePropagatesAisUnavailableAndDoesNotInsertReport() {
        when(aisService.datasetDateStats(
                ArgumentMatchers.<String>isNull(),
                ArgumentMatchers.<LocalDateTime>isNull(),
                ArgumentMatchers.<LocalDateTime>isNull()
        )).thenThrow(new BusinessException(ErrorCode.BAD_REQUEST, "ClickHouse AIS unavailable", HttpStatus.SERVICE_UNAVAILABLE));
        AiReportService service = service();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.generate(new AiReportDtos.GenerateReportRequest("WEEKLY", 7, null, null))
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        verifyNoInteractions(mapper);
    }

    @Test
    void detailAllowsLegacyReportWithoutMetrics() {
        AiReport report = legacyReport();
        when(mapper.findById(7L)).thenReturn(report);

        AiReportDtos.AiReportDetailView detail = service().getDetail(7L);

        assertEquals(7L, detail.id());
        assertNull(detail.metrics());
        assertEquals(List.of("重点"), detail.highlights());
    }

    @Test
    void pdfExporterWritesBytesWithMetrics() {
        AiReportDtos.AiReportMetrics metrics = new AiReportDtos.AiReportMetrics(
                LocalDateTime.of(2025, 3, 31, 23, 59, 59),
                LocalDateTime.of(2025, 4, 30, 23, 59, 59),
                "2025-04-30",
                1300,
                33,
                12,
                4,
                3,
                19,
                List.of(new AiReportDtos.AiReportDateStat("2025-04-30", 800)),
                List.of(new AiReportDtos.AiReportRankingStat("tester", 900))
        );
        AiReportDtos.AiReportDetailView detail = new AiReportDtos.AiReportDetailView(
                1L,
                "MONTHLY",
                30,
                LocalDateTime.of(2025, 3, 31, 23, 59, 59),
                LocalDateTime.of(2025, 4, 30, 23, 59, 59),
                "ShipInsight AIS 月报",
                "摘要",
                List.of("发现"),
                List.of("风险"),
                List.of("建议"),
                List.of("依据"),
                metrics,
                1L,
                "tester",
                LocalDateTime.of(2026, 6, 23, 10, 0)
        );

        byte[] pdf = AiReportPdfExporter.export(detail);

        assertTrue(pdf.length > 100);
    }

    private AiReportService service() {
        return new AiReportService(mapper, aisService, gateway, ragKnowledgeService, auditService, objectMapper);
    }

    private Jwt testJwt() {
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("tester")
                .claim("userId", 1L)
                .claim("displayName", "tester")
                .claim("authorities", List.of("REPORT_READ"))
                .build();
    }

    private AisRecordView sampleRecord() {
        return new AisRecordView(
                "r1",
                "123456789",
                LocalDateTime.of(2025, 4, 30, 8, 30),
                BigDecimal.valueOf(110.1),
                BigDecimal.valueOf(21.2),
                BigDecimal.valueOf(4.5),
                BigDecimal.valueOf(82),
                80,
                "CLEVELAND",
                "IMO1234567",
                "CALL",
                70,
                0,
                null,
                null,
                null,
                null,
                "A",
                "normal",
                "ais.csv",
                1L,
                "tester",
                LocalDateTime.of(2025, 4, 30, 9, 0),
                null
        );
    }

    private AiReport legacyReport() {
        AiReport report = new AiReport();
        report.setId(7L);
        report.setReportType("MONTHLY");
        report.setDays(30);
        report.setTitle("旧报告");
        report.setSummary("旧摘要");
        report.setHighlightsJson("[\"重点\"]");
        report.setRisksJson("[\"风险\"]");
        report.setRecommendationsJson("[\"建议\"]");
        report.setEvidenceJson("[\"依据\"]");
        report.setCreatedBy(1L);
        report.setCreatorName("tester");
        report.setCreatedAt(LocalDateTime.of(2026, 6, 23, 10, 0));
        return report;
    }
}
