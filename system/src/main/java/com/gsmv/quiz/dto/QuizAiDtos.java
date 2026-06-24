package com.gsmv.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

public final class QuizAiDtos {

    private QuizAiDtos() {
    }

    public record ConversationMessage(
            @NotBlank(message = "消息角色不能为空") String role,
            @NotBlank(message = "消息内容不能为空") String content
    ) {
    }

    public record ChatRequest(
            @NotBlank(message = "请输入提问内容") String message,
            @Valid List<ConversationMessage> history
    ) {
    }

    public record ChatResponse(
            String answer,
            String mode
    ) {
    }

    public record ChatHistoryItem(
            Long id,
            String role,
            String content,
            LocalDateTime createdAt
    ) {
    }

    public record ChatHistoryResponse(
            List<ChatHistoryItem> messages
    ) {
    }

    public record ChatStreamEvent(
            String type,
            String content,
            ChatResponse response
    ) {
    }

    // ==================== AI 出题入库 ====================

    public record GenerateQuestionsRequest(
            @NotBlank(message = "题目分类不能为空") String category,
            @NotBlank(message = "题目类型不能为空") String type,
            String difficulty,
            int count
    ) {
    }

    public record GeneratedQuestion(
            String category,
            String type,
            String title,
            String options,
            String answer,
            String explanation,
            String difficulty
    ) {
    }

    public record GenerateQuestionsResponse(
            List<GeneratedQuestion> saved,
            List<GeneratedQuestion> duplicates,
            int totalSaved,
            int totalDuplicates
    ) {
    }
}
