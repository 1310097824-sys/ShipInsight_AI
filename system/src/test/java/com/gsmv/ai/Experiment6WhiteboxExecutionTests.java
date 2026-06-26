package com.gsmv.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gsmv.ai.dto.AssistantAiDtos;
import com.gsmv.ai.dto.ObservationAiDtos;
import com.gsmv.ai.history.AssistantChatHistoryService;
import com.gsmv.ai.rag.RagTextChunker;
import com.gsmv.ai.rag.RagKnowledgeService;
import com.gsmv.ais.AisClickHouseProperties;
import com.gsmv.ais.AisService;
import com.gsmv.ais.dto.AisDatasetDateStat;
import com.gsmv.ais.dto.AisRankingStat;
import com.gsmv.ais.dto.AisRecordView;
import com.gsmv.ais.dto.AisRiskSummary;
import com.gsmv.ai.rag.RagVectorUtils;
import com.gsmv.audit.service.AuditService;
import com.gsmv.common.PageResponse;
import com.gsmv.common.exception.BusinessException;
import com.gsmv.vessel.VesselService;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

class Experiment6WhiteboxExecutionTests {

    private final RagTextChunker chunker = new RagTextChunker();

    @Test
    void utC01NullTextReturnsEmpty() {
        assertTrue(chunker.chunk("t", null).isEmpty());
    }

    @Test
    void utC02BlankTextReturnsEmpty() {
        assertTrue(chunker.chunk("t", " \t \n ").isEmpty());
    }

    @Test
    void utC03ShortTextCreatesSingleChunk() {
        List<RagTextChunker.ChunkDraft> chunks = chunker.chunk("title", "abc def");
        assertEquals(1, chunks.size());
        assertEquals(0, chunks.get(0).index());
        assertEquals("title", chunks.get(0).title());
    }

    @Test
    void utC04TextNormalizationRemovesExtraWhitespace() {
        List<RagTextChunker.ChunkDraft> chunks = chunker.chunk("t", " a\t\tb\r\n\r\n\r\nc ");
        assertEquals("a b\n\nc", chunks.get(0).content());
    }

    @Test
    void utC05LongTextCreatesMultipleChunks() {
        List<RagTextChunker.ChunkDraft> chunks = chunker.chunk("long", "x".repeat(1300));
        assertTrue(chunks.size() >= 3);
    }

    @Test
    void utC06LongChunksKeepOverlap() {
        List<RagTextChunker.ChunkDraft> chunks = chunker.chunk("long", "0123456789".repeat(130));
        assertTrue(chunks.size() >= 2);
        String firstTail = chunks.get(0).content().substring(chunks.get(0).content().length() - 80);
        assertTrue(chunks.get(1).content().contains(firstTail.substring(0, 20)));
    }

    @Test
    void utC07SummaryIsTruncatedForLongContent() {
        List<RagTextChunker.ChunkDraft> chunks = chunker.chunk("long", "abcdef ".repeat(100));
        assertTrue(chunks.get(0).summary().length() <= 183);
        assertTrue(chunks.get(0).summary().endsWith("..."));
    }

    @Test
    void utV01CosineNullVectorReturnsZero() {
        assertEquals(0.0d, RagVectorUtils.cosine(null, List.of(1.0d)));
    }

    @Test
    void utV02CosineEmptyVectorReturnsZero() {
        assertEquals(0.0d, RagVectorUtils.cosine(List.of(), List.of(1.0d)));
    }

    @Test
    void utV03CosineIdenticalVectorReturnsOne() {
        assertEquals(1.0d, RagVectorUtils.cosine(List.of(1.0d, 2.0d), List.of(1.0d, 2.0d)), 0.000001d);
    }

    @Test
    void utV04CosineOrthogonalVectorReturnsZero() {
        assertEquals(0.0d, RagVectorUtils.cosine(List.of(1.0d, 0.0d), List.of(0.0d, 1.0d)), 0.000001d);
    }

    @Test
    void utV05CosineTreatsNullElementAsZero() {
        assertEquals(1.0d, RagVectorUtils.cosine(Arrays.asList(null, 2.0d), List.of(0.0d, 2.0d)), 0.000001d);
    }

    @Test
    void utV06CosineClampsNegativeResultToZero() {
        assertEquals(0.0d, RagVectorUtils.cosine(List.of(-1.0d, 0.0d), List.of(1.0d, 0.0d)), 0.000001d);
    }

    @Test
    void utK01KeywordBlankQueryReturnsZero() {
        assertEquals(0.0d, RagVectorUtils.keywordScore("", "panda"));
    }

    @Test
    void utK02KeywordBlankTextReturnsZero() {
        assertEquals(0.0d, RagVectorUtils.keywordScore("panda", ""));
    }

    @Test
    void utK03KeywordFullPhraseReturnsOne() {
        assertEquals(1.0d, RagVectorUtils.keywordScore("giant panda", "giant panda appears here"));
    }

    @Test
    void utK04KeywordPartialTermsScoreFraction() {
        assertEquals(0.5d, RagVectorUtils.keywordScore("panda tiger", "panda only"), 0.000001d);
    }

    @Test
    void utK05KeywordPunctuationSplitMatchesTerms() {
        assertEquals(1.0d, RagVectorUtils.keywordScore("panda, tiger", "tiger and panda"));
    }

    @Test
    void utK06KeywordShortChineseFallbackMatches() {
        assertTrue(RagVectorUtils.keywordScore("golden monkey", "sichuan golden monkey record") > 0.0d);
    }

    @Test
    void utQ01CacheMissingReturnsNull() {
        assertNull(new AssistantQueryCache().get("missing"));
    }

    @Test
    void utQ02CachePutThenGetHits() {
        AssistantQueryCache cache = new AssistantQueryCache();
        AssistantAiDtos.ChatResponse response = chatResponse("ok");
        cache.put("k", response);
        assertSame(response, cache.get("k"));
    }

    @Test
    void utQ03CacheIgnoresNullKey() {
        AssistantQueryCache cache = new AssistantQueryCache();
        cache.put(null, chatResponse("ignored"));
        assertEquals(0, cacheSize(cache));
    }

    @Test
    void utQ04CacheIgnoresNullResponse() {
        AssistantQueryCache cache = new AssistantQueryCache();
        cache.put("k", null);
        assertNull(cache.get("k"));
    }

    @Test
    void utQ05CacheInvalidateAllClearsEntries() {
        AssistantQueryCache cache = new AssistantQueryCache();
        cache.put("k", chatResponse("ok"));
        cache.invalidateAll();
        assertNull(cache.get("k"));
    }

    @Test
    void utQ06CacheOverflowEvictsOldestEntry() {
        AssistantQueryCache cache = new AssistantQueryCache();
        for (int i = 0; i < 257; i++) {
            cache.put("k" + i, chatResponse("r" + i));
        }
        assertNull(cache.get("k0"));
        assertNotNull(cache.get("k256"));
    }

    @Test
    void utQ07ExpiredCacheEntryIsRemoved() throws Exception {
        AssistantQueryCache cache = new AssistantQueryCache();
        Field entriesField = AssistantQueryCache.class.getDeclaredField("entries");
        entriesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, Object> entries = (ConcurrentHashMap<String, Object>) entriesField.get(cache);
        Class<?> entryClass = Class.forName("com.gsmv.ai.AssistantQueryCache$CacheEntry");
        Constructor<?> constructor = entryClass.getDeclaredConstructor(AssistantAiDtos.ChatResponse.class, long.class);
        constructor.setAccessible(true);
        entries.put("expired", constructor.newInstance(chatResponse("old"), System.currentTimeMillis() - 1_000));
        assertNull(cache.get("expired"));
        assertFalse(entries.containsKey("expired"));
    }

    @Test
    void utG01GatewayParsesPlainJson() throws Exception {
        JsonNode node = parseJsonContent("{\"confidence\":0.86,\"likelyChineseName\":\"panda\"}");
        assertEquals(0.86d, node.path("confidence").asDouble(), 0.000001d);
        assertEquals("panda", node.path("likelyChineseName").asText());
    }

    @Test
    void utG02GatewayParsesMarkdownJsonFence() throws Exception {
        JsonNode node = parseJsonContent("```json\n{\"confidence\":0.7}\n```");
        assertEquals(0.7d, node.path("confidence").asDouble(), 0.000001d);
    }

    @Test
    void utG03GatewayParsesLenientJson() throws Exception {
        JsonNode node = parseJsonContent("{confidence:.5, likelyChineseName:'tiger',}");
        assertEquals(0.5d, node.path("confidence").asDouble(), 0.000001d);
        assertEquals("tiger", node.path("likelyChineseName").asText());
    }

    @Test
    void utG04GatewayExtractsObjectFromArrayLikeText() throws Exception {
        JsonNode node = parseJsonContent("[{\"a\":1}]");
        assertTrue(node.isObject());
        assertEquals(1, node.path("a").asInt());
    }

    @Test
    void utG05GatewayFallbackParsesStructuredText() throws Exception {
        JsonNode node = parseJsonContent("likelyChineseName=sample-vessel confidence=0.91 reasoning='clear image'");
        assertEquals("sample-vessel", node.path("likelyChineseName").asText());
        assertEquals(0.91d, node.path("confidence").asDouble(), 0.000001d);
    }

    @Test
    void utG06GatewayThrowsOnUnparseableText() {
        assertThrows(BusinessException.class, () -> parseJsonContent("no structured fields here"));
    }

    @Test
    @Disabled("ObservationAiService was removed in v1.2")
    void utO01ObservationRuleTagsCompleteInput() throws Exception {
        List<String> tags = buildRuleTags(completeObservation(LocalDateTime.of(2026, 5, 27, 10, 0), 31.0d, 25.0d, 6.0d, 6.0d, 1));
        assertTrue(tags.size() >= 3);
        assertTrue(tags.contains("瑗垮崡灞卞湴"));
    }

    @Test
    @Disabled("ObservationAiService was removed in v1.2")
    void utO02ObservationRuleTagsSummerDay() throws Exception {
        List<String> tags = buildRuleTags(completeObservation(LocalDateTime.of(2026, 7, 1, 12, 0), 31.0d, 25.0d, 6.0d, 6.0d, 1));
        assertTrue(tags.size() >= 3);
    }

    @Test
    @Disabled("ObservationAiService was removed in v1.2")
    void utO03ObservationRuleTagsWinterNight() throws Exception {
        List<String> tags = buildRuleTags(completeObservation(LocalDateTime.of(2026, 1, 1, 23, 0), 31.0d, 25.0d, 6.0d, 6.0d, 1));
        assertTrue(tags.size() >= 3);
    }

    @Test
    @Disabled("ObservationAiService was removed in v1.2")
    void utO04ObservationRuleTagsEnvironmentExtremes() throws Exception {
        List<String> tags = buildRuleTags(completeObservation(LocalDateTime.of(2026, 5, 27, 10, 0), 36.0d, 29.0d, 4.0d, 40.0d, 1));
        assertTrue(tags.size() >= 6);
    }

    @Test
    @Disabled("ObservationAiService was removed in v1.2")
    void utO05ObservationRuleTagsMultipleSpecies() throws Exception {
        List<String> tags = buildRuleTags(completeObservation(LocalDateTime.of(2026, 5, 27, 10, 0), 31.0d, 25.0d, 6.0d, 6.0d, 2));
        assertTrue(tags.size() >= 4);
    }

    @Test
    @Disabled("ObservationAiService was removed in v1.2")
    void utO06ObservationEnvironmentEmptyTrue() throws Exception {
        assertEquals(Boolean.TRUE, invokeObservationPrivate("isEnvironmentEmpty", new Class<?>[]{ObservationAiDtos.EnvironmentSnapshot.class},
                new ObservationAiDtos.EnvironmentSnapshot(null, null, null, null, null, null, null, null)));
    }

    @Test
    @Disabled("ObservationAiService was removed in v1.2")
    void utO07ObservationEnvironmentEmptyFalse() throws Exception {
        assertEquals(Boolean.FALSE, invokeObservationPrivate("isEnvironmentEmpty", new Class<?>[]{ObservationAiDtos.EnvironmentSnapshot.class},
                new ObservationAiDtos.EnvironmentSnapshot(BigDecimal.valueOf(25), null, null, null, null, null, null, null)));
    }

    @Test
    @Disabled("AssistantAiService private intent helpers were replaced in v1.2")
    void utA01AssistantTreatsWhatIsSpeciesAsProfileQuestion() throws Exception {
        assertEquals("vessel_profile", inferAssistantIntent("what is panda", "panda"));
    }

    @Test
    @Disabled("AssistantAiService private intent helpers were replaced in v1.2")
    void utA02AssistantKeepsDistributionQuestionAsSpeciesLookup() throws Exception {
        assertEquals("map_snapshot", inferAssistantIntent("where is panda distributed", "panda"));
    }

    @Test
    @Disabled("AssistantAiService private intent helpers were replaced in v1.2")
    void utA03AssistantTreatsCasualFoodQuestionAsGeneralChat() throws Exception {
        assertEquals("general_chat", inferAssistantIntent("is chicken tasty", "chicken"));
    }

    @Test
    @Disabled("AssistantAiService private intent helpers were replaced in v1.2")
    void utA04AssistantTreatsOpenLocationFoodQuestionAsGeneralChat() throws Exception {
        assertEquals("general_chat", inferAssistantIntent("how is yunnan chicken", "chicken"));
    }

    @Test
    @Disabled("AssistantAiService private intent helpers were replaced in v1.2")
    void utA05AssistantKeepsObservationQuestionAsObservationLookup() throws Exception {
        assertEquals("ais_records", inferAssistantIntent("what observations does chicken have", "chicken"));
    }

    @Test
    @Disabled("AssistantAiService private intent helpers were replaced in v1.2")
    void utA06AssistantTreatsUnrelatedDailyQuestionAsGeneralChat() throws Exception {
        assertEquals("general_chat", inferAssistantIntent("any good movies recently", null));
    }

    @Test
    @Disabled("AssistantAiService private keyword helpers were replaced in v1.2")
    void utA07AssistantExtractsNamedVesselForTrackQuestion() throws Exception {
        assertEquals("CLEVELAND", extractAssistantVesselKeyword("\u67e5\u770b CLEVELAND \u7684\u8f68\u8ff9"));
    }

    @Test
    @Disabled("AssistantAiService private keyword helpers were replaced in v1.2")
    void utA08AssistantExtractsImoForTrackQuestion() throws Exception {
        assertEquals("IMO1234567", extractAssistantVesselKeyword("\u5e2e\u6211\u770b IMO1234567 \u822a\u8ff9"));
    }
    @Test
    @Disabled("AssistantAiService private keyword helpers were replaced in v1.2")
    void utA09AssistantParsesChineseRecentDays() throws Exception {
        assertEquals(3, extractAssistantRecentDays("\u8fd1\u4e09\u5929 AIS \u8bb0\u5f55"));
    }

    @Test
    @Disabled("AssistantAiService private keyword helpers were replaced in v1.2")
    void utA10AssistantParsesRecentWeekAsSevenDays() throws Exception {
        assertEquals(7, extractAssistantRecentDays("\u6700\u8fd1\u4e00\u5468 AIS \u8d8b\u52bf"));
    }

    @Test
    @Disabled("AssistantAiService private keyword helpers were replaced in v1.2")
    void utA11AssistantParsesPastMonthAsThirtyDays() throws Exception {
        assertEquals(30, extractAssistantRecentDays("\u8fc7\u53bb\u4e00\u4e2a\u6708 AIS \u7edf\u8ba1"));
    }

    @Test
    @Disabled("AssistantAiService private intent helpers were replaced in v1.2")
    void utA12AssistantTreatsAisTotalQuestionAsCountIntent() throws Exception {
        assertEquals("ais_count", inferAssistantIntent("\u73b0\u5728\u7cfb\u7edf\u7684 AIS \u8bb0\u5f55\u6709\u591a\u5c11\u6761", null));
    }

    @Test
    @Disabled("AssistantAiService private intent helpers were replaced in v1.2")
    void utA13AssistantTreatsVesselArchiveCountAsVesselCountIntent() throws Exception {
        assertEquals("vessel_count", inferAssistantIntent("\u8239\u8236\u6863\u6848\u6570\u6709\u591a\u5c11?", null));
    }

    @Test
    @Disabled("AssistantAiService private date helpers were replaced in v1.2")
    void utA14AssistantParsesMonthDayKeywordForAisCount() throws Exception {
        assertEquals("4-30", extractAssistantDateKeyword("\u7cfb\u7edf4-30\u7684\u65f6\u5019\u6709\u51e0\u6761ais\u8bb0\u5f55?"));
    }

    @Test
    @Disabled("AssistantAiService internal context class was replaced in v1.2")
    void utA15AssistantCountAnswerUsesDatasetStatsWhenListIsEmpty() throws Exception {
        String answer = buildAssistantAisCountAnswer();
        assertTrue(answer.contains("1,300"));
        assertTrue(answer.contains("2025-04-29"));
        assertTrue(answer.contains("2025-04-30"));
    }

    @Test
    @Disabled("AssistantAiService intent implementation changed in v1.2")
    void utA16AssistantCountQuestionIgnoresFollowUpVesselKeyword() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(testJwt()));
        try {
            List<String> statKeywords = new ArrayList<>();
            AisClickHouseProperties properties = new AisClickHouseProperties();
            AisService aisService = new AisService(properties, new ObjectMapper(), null) {
                @Override
                public List<AisDatasetDateStat> datasetDateStats(String keyword, LocalDateTime observedFrom, LocalDateTime observedTo) {
                    statKeywords.add(keyword);
                    return List.of(
                            new AisDatasetDateStat("2025-04-30", 26092),
                            new AisDatasetDateStat("2025-04-29", 32998),
                            new AisDatasetDateStat("2025-04-28", 575190)
                    );
                }

                @Override
                public PageResponse<AisRecordView> list(String keyword, LocalDateTime observedFrom, LocalDateTime observedTo, int page, int size) {
                    return new PageResponse<>(List.of(), 0, page, size);
                }
            };
            AssistantAiService service = assistantService(aisService, null, null);

            AssistantAiDtos.ChatResponse response = service.chat(new AssistantAiDtos.ChatRequest(
                    "\u73b0\u5728\u7cfb\u7edf\u7684ais\u8bb0\u5f55\u6709\u591a\u5c11\u6761?",
                    List.of(new AssistantAiDtos.ConversationMessage("user", "\u67e5 CLEVELAND \u7684\u8f68\u8ff9"))
            ));

            assertEquals("ais_count", response.structuredQuery().intent());
            assertEquals("CLEVELAND", response.structuredQuery().vesselKeyword());
            assertTrue(statKeywords.stream().allMatch(keyword -> keyword == null || keyword.isBlank()));
            assertTrue(response.answer().contains("634,280"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @Disabled("AssistantAiService intent implementation changed in v1.2")
    void utA17AssistantUsesDateKeywordToCollectSingleDayAisStats() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(testJwt()));
        try {
            List<String> observedWindows = new ArrayList<>();
            AisClickHouseProperties properties = new AisClickHouseProperties();
            AisService aisService = new AisService(properties, new ObjectMapper(), null) {
                @Override
                public List<AisDatasetDateStat> datasetDateStats(String keyword, LocalDateTime observedFrom, LocalDateTime observedTo) {
                    if (observedFrom == null && observedTo == null) {
                        return List.of(new AisDatasetDateStat("2025-04-30", 26092), new AisDatasetDateStat("2025-04-29", 32998));
                    }
                    observedWindows.add(observedFrom + "|" + observedTo);
                    return List.of(new AisDatasetDateStat("2025-04-30", 26092));
                }

                @Override
                public PageResponse<AisRecordView> list(String keyword, LocalDateTime observedFrom, LocalDateTime observedTo, int page, int size) {
                    return new PageResponse<>(List.of(), 0, page, size);
                }
            };
            AssistantAiService service = assistantService(aisService, null, null);

            AssistantAiDtos.ChatResponse response = service.chat(new AssistantAiDtos.ChatRequest(
                    "\u7cfb\u7edf4-30\u7684\u65f6\u5019\u6709\u51e0\u6761ais\u8bb0\u5f55?",
                    List.of()
            ));

            assertEquals("ais_count", response.structuredQuery().intent());
            assertTrue(observedWindows.stream().anyMatch(window -> window.contains("2025-04-30T00:00") && window.contains("2025-04-30T23:59:59")));
            assertTrue(response.answer().contains("26,092"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @Disabled("AssistantAiService intent implementation changed in v1.2")
    void utA18AssistantAiPlanCanDriveVesselCountCollection() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(testJwt()));
        try {
            AiModelGateway gateway = mock(AiModelGateway.class);
            when(gateway.deepSeekJson(org.mockito.ArgumentMatchers.anyList()))
                    .thenReturn(new ObjectMapper().readTree("{\"intent\":\"vessel_count\"}"));
            when(gateway.deepSeekText(org.mockito.ArgumentMatchers.anyList()))
                    .thenReturn("\u7cfb\u7edf\u5f53\u524d\u8239\u8236\u6863\u6848\u603b\u6570\u662f 12 \u6761\u3002");
            VesselService vesselService = mock(VesselService.class);
            when(vesselService.countVessels()).thenReturn(12L);
            AssistantAiService service = assistantService(null, vesselService, gateway);

            AssistantAiDtos.ChatResponse response = service.chat(new AssistantAiDtos.ChatRequest("\u8239\u8236\u6863\u6848\u6570\u6709\u591a\u5c11?", List.of()));

            assertEquals("vessel_count", response.structuredQuery().intent());
            assertTrue(response.answer().contains("12"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @Disabled("AssistantAiService intent implementation changed in v1.2")
    void utA19AssistantAnswersAisTopDateQuestionWithoutClarify() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(testJwt()));
        try {
            AisService aisService = new AisService(new AisClickHouseProperties(), new ObjectMapper(), null) {
                @Override
                public List<AisDatasetDateStat> datasetDateStats(String keyword, LocalDateTime observedFrom, LocalDateTime observedTo) {
                    return List.of(
                            new AisDatasetDateStat("2025-04-30", 26092),
                            new AisDatasetDateStat("2025-04-29", 32998),
                            new AisDatasetDateStat("2025-04-28", 24627)
                    );
                }

                @Override
                public PageResponse<AisRecordView> list(String keyword, LocalDateTime observedFrom, LocalDateTime observedTo, int page, int size) {
                    return new PageResponse<>(List.of(), 0, page, size);
                }
            };
            AssistantAiService service = assistantService(aisService, null, null);

            AssistantAiDtos.ChatResponse response = service.chat(new AssistantAiDtos.ChatRequest("\u54ea\u5929\u7684 AIS \u8bb0\u5f55\u6700\u591a?", List.of()));

            assertEquals("ranking_question", response.structuredQuery().intent());
            assertTrue(response.answer().contains("2025-04-29"));
            assertTrue(response.answer().contains("32,998"));
            assertFalse(response.answer().contains("\u9700\u8981\u518d\u660e\u786e"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @Disabled("AssistantAiService intent implementation changed in v1.2")
    void utA20AssistantAnswersRecentImporterRankingQuestion() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(testJwt()));
        try {
            List<String> windows = new ArrayList<>();
            AisService aisService = new AisService(new AisClickHouseProperties(), new ObjectMapper(), null) {
                @Override
                public List<AisDatasetDateStat> datasetDateStats(String keyword, LocalDateTime observedFrom, LocalDateTime observedTo) {
                    return List.of(
                            new AisDatasetDateStat("2025-04-30", 100),
                            new AisDatasetDateStat("2025-04-29", 80)
                    );
                }

                @Override
                public List<AisRankingStat> importerStats(String keyword, LocalDateTime observedFrom, LocalDateTime observedTo, int limit) {
                    windows.add(observedFrom + "|" + observedTo);
                    return List.of(new AisRankingStat("tester", 120), new AisRankingStat("ops", 60));
                }

                @Override
                public PageResponse<AisRecordView> list(String keyword, LocalDateTime observedFrom, LocalDateTime observedTo, int page, int size) {
                    return new PageResponse<>(List.of(), 0, page, size);
                }
            };
            AssistantAiService service = assistantService(aisService, null, null);

            AssistantAiDtos.ChatResponse response = service.chat(new AssistantAiDtos.ChatRequest("\u8fd1 30 \u5929\u8c01\u5f55\u5165\u7684 AIS \u8bb0\u5f55\u6700\u591a?", List.of()));

            assertEquals("ranking_question", response.structuredQuery().intent());
            assertEquals(30, response.structuredQuery().recentDays());
            assertTrue(windows.stream().anyMatch(window -> window.contains("2025-03-31T23:59:59") && window.contains("2025-04-30T23:59:59")));
            assertTrue(response.answer().contains("tester"));
            assertTrue(response.answer().contains("120"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @Disabled("AssistantAiService intent implementation changed in v1.2")
    void utA21AssistantVesselCountDoesNotMixAisTotals() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(testJwt()));
        try {
            VesselService vesselService = mock(VesselService.class);
            when(vesselService.countVessels()).thenReturn(12L);
            AisService aisService = new AisService(new AisClickHouseProperties(), new ObjectMapper(), null) {
                @Override
                public List<AisDatasetDateStat> datasetDateStats(String keyword, LocalDateTime observedFrom, LocalDateTime observedTo) {
                    return List.of(new AisDatasetDateStat("2025-04-30", 26092));
                }
            };
            AssistantAiService service = assistantService(aisService, vesselService, null);

            AssistantAiDtos.ChatResponse response = service.chat(new AssistantAiDtos.ChatRequest("\u8239\u8236\u6863\u6848\u6570\u6709\u591a\u5c11?", List.of()));

            assertEquals("vessel_count", response.structuredQuery().intent());
            assertTrue(response.answer().contains("12"));
            assertFalse(response.answer().contains("26,092"));
            assertFalse(response.answer().contains("AIS \u8bb0\u5f55\u603b\u6570"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @Disabled("AssistantAiService intent implementation changed in v1.2")
    void utA22AssistantFallsBackToAvailableAisDateRangeWhenSpecificDateHasNoData() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(testJwt()));
        try {
            AisService aisService = new AisService(new AisClickHouseProperties(), new ObjectMapper(), null) {
                @Override
                public List<AisDatasetDateStat> datasetDateStats(String keyword, LocalDateTime observedFrom, LocalDateTime observedTo) {
                    if (observedFrom == null && observedTo == null) {
                        return List.of(
                                new AisDatasetDateStat("2025-04-30", 26092),
                                new AisDatasetDateStat("2025-04-29", 32998)
                        );
                    }
                    return List.of();
                }

                @Override
                public PageResponse<AisRecordView> list(String keyword, LocalDateTime observedFrom, LocalDateTime observedTo, int page, int size) {
                    return new PageResponse<>(List.of(), 0, page, size);
                }
            };
            AssistantAiService service = assistantService(aisService, null, null);

            AssistantAiDtos.ChatResponse response = service.chat(new AssistantAiDtos.ChatRequest("2025-05-01 AIS \u54ea\u5929\u8bb0\u5f55\u6700\u591a?", List.of()));

            assertEquals("ranking_question", response.structuredQuery().intent());
            assertTrue(response.answer().contains("2025-04-29"));
            assertTrue(response.answer().contains("32,998"));
            assertTrue(response.answer().contains("\u6307\u5b9a\u65f6\u95f4"));
            assertTrue(response.answer().contains("2025-04-29 ~ 2025-04-30"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }


    private AssistantAiService assistantService(AisService aisService, VesselService vesselService, AiModelGateway gateway) {
        AisService effectiveAisService = aisService != null ? spy(aisService) : mock(AisService.class);
        doReturn(AisRiskSummary.empty()).when(effectiveAisService).riskSummary(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
        VesselService effectiveVesselService = vesselService != null ? vesselService : mock(VesselService.class);
        when(effectiveVesselService.listVessels(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt()
        )).thenReturn(new PageResponse<>(List.of(), 0, 1, 20));
        AuditService auditService = mock(AuditService.class);
        RagKnowledgeService ragKnowledgeService = mock(RagKnowledgeService.class);
        when(ragKnowledgeService.retrieveForScenario(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt()
        )).thenReturn(List.of());
        return new AssistantAiService(
                null,
                effectiveAisService,
                effectiveVesselService,
                auditService,
                new AssistantQueryCache(),
                ragKnowledgeService,
                gateway,
                mock(AssistantChatHistoryService.class)
        );
    }
    private AssistantAiDtos.ChatResponse chatResponse(String answer) {
        return new AssistantAiDtos.ChatResponse(answer, null, List.of(), List.of(), false);
    }

    private String extractAssistantVesselKeyword(String message) throws Exception {
        AssistantAiService service = assistantService(null, null, null);
        Method method = AssistantAiService.class.getDeclaredMethod("extractVesselKeyword", String.class);
        method.setAccessible(true);
        return (String) method.invoke(service, message);
    }
    private Integer extractAssistantRecentDays(String message) throws Exception {
        AssistantAiService service = assistantService(null, null, null);
        Method method = AssistantAiService.class.getDeclaredMethod("extractRecentDays", String.class);
        method.setAccessible(true);
        return (Integer) method.invoke(service, message);
    }

    private String extractAssistantDateKeyword(String message) throws Exception {
        AssistantAiService service = assistantService(null, null, null);
        Method method = AssistantAiService.class.getDeclaredMethod("extractDateKeyword", String.class);
        method.setAccessible(true);
        return (String) method.invoke(service, message);
    }

    private String buildAssistantAisCountAnswer() throws Exception {
        AssistantAiService service = assistantService(null, null, null);
        Class<?> contextClass = Class.forName("com.gsmv.ai.AssistantAiService$AssistantContext");
        Constructor<?> constructor = contextClass.getDeclaredConstructor(
                List.class,
                long.class,
                List.class,
                long.class,
                List.class,
                long.class,
                List.class,
                LocalDateTime.class,
                LocalDateTime.class,
                String.class,
                List.class,
                List.class,
                List.class,
                List.class,
                List.class,
                AisRiskSummary.class,
                List.class,
                List.class
        );
        constructor.setAccessible(true);
        Object context = constructor.newInstance(
                List.of(),
                0L,
                List.of(),
                0L,
                List.of(),
                0L,
                List.of(),
                null,
                null,
                "",
                List.of("2025-04-30", "2025-04-29"),
                List.of(new AisDatasetDateStat("2025-04-30", 800), new AisDatasetDateStat("2025-04-29", 500)),
                List.of(new AisDatasetDateStat("2025-04-30", 800), new AisDatasetDateStat("2025-04-29", 500)),
                List.of(),
                List.of(),
                AisRiskSummary.empty(),
                List.of("AIS鏃ユ湡鑱氬悎"),
                List.of()
        );
        Method method = AssistantAiService.class.getDeclaredMethod("buildAisCountAnswer", AssistantAiDtos.StructuredQuery.class, contextClass);
        method.setAccessible(true);
        AssistantAiDtos.StructuredQuery plan = new AssistantAiDtos.StructuredQuery("ais_count", null, null, null, null, null, null, null, false, false, null);
        return (String) method.invoke(service, plan, context);
    }

    private int cacheSize(AssistantQueryCache cache) {
        try {
            Field entriesField = AssistantQueryCache.class.getDeclaredField("entries");
            entriesField.setAccessible(true);
            return ((Map<?, ?>) entriesField.get(cache)).size();
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    private Jwt testJwt() {
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("tester")
                .claim("userId", 1L)
                .claim("displayName", "tester")
                .claim("authorities", List.of("ADMIN"))
                .build();
    }

    private JsonNode parseJsonContent(String content) throws Exception {
        AiProperties properties = new AiProperties(
                new AiProperties.Bailian(false, "", "http://localhost", "vision", "embedding", 1024),
                new AiProperties.Ollama(true, "http://localhost:11434", "bge-m3", 1024),
                new AiProperties.DeepSeek(false, "", "http://localhost", "chat"),
                new AiProperties.Embedding("ollama"),
                0.68d,
                60,
                20
        );
        AiModelGateway gateway = new AiModelGateway(properties, RestClient.builder(), new ObjectMapper());
        Method method = AiModelGateway.class.getDeclaredMethod("parseJsonContent", String.class);
        method.setAccessible(true);
        try {
            Object result = method.invoke(gateway, content);
            return assertInstanceOf(JsonNode.class, result);
        } catch (InvocationTargetException ex) {
            if (ex.getCause() instanceof BusinessException businessException) {
                throw businessException;
            }
            throw ex;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> buildRuleTags(ObservationAiDtos.AnalyzeObservationRequest request) throws Exception {
        return (List<String>) invokeObservationPrivate("buildRuleTags", new Class<?>[]{ObservationAiDtos.AnalyzeObservationRequest.class}, request);
    }

    private Object invokeObservationPrivate(String methodName, Class<?>[] parameterTypes, Object... args) {
        throw new UnsupportedOperationException("ObservationAiService was removed in v1.2");
    }

    private String inferAssistantIntent(String message, String vesselKeyword) throws Exception {
        AssistantAiService service = assistantService(null, null, null);
        Method method = AssistantAiService.class.getDeclaredMethod(
                "inferIntent",
                String.class,
                String.class,
                boolean.class,
                boolean.class
        );
        method.setAccessible(true);
        return (String) method.invoke(service, message, vesselKeyword, false, false);
    }

    private ObservationAiDtos.AnalyzeObservationRequest completeObservation(
            LocalDateTime observedAt,
            double salinity,
            double waterTemperature,
            double dissolvedOxygen,
            double depth,
            int speciesCount
    ) {
        List<ObservationAiDtos.SpeciesObservationItem> species = new ArrayList<>();
        for (int i = 0; i < speciesCount; i++) {
            species.add(new ObservationAiDtos.SpeciesObservationItem(null, "Ailuropoda melanoleuca", "panda", 3, "foraging", "ok"));
        }
        return new ObservationAiDtos.AnalyzeObservationRequest(
                1L,
                "southwest mountain",
                observedAt,
                BigDecimal.valueOf(21.18d),
                BigDecimal.valueOf(110.53d),
                "nature reserve",
                "note",
                new ObservationAiDtos.EnvironmentSnapshot(
                        BigDecimal.valueOf(waterTemperature),
                        BigDecimal.valueOf(salinity),
                        BigDecimal.valueOf(8.1d),
                        BigDecimal.valueOf(dissolvedOxygen),
                        BigDecimal.valueOf(4.2d),
                        BigDecimal.valueOf(depth),
                        "sunny",
                        "calm"
                ),
                species
        );
    }
}
