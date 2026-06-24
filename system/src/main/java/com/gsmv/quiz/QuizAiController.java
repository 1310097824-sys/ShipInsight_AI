package com.gsmv.quiz;

import com.gsmv.common.ApiResponse;
import com.gsmv.quiz.dto.QuizAiDtos;
import com.gsmv.quiz.dto.QuizAiDtos.GenerateQuestionsRequest;
import com.gsmv.quiz.dto.QuizAiDtos.GenerateQuestionsResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/quiz/ai")
public class QuizAiController {

    private final QuizAiService quizAiService;

    public QuizAiController(QuizAiService quizAiService) {
        this.quizAiService = quizAiService;
    }

    @PostMapping("/chat")
    public ApiResponse<QuizAiDtos.ChatResponse> chat(@Valid @RequestBody QuizAiDtos.ChatRequest request) {
        return ApiResponse.success(quizAiService.chat(request));
    }

    @PostMapping("/generate")
    public ApiResponse<GenerateQuestionsResponse> generateQuestions(@Valid @RequestBody GenerateQuestionsRequest request) {
        return ApiResponse.success(quizAiService.generateAndSaveQuestions(request));
    }

    @GetMapping("/messages")
    public ApiResponse<QuizAiDtos.ChatHistoryResponse> messages() {
        return ApiResponse.success(quizAiService.listHistory());
    }

    @DeleteMapping("/messages")
    public ApiResponse<Void> clearMessages() {
        quizAiService.clearHistory();
        return ApiResponse.success();
    }

    @GetMapping("/weather/test")
    public ApiResponse<java.util.Map<String, Object>> weatherTest(@RequestParam(defaultValue = "") String msg) {
        return ApiResponse.success(quizAiService.testWeather(msg));
    }

    /**
     * 获取指定城市的天气情况，并由 AI 判断是否适合船只出海。
     * 无需登录（permitAll），专供态势总览页面调用。
     */
    @GetMapping("/weather/interpret")
    public ApiResponse<java.util.Map<String, Object>> weatherInterpret(
            @RequestParam(defaultValue = "湛江") String city
    ) {
        return ApiResponse.success(quizAiService.interpretWeather(city));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody QuizAiDtos.ChatRequest request) {
        SseEmitter emitter = new SseEmitter(120_000L);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        CompletableFuture.runAsync(() -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            try {
                sendEvent(emitter, "status", new QuizAiDtos.ChatStreamEvent("status", "正在思考中...", null));
                QuizAiDtos.ChatResponse response = quizAiService.chat(request);
                for (String chunk : splitAnswerForStreaming(response.answer())) {
                    sendEvent(emitter, "delta", new QuizAiDtos.ChatStreamEvent("delta", chunk, null));
                    sleepQuietly(22);
                }
                sendEvent(emitter, "final", new QuizAiDtos.ChatStreamEvent("final", "", response));
                emitter.complete();
            } catch (Exception exception) {
                try {
                    String message = exception.getMessage();
                    if (message == null || message.isBlank()) {
                        message = "AI 助手生成失败，请稍后再试";
                    }
                    sendEvent(emitter, "error", new QuizAiDtos.ChatStreamEvent("error", message, null));
                } catch (IOException ignored) {
                    // client may have disconnected
                }
                emitter.complete();
            } finally {
                SecurityContextHolder.clearContext();
            }
        });

        return emitter;
    }

    private void sendEvent(
            SseEmitter emitter,
            String eventName,
            QuizAiDtos.ChatStreamEvent event
    ) throws IOException {
        emitter.send(SseEmitter.event()
                .name(eventName)
                .data(event, MediaType.APPLICATION_JSON));
    }

    private List<String> splitAnswerForStreaming(String answer) {
        List<String> chunks = new ArrayList<>();
        if (answer == null || answer.isBlank()) {
            return chunks;
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < answer.length(); index++) {
            char current = answer.charAt(index);
            builder.append(current);
            boolean sentenceBoundary = "。！？；\n".indexOf(current) >= 0;
            if (builder.length() >= 18 || sentenceBoundary) {
                chunks.add(builder.toString());
                builder.setLength(0);
            }
        }
        if (!builder.isEmpty()) {
            chunks.add(builder.toString());
        }
        return chunks;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }
}
