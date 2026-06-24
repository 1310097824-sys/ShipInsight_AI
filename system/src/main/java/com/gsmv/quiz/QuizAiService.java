package com.gsmv.quiz;

import com.fasterxml.jackson.databind.JsonNode;
import com.gsmv.ai.AiModelGateway;
import com.gsmv.common.ErrorCode;
import com.gsmv.common.exception.BusinessException;
import com.gsmv.quiz.dto.QuizAiDtos;
import com.gsmv.quiz.dto.QuizAiDtos.GenerateQuestionsRequest;
import com.gsmv.quiz.dto.QuizAiDtos.GenerateQuestionsResponse;
import com.gsmv.quiz.dto.QuizAiDtos.GeneratedQuestion;
import com.gsmv.quiz.mapper.QuizAiHistoryMapper;
import com.gsmv.quiz.mapper.QuizMapper;
import com.gsmv.quiz.model.QuizAiChatMessage;
import com.gsmv.quiz.model.QuizQuestion;
import com.gsmv.security.CurrentUser;
import com.gsmv.security.SecurityUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class QuizAiService {

    private static final int DEFAULT_HISTORY_LIMIT = 120;
    private static final int MAX_HISTORY_MESSAGES = 10;
    private static final int MAX_GENERATE_COUNT = 10;

    private static final String SYSTEM_PROMPT = """
            你是"航海知识学习助手"，专门服务于航海知识问答平台。

            你的知识范围严格限定在以下三类：
            1. SHIP（船舶知识）：船舶结构、船舶设备、船舶操作、航行规则、船舶避碰、船舶类型等
            2. WEATHER（气象知识）：海洋气象、天气预报、气象灾害、风浪雾、气象观测等
            3. SEA_AREA（海域知识）：海域地理、航海图、航标、潮汐、洋流、海域环境等

            你的核心能力：
            【AI 出题】当用户要求出题、生成题目、练习时，你需要根据用户指定的类别、题型和难度生成题目。
            生成的题目格式如下（请用 Markdown 排版）：
            ---
            **【题目类别】** 船舶知识 / 天气知识 / 海域知识
            **【题型】** 单选 / 多选 / 判断 / 填空
            **【难度】** 简单 / 中等 / 困难

            **题目：** （题目内容）

            A. 选项A
            B. 选项B
            C. 选项C
            D. 选项D
            （判断题写"正确 / 错误"，填空题写"____"）

            **参考答案：** X
            **解析：** （简要解释为什么是这个答案）
            ---

            你可以一次生成 1-5 道题目。生成的题目必须准确、专业，符合航海领域知识。

            【知识解答】当用户提出与船舶、气象、海域相关的问题时，你需要给出准确、专业的解答。
            解答格式：
            - 用清晰的自然语言回答
            - 如果涉及专业术语，简要解释
            - 可以适当举例说明
            - 用 Markdown 排版，便于阅读
            - 如果提供了实时天气数据，请结合数据回答用户的天气相关问题

            【重要约束】
            - 只回答与 SHIP、WEATHER、SEA_AREA 三类知识相关的问题
            - 如果用户问了无关话题，礼貌引导回航海知识领域
            - 回答必须准确、专业，不确定时如实说明
            - 语言使用简体中文
            """;

    private static final String GENERATE_PROMPT_TEMPLATE = """
            请生成 %d 道航海知识题目，要求如下：
            - 题目分类: %s（SHIP=船舶知识, WEATHER=气象知识, SEA_AREA=海域知识）
            - 题目类型: %s（SINGLE=单选题, MULTI=多选题, JUDGE=判断题, FILL=填空题）
            - 难度: %s（EASY=简单, MEDIUM=中等, HARD=困难）

            请严格按照以下 JSON 格式返回，不要包含任何其他文本：
            {
              "questions": [
                {
                  "category": "%s",
                  "type": "%s",
                  "title": "题目文本",
                  "options": [{"label":"A","text":"选项A"},{"label":"B","text":"选项B"},{"label":"C","text":"选项C"},{"label":"D","text":"选项D"}],
                  "answer": "正确答案的label，多选用逗号分隔如 A,C，判断题填 A(正确)或B(错误)，填空题填关键词",
                  "explanation": "答案解析",
                  "difficulty": "%s"
                }
              ]
            }

            注意事项：
            - SINGLE 和 MULTI 类型必须有 4 个选项（A/B/C/D）
            - JUDGE 类型 options 固定为 [{"label":"A","text":"正确"},{"label":"B","text":"错误"}]
            - FILL 类型 options 为空数组 []
            - 多选题答案用逗号分隔，如 "A,C,D"
            - 填空题答案如果有多个关键词，用 | 分隔
            - 题目必须专业准确，符合航海领域知识
            - 不要生成重复或过于简单的题目
            """;

    private static final Pattern WEATHER_KEYWORD_PATTERN = Pattern.compile(
            "天气|气温|温度|风力|风向|下雨|下雪|暴雨|台风|大雾|雾霾|能见度|气压|湿度|浪高|海况|weather|forecast",
            Pattern.CASE_INSENSITIVE
    );

    /** 已知港口/沿海城市列表，优先精确匹配 */
    private static final Pattern KNOWN_CITY_PATTERN = Pattern.compile(
            "上海|北京|广州|深圳|天津|大连|青岛|宁波|厦门|舟山|烟台|威海|连云港|南通|湛江|海口|三亚|福州|泉州|防城港|北海|汕头|珠海|营口|秦皇岛|唐山|日照|温州|台州|莆田|漳州|潮州|汕尾|惠州|中山|香港|澳门"
    );

    /** 带行政后缀的通用城市名，如"青岛市""浙江省" */
    private static final Pattern SUFFIX_CITY_PATTERN = Pattern.compile(
            "[\\u4e00-\\u9fa5]{2,4}(?:市|省|区|县)"
    );

    private final AiModelGateway aiModelGateway;
    private final QuizAiHistoryMapper historyMapper;
    private final QuizMapper quizMapper;
    private final WeatherService weatherService;

    public QuizAiService(
            AiModelGateway aiModelGateway,
            QuizAiHistoryMapper historyMapper,
            QuizMapper quizMapper,
            WeatherService weatherService
    ) {
        this.aiModelGateway = aiModelGateway;
        this.historyMapper = historyMapper;
        this.quizMapper = quizMapper;
        this.weatherService = weatherService;
    }

    // ==================== AI 聊天 ====================

    public QuizAiDtos.ChatResponse chat(QuizAiDtos.ChatRequest request) {
        List<Map<String, Object>> messages = buildMessages(request);
        String answer = aiModelGateway.deepSeekText(messages);
        String mode = detectMode(request.message());

        recordExchange(request, answer);
        return new QuizAiDtos.ChatResponse(answer, mode);
    }

    public QuizAiDtos.ChatHistoryResponse listHistory() {
        CurrentUser currentUser = SecurityUtils.requireCurrentUser();
        List<QuizAiChatMessage> records = historyMapper.listRecentByUserId(currentUser.userId(), DEFAULT_HISTORY_LIMIT);
        List<QuizAiDtos.ChatHistoryItem> messages = new ArrayList<>();

        for (QuizAiChatMessage record : records) {
            messages.add(new QuizAiDtos.ChatHistoryItem(
                    record.getId(),
                    record.getRole(),
                    record.getContent(),
                    record.getCreatedAt()
            ));
        }

        return new QuizAiDtos.ChatHistoryResponse(messages);
    }

    public void clearHistory() {
        CurrentUser currentUser = SecurityUtils.requireCurrentUser();
        historyMapper.deleteByUserId(currentUser.userId());
    }

    // ==================== AI 出题入库 ====================

    public GenerateQuestionsResponse generateAndSaveQuestions(GenerateQuestionsRequest request) {
        int count = Math.max(1, Math.min(request.count(), MAX_GENERATE_COUNT));
        String category = request.category();
        String type = request.type();
        String difficulty = StringUtils.hasText(request.difficulty()) ? request.difficulty() : "EASY";

        String prompt = String.format(GENERATE_PROMPT_TEMPLATE,
                count, category, type, difficulty,
                category, type, difficulty
        );

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(AiModelGateway.message("system",
                "你是航海知识出题专家。请严格按照 JSON 格式返回题目数据，不要包含 markdown 代码块标记。"));
        messages.add(AiModelGateway.message("user", prompt));

        JsonNode json = aiModelGateway.deepSeekJson(messages);
        JsonNode questionsNode = json.path("questions");

        if (!questionsNode.isArray() || questionsNode.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "AI 未返回有效的题目数据", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        List<GeneratedQuestion> saved = new ArrayList<>();
        List<GeneratedQuestion> duplicates = new ArrayList<>();

        for (JsonNode qNode : questionsNode) {
            GeneratedQuestion question = parseQuestion(qNode, category, type, difficulty);
            if (question == null) {
                continue;
            }

            // 查重：按 title 精确匹配
            long existing = quizMapper.countByTitle(question.title());
            if (existing > 0) {
                duplicates.add(question);
                continue;
            }

            // 插入数据库
            QuizQuestion entity = new QuizQuestion();
            entity.setCategory(question.category());
            entity.setType(question.type());
            entity.setTitle(question.title());
            entity.setOptions(question.options());
            entity.setAnswer(question.answer());
            entity.setExplanation(question.explanation());
            entity.setDifficulty(question.difficulty());
            quizMapper.insertQuestion(entity);

            saved.add(question);
        }

        return new GenerateQuestionsResponse(saved, duplicates, saved.size(), duplicates.size());
    }

    private GeneratedQuestion parseQuestion(JsonNode qNode, String defaultCategory, String defaultType, String defaultDifficulty) {
        try {
            String title = qNode.path("title").asText("").trim();
            if (title.isEmpty()) {
                return null;
            }

            String category = qNode.path("category").asText(defaultCategory).trim();
            String type = qNode.path("type").asText(defaultType).trim();
            String difficulty = qNode.path("difficulty").asText(defaultDifficulty).trim();
            String answer = qNode.path("answer").asText("").trim();
            String explanation = qNode.path("explanation").asText("").trim();

            if (answer.isEmpty()) {
                return null;
            }

            // 构建 options JSON
            String optionsJson;
            JsonNode optionsNode = qNode.path("options");
            if (type.equals("JUDGE") || (optionsNode.isMissingNode() && type.equals("JUDGE"))) {
                optionsJson = "[{\"label\":\"A\",\"text\":\"正确\"},{\"label\":\"B\",\"text\":\"错误\"}]";
            } else if (type.equals("FILL") || (optionsNode.isMissingNode() && type.equals("FILL"))) {
                optionsJson = "[]";
            } else if (optionsNode.isArray() && !optionsNode.isEmpty()) {
                optionsJson = optionsNode.toString();
            } else {
                optionsJson = "[{\"label\":\"A\",\"text\":\"\"},{\"label\":\"B\",\"text\":\"\"},{\"label\":\"C\",\"text\":\"\"},{\"label\":\"D\",\"text\":\"\"}]";
            }

            return new GeneratedQuestion(category, type, title, optionsJson, answer, explanation, difficulty);
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== 天气测试 ====================

    /**
     * 测试天气检测链路：关键词匹配 → 城市提取 → 百度API调用。
     * 仅供调试用，返回每一步的中间结果。
     */
    public Map<String, Object> testWeather(String msg) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("input", msg);
        result.put("akConfigured", weatherService.isConfigured());

        boolean keywordHit = StringUtils.hasText(msg) && WEATHER_KEYWORD_PATTERN.matcher(msg).find();
        result.put("weatherKeywordHit", keywordHit);

        String city = null;
        if (keywordHit) {
            city = extractCity(msg);
        }
        result.put("extractedCity", city);

        String weatherData = null;
        if (keywordHit && city != null && weatherService.isConfigured()) {
            weatherData = weatherService.fetchWeatherByCity(city);
        }
        result.put("weatherData", weatherData);
        result.put("success", StringUtils.hasText(weatherData));

        return result;
    }

    // ==================== 天气解读（给态势总览用）====================

    /**
     * 获取指定城市的天气，并用 AI 解读是否适合船只出海。
     * 返回包含 weatherData / aiInterpretation / suitableForSailing 的 Map。
     */
    public Map<String, Object> interpretWeather(String city) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("city", city);
        result.put("akConfigured", weatherService.isConfigured());

        if (!weatherService.isConfigured()) {
            result.put("error", "百度地图 AK 未配置或无效，无法获取天气数据");
            result.put("suitableForSailing", null);
            result.put("aiInterpretation", null);
            result.put("weatherData", null);
            return result;
        }

        String weatherData = weatherService.fetchWeatherByCity(city);
        result.put("weatherData", weatherData);

        if (!StringUtils.hasText(weatherData)) {
            result.put("error", "无法获取 " + city + " 的天气数据，请检查城市名称是否正确");
            result.put("suitableForSailing", null);
            result.put("aiInterpretation", null);
            return result;
        }

        // 用 AI 解读天气是否适合出海
        String aiPrompt = """
                你是一位经验丰富的远洋船长，请根据以下实时天气数据，判断当前是否适合船只出海航行。
                
                请按以下格式回答（用 JSON 格式）：
                {
                  "suitable": true/false (是否适合出海),
                  "summary": "一句话总结（中文，30字以内）",
                  "details": "详细分析（中文，100字以内，说明风力、浪高、能见度等对航行的影响）"
                }
                
                天气数据：
                %s
                """.formatted(weatherData);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(AiModelGateway.message("system", "你是专业航海气象分析专家，回答必须严格按 JSON 格式返回。"));
        messages.add(AiModelGateway.message("user", aiPrompt));

        Boolean suitable = null;
        String summary = null;
        String details = null;

        try {
            JsonNode json = aiModelGateway.deepSeekJson(messages);
            suitable = json.path("suitable").asBoolean();
            summary = json.path("summary").asText("");
            details = json.path("details").asText("");
        } catch (Exception e) {
            // AI 解析失败，尝试从文本中提取
            String aiRaw = aiModelGateway.deepSeekText(messages);
            suitable = aiRaw.contains("适合") && !aiRaw.contains("不适合");
            summary = aiRaw.length() > 50 ? aiRaw.substring(0, 50) : aiRaw;
            details = aiRaw;
        }

        String aiInterpretation = (summary != null ? summary : "")
                + (details != null ? " —— " + details : "");

        result.put("suitableForSailing", suitable);
        result.put("aiInterpretation", aiInterpretation);
        result.put("summary", summary);
        result.put("details", details);
        result.put("error", null);
        return result;
    }

    // ==================== 内部方法 ====================

    private List<Map<String, Object>> buildMessages(QuizAiDtos.ChatRequest request) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(AiModelGateway.message("system", SYSTEM_PROMPT));

        // 天气检测：如果用户问题涉及天气，自动获取实时天气数据注入 prompt
        String weatherContext = tryFetchWeatherContext(request.message());
        if (StringUtils.hasText(weatherContext)) {
            messages.add(AiModelGateway.message("system",
                    "以下是实时天气数据，请在回答用户天气相关问题时参考使用：\n\n" + weatherContext));
        }

        if (request.history() != null && !request.history().isEmpty()) {
            List<QuizAiDtos.ConversationMessage> recentHistory = request.history()
                    .stream()
                    .filter(m -> StringUtils.hasText(m.role()) && StringUtils.hasText(m.content()))
                    .toList();

            int startIndex = Math.max(0, recentHistory.size() - MAX_HISTORY_MESSAGES);
            for (int i = startIndex; i < recentHistory.size(); i++) {
                QuizAiDtos.ConversationMessage msg = recentHistory.get(i);
                messages.add(AiModelGateway.message(msg.role(), msg.content()));
            }
        }

        messages.add(AiModelGateway.message("user", request.message()));
        return messages;
    }

    /**
     * 检测用户消息是否涉及天气，如果是则尝试提取城市名并获取实时天气。
     */
    private String tryFetchWeatherContext(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return null;
        }

        // 检测是否是天气相关问题
        if (!WEATHER_KEYWORD_PATTERN.matcher(userMessage).find()) {
            return null;
        }

        if (!weatherService.isConfigured()) {
            return null;
        }

        // 尝试提取城市名
        String city = extractCity(userMessage);
        if (city == null) {
            return null;
        }

        return weatherService.fetchWeatherByCity(city);
    }

    /**
     * 从用户消息中提取城市名。
     */
    private String extractCity(String message) {
        // 1. 先精确匹配已知城市
        Matcher known = KNOWN_CITY_PATTERN.matcher(message);
        if (known.find()) {
            return known.group();
        }
        // 2. 再匹配带后缀的通用城市名（如"青岛市"）
        Matcher suffix = SUFFIX_CITY_PATTERN.matcher(message);
        if (suffix.find()) {
            String city = suffix.group();
            return city.replaceAll("(市|省|区|县)$", "");
        }
        return null;
    }

    private String detectMode(String message) {
        if (message == null || message.isBlank()) {
            return "chat";
        }
        String lower = message.toLowerCase();
        if (lower.contains("出题") || lower.contains("生成题目") || lower.contains("练习")
                || lower.contains("给我出") || lower.contains("来几道") || lower.contains("来一道")
                || lower.contains("generate")) {
            return "generate";
        }
        return "chat";
    }

    private void recordExchange(QuizAiDtos.ChatRequest request, String answer) {
        CurrentUser currentUser = SecurityUtils.requireCurrentUser();

        if (StringUtils.hasText(request.message())) {
            QuizAiChatMessage userMessage = new QuizAiChatMessage();
            userMessage.setUserId(currentUser.userId());
            userMessage.setRole("user");
            userMessage.setContent(request.message().trim());
            historyMapper.insert(userMessage);
        }

        if (StringUtils.hasText(answer)) {
            QuizAiChatMessage assistantMessage = new QuizAiChatMessage();
            assistantMessage.setUserId(currentUser.userId());
            assistantMessage.setRole("assistant");
            assistantMessage.setContent(answer);
            historyMapper.insert(assistantMessage);
        }
    }
}
