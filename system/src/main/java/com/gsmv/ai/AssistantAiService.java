package com.gsmv.ai;

import com.gsmv.ai.dto.AssistantAiDtos;
import com.gsmv.ai.history.AssistantChatHistoryService;
import com.gsmv.ai.rag.RagKnowledgeService;
import com.gsmv.ai.rag.RagSearchHit;
import com.gsmv.ais.AisService;
import com.gsmv.ais.dto.AisRankingStat;
import com.gsmv.ais.dto.AisRecordView;
import com.gsmv.ais.dto.AisRiskSummary;
import com.gsmv.ais.dto.AisVesselDraftCandidate;
import com.gsmv.ais.dto.AisVesselSummaryView;
import com.gsmv.audit.service.AuditService;
import com.gsmv.common.PageResponse;
import com.gsmv.security.SecurityUtils;
import com.gsmv.vessel.VesselService;
import com.gsmv.vessel.dto.VesselDetailView;
import com.gsmv.vessel.dto.VesselTypeOption;
import com.gsmv.vessel.dto.VesselView;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AssistantAiService {

    private static final Set<String> HIGH_RISK_LEVELS = Set.of("重点关注");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern DAYS_PATTERN = Pattern.compile("(?:最近|近|过去)(\\d{1,3})天");
    private static final Pattern YEARS_PATTERN = Pattern.compile("(?:最近|近|过去)(\\d{1,2})年");
    private static final Pattern LOCATION_TERM_PATTERN = Pattern.compile("([\\p{IsHan}A-Za-z]{2,16}(?:北部|南部|东部|西部|中部|附近|周边|近海|沿海|海域|海湾|海峡|河口|半岛|群岛|港|港口|港口区|航道|锚地|泊位))");
    private static final Map<String, Integer> CHINESE_YEAR_WORDS = Map.ofEntries(
            Map.entry("一年", 1), Map.entry("两年", 2), Map.entry("二年", 2),
            Map.entry("三年", 3), Map.entry("四年", 4), Map.entry("五年", 5),
            Map.entry("六年", 6), Map.entry("七年", 7), Map.entry("八年", 8),
            Map.entry("九年", 9), Map.entry("十年", 10)
    );
    private static final Map<String, List<String>> LOCATION_ALIAS_GROUPS = Map.ofEntries(
            Map.entry("湛江", List.of("湛江", "湛江港", "湛江附近", "湛江周边", "雷州半岛", "湛江航道")),
            Map.entry("珠江口", List.of("珠江口", "伶仃洋", "珠江口海域", "珠江口航道")),
            Map.entry("北部湾", List.of("北部湾", "北部湾海域", "北部湾近海", "广西近海", "防城港", "钦州湾")),
            Map.entry("琼州海峡", List.of("琼州海峡", "琼州海峡航道")),
            Map.entry("南海北部", List.of("南海北部", "南海北部海域", "南海北部近海")),
            Map.entry("广州港", List.of("广州港", "广州", "南沙港")),
            Map.entry("深圳港", List.of("深圳港", "蛇口港", "盐田港", "深圳")),
            Map.entry("香港", List.of("香港", "香港港", "维多利亚港")),
            Map.entry("厦门港", List.of("厦门港", "厦门")),
            Map.entry("上海港", List.of("上海港", "上海", "洋山港"))
    );
    private static final List<String> TREND_KEYWORDS = List.of("趋势", "变化", "波动", "增长", "下降", "对比", "演变", "增减");
    private static final List<String> RISK_KEYWORDS = List.of("高风险", "风险", "异常", "警告", "可疑", "重点关注", "安全隐患");
    private static final List<String> ACTIVITY_KEYWORDS = List.of("谁最活跃", "最活跃", "录入最多", "录入次数", "谁录入", "数据贡献", "谁的记录");
    private static final List<String> AREA_HINTS = List.of("珠江口", "湛江港", "北部湾", "琼州海峡", "广州港", "深圳港", "香港", "厦门港", "上海港", "南海", "航道", "锚地");
    private static final List<String> LOCATION_SUFFIXES = List.of("附件", "周边", "近海", "沿海", "海域", "海湾", "海峡", "港", "港口", "航道", "锚地");
    private static final List<String> MAP_SCOPE_KEYWORDS = List.of(
            "地图覆盖", "涵盖", "覆盖范围", "什么范围", "哪些区域", "哪些海域", "哪些港口",
            "覆盖到哪", "分布在哪些地方", "支持哪些水域", "监控范围"
    );
    private static final List<String> HELP_KEYWORDS = List.of(
            "你能做什么", "可以问什么", "怎么问", "怎么用", "能查什么", "你会什么", "你能帮我什么"
    );
    private static final Set<String> CLARIFY_ONLY_MESSAGES = Set.of(
            "?", "？", "啥", "什么", "什么意思", "然后呢", "继续", "展开", "详细点", "再说说", "具体呢"
    );
    private static final List<String> FOLLOW_UP_HINTS = List.of(
            "只看", "只筛", "换成", "改成", "按", "那", "那就", "那现在", "这里", "这个范围",
            "再看", "继续看", "近30天", "近三年", "最近", "现在呢", "然后呢", "详细点", "展开"
    );
    private static final List<String> VESSEL_PROFILE_KEYWORDS = List.of(
            "是什么", "是啥", "介绍", "介绍一下", "了解一下", "讲讲", "说说", "简介", "资料", "信息", "船型"
    );
    private static final List<String> VESSEL_AIS_KEYWORDS = List.of(
            "AIS", "动态", "记录", "最近", "近30天", "近三年", "活跃", "出现", "轨迹", "航行", "速度", "航向"
    );
    private static final List<String> SYSTEM_DATA_KEYWORDS = List.of(
            "系统", "数据库", "档案", "记录", "AIS", "船舶", "航线", "港口", "地图", "分布",
            "哪里", "在哪", "态势", "交通", "航行", "速度", "航向", "MMSI", "船名", "类型",
            "吨位", "风险", "异常", "统计", "数量", "有哪些", "列出", "筛选", "轨迹"
    );
    private static final List<String> EXPLICIT_SYSTEM_DATA_KEYWORDS = List.of(
            "系统", "数据库", "档案", "记录", "AIS", "船舶", "航线", "港口", "态势", "交通",
            "航行", "速度", "航向", "MMSI", "船名", "类型", "吨位", "风险", "异常", "统计", "筛选"
    );
    private static final List<String> CASUAL_GENERAL_KEYWORDS = List.of(
            "好吃", "吃吗", "能吃", "怎么吃", "做法", "口感", "味道", "价格", "多少钱", "哪里买",
            "怎么样", "好不好", "推荐", "可以吗", "能不能", "聊天", "笑话", "天气", "电影", "旅游"
    );

    private final AiProperties aiProperties;
    private final AisService aisService;
    private final VesselService vesselService;
    private final AuditService auditService;
    private final AssistantQueryCache assistantQueryCache;
    private final RagKnowledgeService ragKnowledgeService;
    private final AiModelGateway aiModelGateway;
    private final AssistantChatHistoryService assistantChatHistoryService;

    public AssistantAiService(
            AiProperties aiProperties,
            AisService aisService,
            VesselService vesselService,
            AuditService auditService,
            AssistantQueryCache assistantQueryCache,
            RagKnowledgeService ragKnowledgeService,
            AiModelGateway aiModelGateway,
            AssistantChatHistoryService assistantChatHistoryService
    ) {
        this.aiProperties = aiProperties;
        this.aisService = aisService;
        this.vesselService = vesselService;
        this.auditService = auditService;
        this.assistantQueryCache = assistantQueryCache;
        this.ragKnowledgeService = ragKnowledgeService;
        this.aiModelGateway = aiModelGateway;
        this.assistantChatHistoryService = assistantChatHistoryService;
    }

    public AssistantAiDtos.ChatResponse chat(AssistantAiDtos.ChatRequest request) {
        Long currentUserId = SecurityUtils.requireCurrentUser().userId();
        String cacheKey = currentUserId + "::" + buildCacheKey(request);
        AssistantAiDtos.ChatResponse cachedResponse = assistantQueryCache.get(cacheKey);
        if (cachedResponse != null) {
            AssistantAiDtos.ChatResponse response = withCacheHit(cachedResponse, true);
            assistantChatHistoryService.recordExchange(request, response);
            auditService.record(currentUserId, "AI", "ASSISTANT_CHAT", "ASSISTANT", null, true,
                    "{\"message\":\"" + escapeJson(request.message()) + "\",\"cached\":true}");
            return response;
        }

        AssistantAiDtos.StructuredQuery plan = extractStructuredQueryFast(request.message(), request.history());
        AssistantAiDtos.ChatResponse response;
        if (isLightweightIntent(plan.intent())) {
            response = buildLightweightResponse(plan, request);
        } else {
            AisAssistantContext context = collectContext(plan);
            List<RagSearchHit> ragHits = ragKnowledgeService.retrieveForScenario(
                    RagKnowledgeService.SCENARIO_ASSISTANT, request.message(), 6);
            response = buildConversationalResponse(request, plan, context, ragHits);
        }
        assistantChatHistoryService.recordExchange(request, response);
        assistantQueryCache.put(cacheKey, response);

        auditService.record(currentUserId, "AI", "ASSISTANT_CHAT", "ASSISTANT", null, true,
                "{\"message\":\"" + escapeJson(request.message()) + "\",\"cached\":false}");
        return response;
    }

    private AssistantAiDtos.ChatResponse withCacheHit(AssistantAiDtos.ChatResponse response, boolean cacheHit) {
        return new AssistantAiDtos.ChatResponse(
                response.answer(), response.structuredQuery(), response.highlights(), response.evidence(), cacheHit);
    }

    private AssistantAiDtos.ChatResponse buildConversationalResponse(
            AssistantAiDtos.ChatRequest request,
            AssistantAiDtos.StructuredQuery plan,
            AisAssistantContext context,
            List<RagSearchHit> ragHits
    ) {
        AssistantAiDtos.ChatResponse localResponse = buildLocalResponse(plan, context);
        List<AssistantAiDtos.EvidenceItem> evidence = mergeEvidence(localResponse.evidence(), ragHits);
        List<String> highlights = buildConversationalHighlights(localResponse.highlights(), ragHits);

        try {
            String answer = aiModelGateway.deepSeekText(buildConversationMessages(request, plan, context, ragHits));
            if (!StringUtils.hasText(answer)) {
                answer = softenLocalAnswer(localResponse.answer());
            }
            return new AssistantAiDtos.ChatResponse(normalizeAssistantAnswer(answer), plan, highlights, evidence, false);
        } catch (RuntimeException ignored) {
            return new AssistantAiDtos.ChatResponse(softenLocalAnswer(localResponse.answer()), plan, highlights, evidence, false);
        }
    }

    private List<Map<String, Object>> buildConversationMessages(
            AssistantAiDtos.ChatRequest request, AssistantAiDtos.StructuredQuery plan,
            AisAssistantContext context, List<RagSearchHit> ragHits
    ) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(AiModelGateway.message("system", """
                你是 ShipInsight AIS 船舶交通态势平台里的 AI 分析助手，回答要像正常对话助手一样自然。
                直接回答用户问题，不要模板化，不要总说"按当前筛选条件""系统中匹配到"。
                用户问"介绍一下/是什么/某个船名或MMSI"时，默认按船舶档案介绍来回答：先说明这艘船的基本信息（船型、吨位、船旗国），再讲 AIS 动态、航行状态和系统内相关记录。
                用户问态势/风险时，要结合风险摘要中的低速/停船/异常数据做分析，给出合理的航行安全建议。
                系统数据和 RAG 证据是参考资料，不要机械罗列；可以自然地说"我这里查到..."。
                如果用户问的是日常、常识、闲聊或开放式问题，要像通用 DeepSeek 助手一样直接回答，不要强行转成系统统计。
                如果资料不足，可以结合通用航海和船舶知识回答，但要避免编造具体系统记录。
                回答使用中文，语气专业、顺口、像 GPT 一样会解释；一般控制在 2 到 5 个短段落。
                """));

        List<AssistantAiDtos.ConversationMessage> history = request.history() == null ? List.of() : request.history();
        history.stream()
                .filter(item -> item != null && StringUtils.hasText(item.role()) && StringUtils.hasText(item.content()))
                .skip(Math.max(0, history.size() - 6))
                .forEach(item -> messages.add(AiModelGateway.message(normalizeRole(item.role()), truncateForPrompt(item.content(), 700))));

        messages.add(AiModelGateway.message("user", """
                用户问题：
                %s

                结构化理解：
                %s

                系统数据摘要：
                %s

                RAG 检索证据：
                %s

                请基于这些资料自然回答。不要输出 JSON，不要写"根据结构化查询"，不要把右侧证据区当成正文重复说明。
                """.formatted(
                request.message(), describePlan(plan),
                buildContextForPrompt(plan, context), buildRagForPrompt(ragHits))));
        return messages;
    }

    private List<AssistantAiDtos.EvidenceItem> mergeEvidence(
            List<AssistantAiDtos.EvidenceItem> localEvidence, List<RagSearchHit> ragHits) {
        List<AssistantAiDtos.EvidenceItem> evidence = new ArrayList<>();
        if (localEvidence != null) evidence.addAll(localEvidence.stream().limit(5).toList());
        if (ragHits != null) {
            ragHits.stream().limit(6).forEach(hit -> evidence.add(new AssistantAiDtos.EvidenceItem(
                    "rag:" + safe(hit.sourceType()).toLowerCase(Locale.ROOT), hit.title(),
                    firstNonBlank(hit.summary(), truncateForReply(hit.content(), 140)),
                    hit.sourceId(), hit.score(), hit.sourcePath())));
        }
        return evidence.stream().limit(10).toList();
    }

    private List<String> buildConversationalHighlights(List<String> localHighlights, List<RagSearchHit> ragHits) {
        List<String> highlights = new ArrayList<>();
        if (localHighlights != null) highlights.addAll(localHighlights.stream().limit(3).toList());
        if (ragHits != null && !ragHits.isEmpty()) highlights.add("参考了 " + ragHits.size() + " 条 RAG 证据");
        return highlights.stream().filter(StringUtils::hasText).distinct().limit(4).toList();
    }

    private String buildContextForPrompt(AssistantAiDtos.StructuredQuery plan, AisAssistantContext context) {
        List<String> lines = new ArrayList<>();
        boolean isEmpty = context.riskSummary() == null
                && context.vessels().isEmpty()
                && context.aisRecords().isEmpty()
                && context.importerStats().isEmpty()
                && context.vesselDraftCandidates().isEmpty()
                && context.trendPoints().isEmpty();
        if ("general_chat".equals(safe(plan.intent())) && isEmpty) {
            return "本次问题没有命中必须引用的系统业务数据。请优先按通用问答自然回答，RAG 证据仅作为可选参考。";
        }
        if ("general_chat".equals(safe(plan.intent()))) {
            lines.add("本次问题偏通用问答；下面资料仅供参考，不要把回答写成系统统计。");
        }

        lines.add("平台概况：船舶档案 " + context.totalVessels() + " 条，活跃 AIS 记录 " + context.recentAisCount() + " 条。");

        if (context.riskSummary() != null) {
            AisRiskSummary risk = context.riskSummary();
            lines.add("风险态势：总记录 " + risk.total() + " 条，低速 " + risk.lowSpeedCount()
                    + " 次，停船 " + risk.stoppedCount() + " 次，异常 " + risk.abnormalNoteCount()
                    + " 次，唯一船舶 " + risk.uniqueVesselCount() + " 艘。");
        }

        if (!context.vessels().isEmpty()) {
            lines.add("相关船舶档案：");
            context.vessels().stream().limit(5).forEach(v -> lines.add("- " + displayVesselName(v)
                    + "；MMSI：" + firstNonBlank(v.mmsi(), "未填写")
                    + "；船型：" + firstNonBlank(v.vesselTypeName(), "未填写")
                    + "；风险等级：" + firstNonBlank(v.riskLevel(), "未标注")
                    + "；航行状态：" + firstNonBlank(v.navigationStatus(), "未知")
                    + "；区域：" + firstNonBlank(v.usualRegion(), v.routeArea(), "未填写")));
        }

        if (!context.aisRecords().isEmpty()) {
            lines.add("最近 AIS 动态：");
            context.aisRecords().stream().limit(6).forEach(r -> lines.add("- " + displayAisRecord(r)));
        }

        if (!context.importerStats().isEmpty()) {
            lines.add("数据贡献：" + context.importerStats().stream().limit(5)
                    .map(s -> s.label() + " " + s.recordCount() + " 条")
                    .collect(Collectors.joining("；")));
        }

        if (!context.vesselDraftCandidates().isEmpty()) {
            lines.add("未建档船舶候选：" + context.vesselDraftCandidates().stream().limit(5)
                    .map(c -> firstNonBlank(c.vesselName(), c.mmsi(), "未知"))
                    .collect(Collectors.joining("、")));
        }

        if (!context.trendPoints().isEmpty()) {
            lines.add("趋势：" + context.trendPoints().stream().limit(8)
                    .map(p -> p.month() + " " + p.recordCount() + " 条记录")
                    .collect(Collectors.joining("；")));
        }

        return String.join("\n", lines);
    }

    private String buildRagForPrompt(List<RagSearchHit> ragHits) {
        if (ragHits == null || ragHits.isEmpty()) return "未检索到强相关 RAG 证据。";
        return ragHits.stream().limit(6)
                .map(hit -> "- 来源：" + hit.sourceType() + "；标题：" + hit.title()
                        + "；相似度：" + String.format(Locale.ROOT, "%.2f", hit.score())
                        + "；摘要：" + truncateForPrompt(firstNonBlank(hit.summary(), hit.content()), 360))
                .collect(Collectors.joining("\n"));
    }

    private String describePlan(AssistantAiDtos.StructuredQuery plan) {
        return "意图=" + safe(plan.intent())
                + "；地点=" + firstNonBlank(plan.locationKeyword(), "无")
                + "；区域=" + firstNonBlank(plan.areaKeyword(), "无")
                + "；船舶=" + firstNonBlank(plan.vesselKeyword(), "无")
                + "；风险等级=" + firstNonBlank(plan.riskLevel(), "无")
                + "；航行状态=" + firstNonBlank(plan.navigationStatus(), "无")
                + "；时间=" + (plan.recentDays() != null ? "近" + plan.recentDays() + "天"
                    : plan.yearsBack() != null ? "近" + plan.yearsBack() + "年" : "默认");
    }

    private String softenLocalAnswer(String answer) {
        String softened = firstNonBlank(answer, "我这里暂时没有查到足够资料，不过可以继续换个关键词再试。");
        softened = softened.replace("按当前筛选条件，", "我这里查到，");
        return softened;
    }

    private String normalizeAssistantAnswer(String answer) {
        String cleaned = answer == null ? "" : answer.trim();
        cleaned = cleaned.replaceFirst("^```(?:markdown|text)?\\s*", "").replaceFirst("\\s*```$", "").trim();
        return firstNonBlank(cleaned, "我这里暂时没有组织出可靠回答，可以换个问法再试。");
    }

    private String truncateForPrompt(String value, int maxLength) {
        String normalized = normalize(value);
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    private String normalizeRole(String role) {
        String normalized = role == null ? "" : role.trim().toLowerCase(Locale.ROOT);
        return ("assistant".equals(normalized) || "system".equals(normalized) || "user".equals(normalized))
                ? normalized : "user";
    }

    private String buildCacheKey(AssistantAiDtos.ChatRequest request) {
        String messageKey = normalize(request.message()).toLowerCase(Locale.ROOT);
        if (request.history() == null || request.history().isEmpty()) return messageKey;
        List<AssistantAiDtos.ConversationMessage> history = request.history().stream()
                .filter(item -> item != null && StringUtils.hasText(item.role()) && StringUtils.hasText(item.content()))
                .toList();
        String historyKey = history.stream()
                .skip(Math.max(0, history.size() - 2))
                .map(item -> item.role().trim().toLowerCase(Locale.ROOT) + ":" + normalize(item.content()).toLowerCase(Locale.ROOT))
                .collect(Collectors.joining("|"));
        return messageKey + "||" + historyKey;
    }

    // ==================== Structured Query Extraction ====================

    private AssistantAiDtos.StructuredQuery extractStructuredQueryFast(String question) {
        String message = normalize(question);

        Integer recentDays = extractNumber(message, DAYS_PATTERN);
        Integer yearsBack = extractNumber(message, YEARS_PATTERN);
        if (yearsBack == null) yearsBack = extractChineseYears(message);

        boolean includeTrend = containsAny(message, TREND_KEYWORDS);
        boolean riskOnly = containsAny(message, RISK_KEYWORDS);
        String riskLevel = extractRiskLevel(message);
        String navigationStatus = extractNavigationStatus(message);

        // Extract area keyword from known areas
        List<VesselView> vesselSamples = vesselService.listVessels(null, null, null, null, null, null, 1, 20).items();
        String areaKeyword = pickLongestMatch(message, AREA_HINTS);
        String locationKeyword = resolveLocationKeyword(message);
        String vesselKeyword = extractVesselKeyword(message, vesselSamples);
        String intent = inferIntent(message, vesselKeyword, areaKeyword, includeTrend, riskOnly);

        return new AssistantAiDtos.StructuredQuery(
                intent, emptyToNull(locationKeyword), emptyToNull(areaKeyword),
                emptyToNull(vesselKeyword), emptyToNull(riskLevel), emptyToNull(navigationStatus),
                yearsBack, recentDays, includeTrend, riskOnly, inferLimit(message, intent));
    }

    private AssistantAiDtos.StructuredQuery extractStructuredQueryFast(
            String question, List<AssistantAiDtos.ConversationMessage> history) {
        return extractStructuredQueryFast(question, history, 0);
    }

    private AssistantAiDtos.StructuredQuery extractStructuredQueryFast(
            String question, List<AssistantAiDtos.ConversationMessage> history, int depth) {
        AssistantAiDtos.StructuredQuery currentPlan = extractStructuredQueryFast(question);
        if (depth >= 3 || !shouldInheritContext(question, currentPlan)) return currentPlan;
        PreviousUserTurn previousTurn = findLatestPreviousUserTurn(history, question);
        if (previousTurn == null || !StringUtils.hasText(previousTurn.message())) return currentPlan;
        AssistantAiDtos.StructuredQuery previousPlan = extractStructuredQueryFast(
                previousTurn.message(), previousTurn.earlierHistory(), depth + 1);
        return mergeStructuredQuery(previousPlan, currentPlan, question);
    }

    // ==================== Context Collection ====================

    private AisAssistantContext collectContext(AssistantAiDtos.StructuredQuery plan) {
        long totalVessels = vesselService.countVessels();

        if ("general_chat".equals(safe(plan.intent()))) {
            return new AisAssistantContext(totalVessels, 0, null, List.of(), List.of(),
                    List.of(), List.of(), List.of());
        }

        LocalDateTime observedFrom = resolveObservedFrom(plan);
        AisRiskSummary riskSummary = aisService.riskSummary(
                emptyToNull(plan.vesselKeyword()), observedFrom, null);

        List<AisRecordView> aisRecords = List.of();
        try {
            PageResponse<AisRecordView> aisPage = aisService.list(
                    emptyToNull(plan.vesselKeyword()), observedFrom, null, 1,
                    Math.min(resolveLimit(plan.limit(), 10) * 3, 30));
            aisRecords = aisPage.items().stream()
                    .filter(r -> matchesAisFilters(r, plan))
                    .sorted(Comparator.comparing(AisRecordView::baseDateTime).reversed())
                    .toList();
        } catch (Exception ignored) { /* ClickHouse may be empty */ }
        long recentAisCount = aisRecords.size();

        List<VesselView> vessels = List.of();
        try {
            PageResponse<VesselView> vesselPage = vesselService.listVessels(
                    emptyToNull(plan.vesselKeyword()), null, null,
                    emptyToNull(plan.riskLevel()),
                    emptyToNull(plan.navigationStatus()),
                    emptyToNull(plan.areaKeyword()), 1,
                    Math.min(resolveLimit(plan.limit(), 10) * 2, 30));
            vessels = vesselPage.items();
        } catch (Exception ignored) { /* may be empty */ }

        List<AisRankingStat> importerStats = List.of();
        if ("importer_activity".equals(safe(plan.intent()))) {
            try {
                importerStats = aisService.importerStats(null, observedFrom, null, 10);
            } catch (Exception ignored) { }
        }

        List<AisVesselDraftCandidate> draftCandidates = List.of();
        if ("vessel_lookup".equals(safe(plan.intent())) && vessels.isEmpty() && StringUtils.hasText(plan.vesselKeyword())) {
            try {
                draftCandidates = aisService.vesselDraftCandidates(
                        emptyToNull(plan.vesselKeyword()), observedFrom, null, 10);
            } catch (Exception ignored) { }
        }

        List<TrendPoint> trendPoints = List.of();
        if (shouldBuildTrend(plan)) {
            try {
                List<AisRankingStat> dateStats = aisService.importerStats(
                        emptyToNull(plan.vesselKeyword()), observedFrom, null, 12);
                trendPoints = buildTrendFromDateStats(dateStats);
            } catch (Exception ignored) { }
        }

        return new AisAssistantContext(totalVessels, recentAisCount, riskSummary, vessels,
                aisRecords, importerStats, draftCandidates, trendPoints);
    }

    private boolean matchesAisFilters(AisRecordView record, AssistantAiDtos.StructuredQuery plan) {
        if (StringUtils.hasText(plan.vesselKeyword())) {
            String keyword = normalize(plan.vesselKeyword());
            if (!containsIgnoreCase(firstNonBlank(record.vesselName(), ""), keyword)
                    && !containsIgnoreCase(firstNonBlank(record.mmsi(), ""), keyword)
                    && !containsIgnoreCase(firstNonBlank(record.imo(), ""), keyword)) {
                return false;
            }
        }
        if (StringUtils.hasText(plan.locationKeyword())) {
            // approximate location filtering
            if (record.latitude() != null && record.longitude() != null) {
                String locStr = record.latitude() + "," + record.longitude();
                // For AIS records, location matching via coordinates is loose
                // We keep them all and let the LLM handle it
            }
        }
        return true;
    }

    // ==================== Local Response Building ====================

    private AssistantAiDtos.ChatResponse buildLocalResponse(
            AssistantAiDtos.StructuredQuery plan, AisAssistantContext context) {
        String answer = switch (safe(plan.intent())) {
            case "general_chat" -> buildGeneralChatAnswer(plan, context);
            case "coverage_scope" -> buildCoverageScopeAnswer(plan, context);
            case "importer_activity" -> buildImporterActivityAnswer(plan, context);
            case "traffic_trend" -> buildTrafficTrendAnswer(plan, context);
            case "ais_lookup" -> buildAisLookupAnswer(plan, context);
            case "vessel_profile" -> buildVesselProfileAnswer(plan, context);
            case "vessel_lookup" -> buildVesselLookupAnswer(plan, context);
            default -> buildOverviewAnswer(plan, context);
        };
        return new AssistantAiDtos.ChatResponse(answer, plan,
                buildHighlights(plan, context), buildEvidence(plan, context), false);
    }

    private String buildGeneralChatAnswer(AssistantAiDtos.StructuredQuery plan, AisAssistantContext context) {
        if (!context.vessels().isEmpty()) {
            VesselView vessel = context.vessels().get(0);
            return "这个问题更偏日常或通用问答。系统里和它相关的船舶档案是 "
                    + displayVesselName(vessel)
                    + "（" + firstNonBlank(vessel.vesselTypeName(), "未知船型") + "，"
                    + firstNonBlank(vessel.riskLevel(), "未标注风险等级") + "，"
                    + firstNonBlank(vessel.navigationStatus(), "未知航行状态") + "）。"
                    + " 如果 DeepSeek 服务可用，我会结合这些资料和通用知识直接回答你的原问题。";
        }
        return "这是一个通用问题，我会优先按自然问答来回答，并把 RAG 检索到的资料作为参考；当前如果模型服务不可用，就只能先给出有限的系统资料。";
    }

    private AssistantAiDtos.ChatResponse buildLightweightResponse(
            AssistantAiDtos.StructuredQuery plan, AssistantAiDtos.ChatRequest request) {
        String answer = switch (safe(plan.intent())) {
            case "clarify" -> buildClarificationAnswer(request);
            case "capability_help" -> buildCapabilityAnswer();
            default -> "我已经收到你的问题了，但当前还缺少足够的查询线索。你可以补充船名、MMSI、港口或时间范围，我会继续往下查。";
        };
        return new AssistantAiDtos.ChatResponse(answer, plan,
                buildLightweightHighlights(plan), buildLightweightEvidence(plan, request), false);
    }

    private String buildCoverageScopeAnswer(AssistantAiDtos.StructuredQuery plan, AisAssistantContext context) {
        List<String> areaNames = AREA_HINTS.stream().limit(8).toList();
        StringBuilder answer = new StringBuilder();
        if (StringUtils.hasText(plan.locationKeyword())) {
            answer.append("当前平台已按「")
                    .append(plan.locationKeyword())
                    .append("」做了区域聚焦，");
        } else {
            answer.append("ShipInsight AIS 船舶交通态势平台覆盖中国南海及周边主要港口和航道，");
        }
        answer.append("主要集中在 ")
                .append(String.join("、", areaNames))
                .append(" 等水域。");
        answer.append(" 系统当前收录船舶档案 ")
                .append(context.totalVessels())
                .append(" 条，AIS 活跃记录 ")
                .append(context.recentAisCount())
                .append(" 条。");
        if (context.riskSummary() != null && context.riskSummary().hasSignals()) {
            answer.append(" 当前区域态势中检测到 ")
                    .append(context.riskSummary().lowSpeedCount())
                    .append(" 次低速航行和 ")
                    .append(context.riskSummary().stoppedCount())
                    .append(" 次停船事件，建议关注。");
        }
        answer.append(" 可以追问「只看湛江港」「琼州海峡近30天态势」来缩小范围。");
        return answer.toString();
    }

    private String buildImporterActivityAnswer(AssistantAiDtos.StructuredQuery plan, AisAssistantContext context) {
        if (context.importerStats().isEmpty()) {
            return "当前还没有检索到数据录入活跃度的相关统计。";
        }
        AisRankingStat top = context.importerStats().get(0);
        StringBuilder answer = new StringBuilder();
        answer.append("按当前筛选条件，最活跃的数据贡献者是 ")
                .append(top.label())
                .append("，共录入 ")
                .append(top.recordCount())
                .append(" 条 AIS 记录。");
        if (context.importerStats().size() > 1) {
            String ranking = context.importerStats().stream().limit(3)
                    .map(s -> s.label() + "（" + s.recordCount() + " 条）")
                    .collect(Collectors.joining("、"));
            answer.append(" 前三位依次为 ").append(ranking).append("。");
        }
        if (StringUtils.hasText(plan.locationKeyword())) {
            answer.append(" 区域过滤为 ").append(plan.locationKeyword()).append("。");
        }
        return answer.toString();
    }

    private String buildTrafficTrendAnswer(AssistantAiDtos.StructuredQuery plan, AisAssistantContext context) {
        if (context.trendPoints().isEmpty() && (context.riskSummary() == null || !context.riskSummary().hasSignals())) {
            String scope = firstNonBlank(plan.locationKeyword(), plan.areaKeyword(), "当前筛选范围");
            return scope + " 的态势趋势分析显示，当前缺少足够的月度变化数据。系统中共有船舶档案 "
                    + context.totalVessels() + " 条，可以缩小时间范围或补充船名再查。";
        }

        String scope = firstNonBlank(plan.locationKeyword(), plan.areaKeyword(), "当前筛选范围");
        StringBuilder answer = new StringBuilder();
        answer.append(scope).append(" 的 AIS 态势分析显示，");

        if (!context.trendPoints().isEmpty()) {
            answer.append(summarizeTrendSentence(context.trendPoints()));
        } else {
            answer.append("当前缺少足够的月度变化数据。");
        }

        if (context.riskSummary() != null && context.riskSummary().hasSignals()) {
            answer.append(" 当前风险概况：低速 ")
                    .append(context.riskSummary().lowSpeedCount())
                    .append(" 次，停船 ")
                    .append(context.riskSummary().stoppedCount())
                    .append(" 次，异常标记 ")
                    .append(context.riskSummary().abnormalNoteCount())
                    .append(" 次，涉及 ")
                    .append(context.riskSummary().uniqueVesselCount())
                    .append(" 艘船舶。");
        }
        if (!context.vessels().isEmpty()) {
            answer.append(" 相关船舶包括 ").append(joinVesselNames(context.vessels(), 5)).append("。");
        }
        return answer.toString();
    }

    private String buildAisLookupAnswer(AssistantAiDtos.StructuredQuery plan, AisAssistantContext context) {
        if (context.aisRecords().isEmpty()) {
            if (!context.vessels().isEmpty()) {
                return "按当前筛选条件，暂未匹配到直接相关的 AIS 动态记录，但系统船舶档案中有 "
                        + context.vessels().size() + " 艘相关船舶，包括 "
                        + joinVesselNames(context.vessels(), 5) + "。";
            }
            return "当前还没有检索到相关的 AIS 动态数据。系统共有船舶档案 "
                    + context.totalVessels() + " 条，可以尝试补充船名或 MMSI 再查。";
        }

        LocalDateTime newest = context.aisRecords().stream()
                .map(AisRecordView::baseDateTime)
                .max(LocalDateTime::compareTo).orElse(LocalDateTime.now());
        LocalDateTime oldest = context.aisRecords().stream()
                .map(AisRecordView::baseDateTime)
                .min(LocalDateTime::compareTo).orElse(newest);

        StringBuilder answer = new StringBuilder();
        answer.append("按当前筛选条件，共匹配到 ")
                .append(context.aisRecords().size())
                .append(" 条 AIS 动态记录，时间范围从 ")
                .append(formatDate(oldest)).append(" 到 ").append(formatDate(newest)).append("。");
        if (StringUtils.hasText(plan.locationKeyword())) {
            answer.append(" 区域已过滤为 ").append(plan.locationKeyword()).append("。");
        }
        if (StringUtils.hasText(plan.vesselKeyword())) {
            answer.append(" 船舶聚焦于 ").append(plan.vesselKeyword()).append("。");
        }
        if (!context.vessels().isEmpty()) {
            answer.append(" 关联船舶档案包括 ").append(joinVesselNames(context.vessels(), 6)).append("。");
        }
        if (!context.importerStats().isEmpty()) {
            answer.append(" 数据贡献排名前三：")
                    .append(context.importerStats().stream().limit(3)
                            .map(s -> s.label() + "(" + s.recordCount() + "条)")
                            .collect(Collectors.joining("、"))).append("。");
        }
        return answer.toString();
    }

    private String buildVesselProfileAnswer(AssistantAiDtos.StructuredQuery plan, AisAssistantContext context) {
        if (context.vessels().isEmpty()) {
            return "当前还没有检索到对应船舶的档案资料。系统共有船舶档案 "
                    + context.totalVessels() + " 条，可以尝试提供 MMSI 或更完整的船名。";
        }

        VesselView vessel = context.vessels().get(0);
        String displayName = displayVesselName(vessel);
        StringBuilder answer = new StringBuilder();
        answer.append(displayName).append("是");

        String vesselType = firstNonBlank(vessel.vesselTypeName(), vessel.vesselTypePath(), "未知船型");
        answer.append("一艘").append(vesselType);

        List<String> facts = new ArrayList<>();
        if (StringUtils.hasText(vessel.mmsi())) facts.add("MMSI 编号 " + vessel.mmsi());
        if (StringUtils.hasText(vessel.flagState())) facts.add("船旗国 " + vessel.flagState());
        if (vessel.lengthM() != null && vessel.widthM() != null) {
            facts.add("长 " + vessel.lengthM() + " 米、宽 " + vessel.widthM() + " 米");
        }
        if (StringUtils.hasText(vessel.riskLevel())) facts.add("风险等级为" + vessel.riskLevel());
        if (StringUtils.hasText(vessel.navigationStatus())) facts.add("当前航行状态为" + vessel.navigationStatus());
        if (!facts.isEmpty()) answer.append("（").append(String.join("，", facts)).append("）");
        answer.append("。");

        if (StringUtils.hasText(vessel.routeArea())) {
            answer.append(" 航线范围主要在 ").append(vessel.routeArea()).append("。");
        }
        if (StringUtils.hasText(vessel.usualRegion())) {
            answer.append(" 常用区域为 ").append(vessel.usualRegion()).append("。");
        }

        if (!context.aisRecords().isEmpty()) {
            answer.append(" 在系统当前 AIS 数据中，该船还有 ")
                    .append(context.aisRecords().size())
                    .append(" 条动态记录，可追问「近30天航行轨迹」或「当前航行状态」。");
        }
        return answer.toString();
    }

    private String buildVesselLookupAnswer(AssistantAiDtos.StructuredQuery plan, AisAssistantContext context) {
        if (context.vessels().isEmpty() && context.vesselDraftCandidates().isEmpty()) {
            return "按当前筛选条件，系统中共匹配到 0 艘相关船舶。平台当前收录船舶档案 "
                    + context.totalVessels() + " 条，可以换个关键词或放宽条件再查。";
        }

        StringBuilder answer = new StringBuilder();
        if (plan.riskOnly()) {
            answer.append("按高风险筛选条件，");
        } else {
            answer.append("按当前筛选条件，");
        }
        answer.append("系统中共匹配到 ")
                .append(context.vessels().size())
                .append(" 艘相关船舶");

        if (!context.vessels().isEmpty()) {
            answer.append("，包括 ").append(joinVesselNames(context.vessels(), 6)).append("。");
        }

        if (!context.vesselDraftCandidates().isEmpty()) {
            answer.append(" 同时从 AIS 数据中发现了 ")
                    .append(context.vesselDraftCandidates().size())
                    .append(" 艘尚未建档的候选船舶：")
                    .append(context.vesselDraftCandidates().stream().limit(5)
                            .map(c -> firstNonBlank(c.vesselName(), c.mmsi(), "未知"))
                            .collect(Collectors.joining("、"))).append("。");
        }
        if (!context.aisRecords().isEmpty()) {
            answer.append(" 这些船舶关联到 ").append(context.aisRecords().size()).append(" 条 AIS 动态记录。");
        }
        return answer.toString();
    }

    private String buildOverviewAnswer(AssistantAiDtos.StructuredQuery plan, AisAssistantContext context) {
        if (context.vessels().isEmpty() && context.aisRecords().isEmpty()
                && (context.riskSummary() == null || !context.riskSummary().hasSignals())) {
            return "当前还没有检索到足够的数据来直接回答这个问题。平台共有船舶档案 "
                    + context.totalVessels() + " 条。可以补充船名、MMSI、港口或时间范围再试。";
        }

        String focus = firstNonBlank(plan.locationKeyword(), plan.areaKeyword(), plan.vesselKeyword());
        StringBuilder answer = new StringBuilder();
        if (StringUtils.hasText(focus)) {
            answer.append("围绕 「").append(focus).append("」 这部分数据来看，");
        } else {
            answer.append("先给你一个和当前问题最相关的概览：");
        }

        if (!context.vessels().isEmpty()) {
            answer.append(" 相关船舶包括 ").append(joinVesselNames(context.vessels(), 5)).append("。");
        }
        if (!context.aisRecords().isEmpty()) {
            answer.append(" AIS 动态涉及 ").append(context.aisRecords().size()).append(" 条记录。");
        }
        if (context.riskSummary() != null && context.riskSummary().hasSignals()) {
            answer.append(" 风险方面，低速 ").append(context.riskSummary().lowSpeedCount())
                    .append(" 次，停船 ").append(context.riskSummary().stoppedCount())
                    .append(" 次，异常 ").append(context.riskSummary().abnormalNoteCount()).append(" 次。");
        }
        answer.append(" 平台当前累计船舶档案 ").append(context.totalVessels()).append(" 条。");
        return answer.toString();
    }

    // ==================== Highlights ====================

    private List<String> buildHighlights(AssistantAiDtos.StructuredQuery plan, AisAssistantContext context) {
        List<String> highlights = new ArrayList<>();
        if ("general_chat".equals(safe(plan.intent()))) {
            if (!context.vessels().isEmpty()) {
                highlights.add("通用问答模式：已找到 " + context.vessels().size() + " 艘相关船舶");
            } else {
                highlights.add("通用问答模式：RAG 证据作为可选参考");
            }
            return highlights;
        }
        if (!context.vessels().isEmpty()) {
            highlights.add("匹配到 " + context.vessels().size() + " 艘船舶档案");
        }
        if (!context.aisRecords().isEmpty()) {
            highlights.add("匹配到 " + context.aisRecords().size() + " 条 AIS 动态");
        }
        if (context.riskSummary() != null && context.riskSummary().hasSignals()) {
            highlights.add("检测到 " + context.riskSummary().abnormalNoteCount() + " 次异常标记");
        }
        if (!context.importerStats().isEmpty()) {
            AisRankingStat top = context.importerStats().get(0);
            highlights.add(top.label() + " 是当前最活跃的数据贡献者");
        }
        if (plan.riskOnly()) {
            highlights.add("已启用高风险筛选");
        }
        if (StringUtils.hasText(plan.locationKeyword())) {
            highlights.add("区域过滤：" + plan.locationKeyword());
        }
        if (highlights.isEmpty()) {
            highlights.add("当前条件下没有直接命中完整数据，建议缩小范围再试");
        }
        return highlights.stream().limit(4).toList();
    }

    private List<String> buildLightweightHighlights(AssistantAiDtos.StructuredQuery plan) {
        return switch (safe(plan.intent())) {
            case "clarify" -> List.of(
                    "当前问题太短，系统还拿不到明确查询目标",
                    "补充船名、MMSI、港口或时间范围后，回答会更具体",
                    "可以直接追问船舶档案、AIS 动态、风险态势或覆盖范围"
            );
            case "capability_help" -> List.of(
                    "支持船舶档案查询、AIS 动态分析、风险态势监测和交通趋势总结",
                    "支持模糊地点识别，例如湛江港、珠江口、北部湾",
                    "重复问题会优先命中缓存，返回更快"
            );
            default -> List.of("当前已切换到轻量回答模式");
        };
    }

    private List<AssistantAiDtos.EvidenceItem> buildEvidence(AssistantAiDtos.StructuredQuery plan, AisAssistantContext context) {
        List<AssistantAiDtos.EvidenceItem> evidence = new ArrayList<>();
        if ("general_chat".equals(safe(plan.intent()))) {
            context.vessels().stream().limit(3).forEach(v -> evidence.add(new AssistantAiDtos.EvidenceItem(
                    "vessel", displayVesselName(v),
                    "船型：" + firstNonBlank(v.vesselTypeName(), "未知")
                            + "；风险等级：" + firstNonBlank(v.riskLevel(), "未标注")
                            + "；航行状态：" + firstNonBlank(v.navigationStatus(), "未知"))));
            if (evidence.isEmpty()) {
                evidence.add(new AssistantAiDtos.EvidenceItem("mode", "通用问答",
                        "该问题未触发系统统计意图，将优先由通用模型回答。"));
            }
            return evidence;
        }

        evidence.add(new AssistantAiDtos.EvidenceItem("dashboard", "平台概况",
                "船舶档案 " + context.totalVessels() + " 条，AIS 记录 " + context.recentAisCount() + " 条"));

        context.vessels().stream().limit(3).forEach(v -> evidence.add(new AssistantAiDtos.EvidenceItem(
                "vessel", displayVesselName(v),
                "MMSI：" + firstNonBlank(v.mmsi(), "未填写")
                        + "；船型：" + firstNonBlank(v.vesselTypeName(), "未知")
                        + "；风险等级：" + firstNonBlank(v.riskLevel(), "未标注"))));

        context.aisRecords().stream().limit(3).forEach(r -> evidence.add(new AssistantAiDtos.EvidenceItem(
                "ais", firstNonBlank(r.vesselName(), r.mmsi(), "未知船舶"),
                formatDate(r.baseDateTime()) + "；航速：" + (r.sog() != null ? r.sog() + "节" : "未知"))));

        context.importerStats().stream().limit(2).forEach(s -> evidence.add(new AssistantAiDtos.EvidenceItem(
                "importer", s.label(), "录入 " + s.recordCount() + " 条记录")));

        if (context.riskSummary() != null && context.riskSummary().hasSignals()) {
            evidence.add(new AssistantAiDtos.EvidenceItem("risk", "风险摘要",
                    "低速 " + context.riskSummary().lowSpeedCount()
                            + "，停船 " + context.riskSummary().stoppedCount()
                            + "，异常 " + context.riskSummary().abnormalNoteCount()));
        }

        if (StringUtils.hasText(plan.vesselKeyword()) && context.vessels().isEmpty()) {
            evidence.add(new AssistantAiDtos.EvidenceItem("hint", "检索提示",
                    "当前船舶关键词没有命中档案，可尝试 MMSI 或完整船名"));
        }
        return evidence.stream().limit(6).toList();
    }

    private List<AssistantAiDtos.EvidenceItem> buildLightweightEvidence(
            AssistantAiDtos.StructuredQuery plan, AssistantAiDtos.ChatRequest request) {
        List<AssistantAiDtos.EvidenceItem> evidence = new ArrayList<>();
        if ("clarify".equals(safe(plan.intent()))) {
            String previousQuestion = findLatestUserQuestion(request.history(), request.message());
            evidence.add(new AssistantAiDtos.EvidenceItem("hint", "提问建议",
                    StringUtils.hasText(previousQuestion)
                            ? "如果你是在继续上一题「" + truncateForReply(previousQuestion, 18)
                                + "」，可以直接补一句「只看湛江港」「展开AIS动态」或「按近30天再看」。"
                            : "可以补充船名、MMSI、港口或时间范围。"));
            return evidence;
        }
        evidence.add(new AssistantAiDtos.EvidenceItem("capability", "支持的提问方式",
                "可直接查询船舶档案、AIS 动态、风险态势、交通趋势、数据贡献排行等。"));
        evidence.add(new AssistantAiDtos.EvidenceItem("example", "示例问题",
                "例如：湛江港近7天有哪些高风险船舶？珠江口今天的AIS态势如何？"));
        return evidence;
    }

    // ==================== Intent Classification ====================

    private String inferIntent(String message, String vesselKeyword, String areaKeyword,
                               boolean includeTrend, boolean riskOnly) {
        if (isClarifyOnlyMessage(message)) return "clarify";
        if (containsAny(message, HELP_KEYWORDS)) return "capability_help";
        if (isMapScopeQuestion(message)) return "coverage_scope";
        if (containsAny(message, ACTIVITY_KEYWORDS)) return "importer_activity";
        if (includeTrend || containsAny(message, List.of("态势", "趋势", "变化", "异常"))) {
            return "traffic_trend";
        }
        if (isAisLookupQuestion(message, vesselKeyword)) return "ais_lookup";
        if (isVesselProfileQuestion(message, vesselKeyword)) return "vessel_profile";
        if (isCasualGeneralQuestion(message) && !hasExplicitSystemDataSignal(message)) return "general_chat";
        if (riskOnly || isSystemDataQuestion(message, vesselKeyword, areaKeyword)) return "vessel_lookup";
        return "general_chat";
    }

    private boolean isAisLookupQuestion(String message, String vesselKeyword) {
        if (containsAny(message, List.of("AIS", "动态", "轨迹", "航迹", "行踪"))) return true;
        boolean hasDomainAnchor = hasDomainAnchor(message, vesselKeyword, null);
        if (containsAny(message, List.of("记录", "出现", "航行")) && hasDomainAnchor) return true;
        return containsAny(message, List.of("最近", "近30天", "近三年", "过去")) && hasDomainAnchor
                && hasExplicitSystemDataSignal(message);
    }

    private boolean isVesselProfileQuestion(String message, String vesselKeyword) {
        if (!StringUtils.hasText(vesselKeyword)) return false;
        String normalized = normalize(message);
        if (containsAny(normalized, VESSEL_AIS_KEYWORDS)
                || containsAny(normalized, List.of("分布", "哪里", "在哪", "趋势", "变化"))) {
            return false;
        }
        if (containsAny(normalized, VESSEL_PROFILE_KEYWORDS)) return true;
        // exact match: user just asked the vessel name
        String compactMessage = normalized.replaceAll("[\\s?？。！!，,、]+", "");
        String compactVessel = vesselKeyword.replaceAll("[\\s?？。！!，,、]+", "");
        return compactMessage.equalsIgnoreCase(compactVessel);
    }

    private boolean isCasualGeneralQuestion(String message) {
        return containsAny(message, CASUAL_GENERAL_KEYWORDS);
    }

    private boolean hasExplicitSystemDataSignal(String message) {
        return containsAny(message, EXPLICIT_SYSTEM_DATA_KEYWORDS);
    }

    private boolean isSystemDataQuestion(String message, String vesselKeyword, String areaKeyword) {
        if (hasExplicitSystemDataSignal(message)) return true;
        return hasDomainAnchor(message, vesselKeyword, areaKeyword) && containsAny(message, SYSTEM_DATA_KEYWORDS);
    }

    private boolean hasDomainAnchor(String message, String vesselKeyword, String areaKeyword) {
        return StringUtils.hasText(vesselKeyword) || StringUtils.hasText(areaKeyword)
                || containsAny(message, List.of("船舶", "AIS", "航线", "港口", "交通态势", "MMSI"));
    }

    private boolean isClarifyOnlyMessage(String message) {
        String compact = normalize(message).replaceAll("\\s+", "");
        if (!StringUtils.hasText(compact)) return true;
        if (CLARIFY_ONLY_MESSAGES.contains(compact)) return true;
        return compact.length() <= 2 && compact.chars().allMatch(ch -> !Character.isLetterOrDigit(ch) || ch == '?' || ch == '？');
    }

    private boolean isMapScopeQuestion(String message) {
        return containsAny(message, MAP_SCOPE_KEYWORDS)
                || (containsAny(message, List.of("地图", "区域", "港口", "水域"))
                && containsAny(message, List.of("范围", "覆盖", "涵盖", "哪些", "哪里", "地方")));
    }

    // ==================== Context Inheritance ====================

    private boolean shouldInheritContext(String question, AssistantAiDtos.StructuredQuery plan) {
        String intent = safe(plan.intent());
        if ("clarify".equals(intent) || "capability_help".equals(intent)) return false;
        return isFilterOnlyFollowUp(question);
    }

    private boolean isFilterOnlyFollowUp(String question) {
        String normalized = normalize(question);
        return containsAny(normalized, FOLLOW_UP_HINTS) && !StringUtils.hasText(extractVesselKeyword(normalized, List.of()));
    }

    private AssistantAiDtos.StructuredQuery mergeStructuredQuery(
            AssistantAiDtos.StructuredQuery prev, AssistantAiDtos.StructuredQuery curr, String question) {
        String intent = safe(curr.intent());
        if ("general_chat".equals(intent) || "clarify".equals(intent) || "capability_help".equals(intent)) {
            return curr;
        }

        return new AssistantAiDtos.StructuredQuery(
                intent,
                firstNonBlank(curr.locationKeyword(), prev.locationKeyword()),
                firstNonBlank(curr.areaKeyword(), prev.areaKeyword()),
                firstNonBlank(curr.vesselKeyword(), prev.vesselKeyword()),
                firstNonBlank(curr.riskLevel(), prev.riskLevel()),
                firstNonBlank(curr.navigationStatus(), prev.navigationStatus()),
                curr.yearsBack() != null ? curr.yearsBack() : prev.yearsBack(),
                curr.recentDays() != null ? curr.recentDays() : prev.recentDays(),
                curr.includeTrend() || prev.includeTrend(),
                curr.riskOnly() || prev.riskOnly(),
                curr.limit() != null ? curr.limit() : prev.limit()
        );
    }

    private PreviousUserTurn findLatestPreviousUserTurn(
            List<AssistantAiDtos.ConversationMessage> history, String currentQuestion) {
        if (history == null || history.isEmpty()) return null;
        String compactCurrent = normalize(currentQuestion).replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        for (int i = history.size() - 1; i >= 0; i--) {
            AssistantAiDtos.ConversationMessage item = history.get(i);
            if (!"user".equals(normalizeRole(item.role()))) continue;
            String compactItem = normalize(item.content()).replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
            if (compactItem.equals(compactCurrent)) continue;
            List<AssistantAiDtos.ConversationMessage> earlierHistory = history.subList(0, i);
            return new PreviousUserTurn(item.content(), List.copyOf(earlierHistory));
        }
        return null;
    }

    // ==================== Extraction Helpers ====================

    private String extractVesselKeyword(String message, List<VesselView> vesselSamples) {
        // Try matching MMSI (9-digit number)
        Matcher mmsiMatcher = Pattern.compile("\\b(\\d{9})\\b").matcher(message);
        if (mmsiMatcher.find()) return mmsiMatcher.group(1);

        // Try matching known vessel names from samples
        if (!vesselSamples.isEmpty()) {
            List<String> vesselNames = vesselSamples.stream()
                    .flatMap(v -> {
                        List<String> names = new ArrayList<>();
                        if (StringUtils.hasText(v.vesselName())) names.add(v.vesselName());
                        if (StringUtils.hasText(v.mmsi())) names.add(v.mmsi());
                        return names.stream();
                    }).distinct().toList();
            String match = pickLongestMatch(message, vesselNames);
            if (StringUtils.hasText(match)) return match;
        }

        return null;
    }

    private String extractRiskLevel(String message) {
        if (containsAny(message, List.of("重点关注", "高风险", "高危"))) return "重点关注";
        if (containsAny(message, List.of("普通关注", "普通"))) return "普通关注";
        if (containsAny(message, List.of("低风险"))) return "低风险";
        return null;
    }

    private String extractNavigationStatus(String message) {
        if (containsAny(message, List.of("在航"))) return "在航";
        if (containsAny(message, List.of("锚泊", "锚地"))) return "锚泊";
        if (containsAny(message, List.of("港内作业", "港内"))) return "港内作业";
        return null;
    }

    private String resolveLocationKeyword(String message) {
        String normalized = normalize(message);
        for (var entry : LOCATION_ALIAS_GROUPS.entrySet()) {
            for (String alias : entry.getValue()) {
                if (normalized.contains(alias)) return entry.getKey();
            }
        }
        return extractLocationBySuffix(message);
    }

    private String extractLocationBySuffix(String message) {
        Matcher matcher = LOCATION_TERM_PATTERN.matcher(message);
        String best = null;
        int bestLen = 0;
        while (matcher.find()) {
            String candidate = matcher.group(0);
            boolean hasSuffix = LOCATION_SUFFIXES.stream().anyMatch(s -> candidate.endsWith(s));
            if (hasSuffix && candidate.length() > bestLen) {
                best = candidate;
                bestLen = candidate.length();
            }
        }
        return best;
    }

    private Integer extractNumber(String message, Pattern pattern) {
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            try { return Integer.parseInt(matcher.group(1)); } catch (NumberFormatException ignored) { }
        }
        return null;
    }

    private Integer extractChineseYears(String message) {
        for (var entry : CHINESE_YEAR_WORDS.entrySet()) {
            if (message.contains(entry.getKey())) return entry.getValue();
        }
        return null;
    }

    // ==================== Helpers ====================

    private boolean shouldBuildTrend(AssistantAiDtos.StructuredQuery plan) {
        return plan.includeTrend() || "traffic_trend".equals(safe(plan.intent()));
    }

    private LocalDateTime resolveObservedFrom(AssistantAiDtos.StructuredQuery plan) {
        if (plan.recentDays() != null) return LocalDateTime.now().minusDays(plan.recentDays());
        if (plan.yearsBack() != null) return LocalDateTime.now().minusYears(plan.yearsBack());
        return LocalDateTime.now().minusDays(30); // default 30 days
    }

    private List<TrendPoint> buildTrendFromDateStats(List<AisRankingStat> dateStats) {
        if (dateStats == null || dateStats.isEmpty()) return List.of();
        return dateStats.stream()
                .filter(s -> StringUtils.hasText(s.label()))
                .map(s -> new TrendPoint(s.label(), (int) s.recordCount()))
                .limit(12).toList();
    }

    private String summarizeTrendSentence(List<TrendPoint> points) {
        if (points.isEmpty()) return "暂无显著变化趋势。";
        TrendPoint first = points.get(0);
        TrendPoint last = points.get(points.size() - 1);
        long change = last.recordCount() - first.recordCount();
        String direction = change > 0 ? "上升" : change < 0 ? "下降" : "持平";
        return "近 " + points.size() + " 个统计周期内呈" + direction + "趋势，从 "
                + first.recordCount() + " 条到 " + last.recordCount() + " 条。";
    }

    private String displayVesselName(VesselView vessel) {
        if (vessel == null) return "未知船舶";
        String name = firstNonBlank(vessel.vesselName(), vessel.mmsi(), "未知船舶");
        if (StringUtils.hasText(vessel.vesselTypeName())) name += "（" + vessel.vesselTypeName() + "）";
        return name;
    }

    private String displayAisRecord(AisRecordView record) {
        return firstNonBlank(record.vesselName(), record.mmsi(), "未知")
                + "；时间：" + formatDate(record.baseDateTime())
                + "；航速：" + (record.sog() != null ? record.sog() + "节" : "未知")
                + "；位置：" + formatCoord(record.latitude(), record.longitude());
    }

    private String formatCoord(BigDecimal lat, BigDecimal lng) {
        if (lat == null || lng == null) return "未知";
        return String.format(Locale.ROOT, "%.4f,%.4f", lat, lng);
    }

    private String formatDate(LocalDateTime dateTime) {
        return dateTime == null ? "未知" : dateTime.format(DATE_FORMATTER);
    }

    private String joinVesselNames(List<VesselView> vessels, int limit) {
        return vessels.stream().limit(limit)
                .map(v -> firstNonBlank(v.vesselName(), v.mmsi(), "未知"))
                .collect(Collectors.joining("、"));
    }

    private String pickLongestMatch(String message, Collection<String> candidates) {
        if (candidates == null || candidates.isEmpty()) return null;
        String normalized = normalize(message);
        String best = null;
        int bestLen = 0;
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate) && normalized.contains(normalize(candidate))
                    && candidate.length() > bestLen) {
                best = candidate;
                bestLen = candidate.length();
            }
        }
        return best;
    }

    private String findLatestUserQuestion(
            List<AssistantAiDtos.ConversationMessage> history, String currentMessage) {
        if (history == null || history.isEmpty()) return null;
        String compactCurrent = normalize(currentMessage).replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        for (int i = history.size() - 1; i >= 0; i--) {
            AssistantAiDtos.ConversationMessage item = history.get(i);
            if (!"user".equals(normalizeRole(item.role()))) continue;
            String compactItem = normalize(item.content()).replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
            if (!compactItem.equals(compactCurrent)) return item.content();
        }
        return null;
    }

    private String buildClarificationAnswer(AssistantAiDtos.ChatRequest request) {
        String previousQuestion = findLatestUserQuestion(request.history(), request.message());
        if (StringUtils.hasText(previousQuestion)) {
            return "这句追问还不够明确。如果你是在继续上一题「"
                    + truncateForReply(previousQuestion, 18)
                    + "」，可以直接补一句「只看珠江口」「展开AIS动态」或「按近30天再看一遍」，我就能顺着往下答。";
        }
        return "这句问题现在还太短，我暂时分不清你是想看船舶档案、AIS 动态、风险态势，还是覆盖范围。"
                + " 你可以补充船名、MMSI、港口或时间，比如「湛江港近7天有哪些高风险船舶」。";
    }

    private String buildCapabilityAnswer() {
        return "我更擅长帮你查这几类问题：船舶档案查询（某船的基本参数和历史）、AIS 动态分析（某区域或船舶的最近航行数据）、"
                + "风险态势监测（低速、停船、异常事件统计）、交通趋势总结（特定水域的 AIS 数据变化），"
                + "以及数据贡献排行。你可以直接把船舶、港口和时间范围一起说出来，我会先转成结构化查询，再给你一段分析总结。";
    }

    private int inferLimit(String message, String intent) {
        if ("general_chat".equals(safe(intent))) return 10;
        if (message.contains("所有") || message.contains("全部") || message.contains("列出")) return 30;
        return 10;
    }

    private boolean isLightweightIntent(String intent) {
        String normalizedIntent = safe(intent);
        return "clarify".equals(normalizedIntent) || "capability_help".equals(normalizedIntent);
    }

    // ==================== Utility Methods ====================

    private boolean containsAny(String message, List<String> keywords) {
        if (!StringUtils.hasText(message)) return false;
        String normalized = normalize(message);
        return keywords.stream().anyMatch(normalized::contains);
    }

    private boolean containsIgnoreCase(String haystack, String needle) {
        if (!StringUtils.hasText(haystack) || !StringUtils.hasText(needle)) return false;
        return haystack.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private String truncateForReply(String value, int maxLength) {
        String normalized = normalize(value);
        if (normalized.length() <= maxLength) return normalized;
        return normalized.substring(0, Math.max(0, maxLength)) + "...";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) if (StringUtils.hasText(value)) return value;
        return "";
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("[\\t\\n\\r]+", " ").trim();
    }

    private String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\"", "\\\"");
    }

    private int resolveLimit(Integer limit, int defaultLimit) {
        return limit != null && limit > 0 ? limit : defaultLimit;
    }

    // ==================== Private Records ====================

    private record TrendPoint(String month, int recordCount) {}

    private record PreviousUserTurn(String message, List<AssistantAiDtos.ConversationMessage> earlierHistory) {}

    private record AisAssistantContext(
            long totalVessels,
            long recentAisCount,
            AisRiskSummary riskSummary,
            List<VesselView> vessels,
            List<AisRecordView> aisRecords,
            List<AisRankingStat> importerStats,
            List<AisVesselDraftCandidate> vesselDraftCandidates,
            List<TrendPoint> trendPoints
    ) {}
}
