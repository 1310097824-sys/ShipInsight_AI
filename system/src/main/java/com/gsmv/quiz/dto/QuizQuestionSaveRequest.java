package com.gsmv.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QuizQuestionSaveRequest(
        @NotBlank(message = "分类不能为空")
        String category,

        @NotBlank(message = "题型不能为空")
        String type,

        @NotBlank(message = "题目不能为空")
        @Size(max = 512)
        String title,

        @NotBlank(message = "选项不能为空")
        String options,

        @NotBlank(message = "答案不能为空")
        String answer,

        String explanation,

        @NotBlank(message = "难度不能为空")
        String difficulty
) {}
