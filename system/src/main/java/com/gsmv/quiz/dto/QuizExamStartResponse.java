package com.gsmv.quiz.dto;

import com.gsmv.quiz.model.QuizQuestion;
import java.util.List;

public record QuizExamStartResponse(
        Long recordId,
        List<QuizQuestion> questions
) {}
